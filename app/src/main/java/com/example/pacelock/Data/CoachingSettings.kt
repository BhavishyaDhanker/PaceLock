package com.example.pacelock.Data

data class CoachingSettings(
    val ttsEnabled: Boolean = true,
    val hapticsEnabled : Boolean = false,
    val metronomeEnabled : Boolean = false,
    val metronomeBpm : Int = 160,
    val metronomeUseSound : Boolean = false,
    val targetPacePerSecPerKm : Float = 270f,
    val targetPaceTolerance : Float = 3f
)
