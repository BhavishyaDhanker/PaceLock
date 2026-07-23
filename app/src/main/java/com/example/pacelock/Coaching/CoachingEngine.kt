package com.example.pacelock.Coaching

import androidx.fragment.app.strictmode.Violation
import com.example.pacelock.Data.HapticPattern
import com.google.android.gms.location.Priority

class CoachingEngine(private val config: CoachingConfig) {
    private val announcedMilestones = mutableSetOf<Int>()

    private var paceViolationStartTime = 0L
    private var currentViolationType: ViolationType = ViolationType.NONE
    private val VIOLATION_HOLD_SECONDS = 3L
    private val ALERT_COOLDOWN_SECONDS = 30L
    private var lastAlertTime = 0L

    fun evaluate(
        distanceMeters: Float,
        currentPacePerSecPerKm: Float,
        elapsedSeconds: Long
    ): CoachingCue?{

        // Distance Milestones
        val kmCompleted = (distanceMeters/1000).toInt()
        if(kmCompleted > 0 && !announcedMilestones.contains(kmCompleted)){
            announcedMilestones.add(kmCompleted)
            return CoachingCue(
                "$kmCompleted kilometer${if(kmCompleted > 1)"s" else ""} completed. Keep it up!",
                CuePriority.MEDIUM,
                HapticPattern.MILESTONE
            )
        }


        // Pace Alert
        if(config.targetPacePerSecPerKm > 0f && currentPacePerSecPerKm > 0f){
            val lowerLimit =
                config.targetPacePerSecPerKm -
                        config.targetPaceToleranceSec

            val upperLimit =
                config.targetPacePerSecPerKm +
                        config.targetPaceToleranceSec

            val violation = when{
                currentPacePerSecPerKm > upperLimit -> ViolationType.TOO_SLOW

                currentPacePerSecPerKm < lowerLimit -> ViolationType.TOO_FAST

                else-> ViolationType.NONE
            }


            if(violation == ViolationType.NONE){
                paceViolationStartTime = 0L
                currentViolationType = ViolationType.NONE
            }else{
                if(violation != currentViolationType) {
                    currentViolationType = violation
                    paceViolationStartTime = elapsedSeconds
                }

                val violationDuration = elapsedSeconds - paceViolationStartTime
                val cooldownElapsed = elapsedSeconds - lastAlertTime

                if(violationDuration > VIOLATION_HOLD_SECONDS  && cooldownElapsed > ALERT_COOLDOWN_SECONDS){
                    lastAlertTime = elapsedSeconds

                    return when(violation){
                        ViolationType.TOO_FAST -> CoachingCue(
                            "Ease up — you're ahead of your pace",
                            CuePriority.HIGH,
                            HapticPattern.TOO_FAST
                        )

                        ViolationType.TOO_SLOW -> CoachingCue(
                            "You're slowing down. Pickup the pace!",
                            CuePriority.HIGH,
                            HapticPattern.TOO_SLOW
                        )

                        else -> null
                    }
                }
            }

        }
        return null
    }

    fun reset(){
        announcedMilestones.clear()
        paceViolationStartTime = 0L
        currentViolationType = ViolationType.NONE
        lastAlertTime = 0L
    }

}

data class CoachingConfig(
    val targetPacePerSecPerKm: Float,
    val targetPaceToleranceSec: Float = 0.1f
)

data class CoachingCue(
    val message: String,
    val priority: CuePriority,
    val hapticPattern: HapticPattern
)

enum class CuePriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class ViolationType{
    NONE,
    TOO_SLOW,
    TOO_FAST,
}