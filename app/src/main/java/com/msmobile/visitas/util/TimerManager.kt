package com.msmobile.visitas.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

class TimerManager {
    private val timerSupervisorJob: Job = Job()
    private val timerScope = CoroutineScope(timerSupervisorJob)
    private val _timer = MutableStateFlow<TimerState>(TimerState.Stopped)

    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    fun start(
        elapsedTime: Duration,
        tickUnit: Duration
    ) {
        if (_timer.value is TimerState.Running) return
        _timer.update {
            TimerState.Running(
                elapsedTime = elapsedTime,
                tickUnit = tickUnit
            )
        }
        timerScope.launch {
            while (true) {
                delay(tickUnit)
                _timer.update { state ->
                    TimerState.Running(
                        elapsedTime = state.elapsedTime + tickUnit,
                        tickUnit = tickUnit
                    )
                }
            }
        }
    }

    fun resume() {
        start(_timer.value.elapsedTime, _timer.value.tickUnit)
    }

    fun pause() {
        timerSupervisorJob.cancelChildren()
        _timer.update { state ->
            TimerState.Paused(
                elapsedTime = state.elapsedTime,
                tickUnit = state.tickUnit
            )
        }
    }

    sealed class TimerState {
        abstract val elapsedTime: Duration
        abstract val tickUnit: Duration

        data class Running(override val elapsedTime: Duration, override val tickUnit: Duration) :
            TimerState()

        data class Paused(override val elapsedTime: Duration, override val tickUnit: Duration) :
            TimerState()

        data object Stopped : TimerState() {
            override val elapsedTime: Duration = Duration.ZERO
            override val tickUnit: Duration = Duration.ZERO
        }
    }
}