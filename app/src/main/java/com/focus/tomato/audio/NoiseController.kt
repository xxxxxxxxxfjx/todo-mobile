package com.focus.tomato.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NoiseState(
    val playing: Boolean = false,
    val preset: NoisePreset = NoisePreset.RAIN,
    val volume: Float = 0.55f,
    val stopAtMinutes: Int = 0
)

/** Shared UI/service state for the ambient-noise player. */
object NoiseController {
    private val _state = MutableStateFlow(NoiseState())
    val state: StateFlow<NoiseState> = _state.asStateFlow()

    fun update(transform: (NoiseState) -> NoiseState) {
        _state.value = transform(_state.value)
    }

    fun setVolume(value: Float) {
        _state.value = _state.value.copy(volume = value.coerceIn(0f, 1f))
    }
}
