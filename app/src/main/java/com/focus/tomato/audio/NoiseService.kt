package com.focus.tomato.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.focus.tomato.MainActivity
import com.focus.tomato.R

class NoiseService : Service() {

    companion object {
        const val ACTION_START = "com.focus.tomato.noise.START"
        const val ACTION_STOP = "com.focus.tomato.noise.STOP"
        const val ACTION_SET_VOLUME = "com.focus.tomato.noise.VOLUME"
        const val EXTRA_PRESET = "preset"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_VOLUME = "volume"
        const val NOTIF_NOISE = 3001
        const val CHANNEL_NOISE = "ambient_noise"

        fun start(context: Context, preset: NoisePreset, minutes: Int, volume: Float) {
            val intent = Intent(context, NoiseService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_PRESET, preset.name)
                .putExtra(EXTRA_MINUTES, minutes)
                .putExtra(EXTRA_VOLUME, volume)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, NoiseService::class.java)) }
            NoiseController.update { it.copy(playing = false) }
        }

        fun setVolume(context: Context, volume: Float) {
            val intent = Intent(context, NoiseService::class.java)
                .setAction(ACTION_SET_VOLUME)
                .putExtra(EXTRA_VOLUME, volume)
            runCatching { context.startService(intent) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }
    private var focusRequest: AudioFocusRequest? = null
    private var currentPreset = NoisePreset.RAIN
    private var stopAtMinutes = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentPreset = runCatching {
                    NoisePreset.valueOf(intent.getStringExtra(EXTRA_PRESET) ?: NoisePreset.RAIN.name)
                }.getOrDefault(NoisePreset.RAIN)
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.55f).coerceIn(0f, 1f)
                stopAtMinutes = intent.getIntExtra(EXTRA_MINUTES, 0).coerceIn(0, 240)
                startForegroundCompat(notification())
                requestAudioFocus()
                NoiseEngine.start(currentPreset, volume)
                NoiseController.update {
                    it.copy(
                        playing = true,
                        preset = currentPreset,
                        volume = volume,
                        stopAtMinutes = stopAtMinutes
                    )
                }
                handler.removeCallbacks(stopRunnable)
                if (stopAtMinutes > 0) {
                    handler.postDelayed(stopRunnable, stopAtMinutes * 60_000L)
                }
            }
            ACTION_SET_VOLUME -> {
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.55f).coerceIn(0f, 1f)
                NoiseEngine.setVolume(volume)
                NoiseController.setVolume(volume)
            }
            ACTION_STOP, null -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        NoiseEngine.stop()
        NoiseController.update { it.copy(playing = false) }
        abandonAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_NOISE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_NOISE, notification)
        }
    }

    private fun notification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 31,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 32,
            Intent(this, NoiseService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_NOISE)
            .setSmallIcon(R.drawable.ic_stat_tomato)
            .setContentTitle("白噪音 · ${currentPreset.label}")
            .setContentText(
                if (stopAtMinutes > 0) "将在 $stopAtMinutes 分钟后自动停止" else "专注时的背景声"
            )
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_NOISE,
            "白噪音播放",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "播放雨声、森林、海浪等背景声"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun requestAudioFocus() {
        val manager = getSystemService(AudioManager::class.java) ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change <= AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    handler.post { stopSelf() }
                }
            }
            .build()
        focusRequest = request
        manager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { request ->
            runCatching { getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request) }
        }
        focusRequest = null
    }
}
