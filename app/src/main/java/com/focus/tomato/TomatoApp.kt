package com.focus.tomato

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.focus.tomato.data.AppDatabase
import com.focus.tomato.data.SettingsRepository
import com.focus.tomato.data.TimerStore
import com.focus.tomato.timer.NotificationHelper

class TomatoApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, AppDatabase::class.java, "tomato-focus.db")
            .fallbackToDestructiveMigration()
            .build()
        settings = SettingsRepository(this)
        TimerStore.init(this)
        NotificationHelper.createChannels(this)
    }

    companion object {
        lateinit var instance: TomatoApp
            private set

        fun context(): Context = instance.applicationContext
    }
}
