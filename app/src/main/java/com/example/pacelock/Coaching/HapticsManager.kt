package com.example.pacelock.Coaching

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.pacelock.Data.HapticPattern

class HapticsManager(context: Context) {

    private val vibrator = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator
    }else{
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrate(pattern: HapticPattern) {
        when(pattern){
            HapticPattern.TOO_SLOW -> twoShortBursts()
            HapticPattern.TOO_FAST -> threeShortBursts()
            HapticPattern.MILESTONE -> oneLongBurst()

        }
    }

    private fun twoShortBursts(){
        vibratePattern(longArrayOf(0, 200, 150, 200))
    }

    private fun threeShortBursts(){
        vibratePattern(longArrayOf(0, 200, 150, 200, 150, 200))
    }

    private fun oneLongBurst(){
        vibratePattern(longArrayOf(0, 500))
    }

    private fun vibratePattern(pattern: LongArray){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }else{
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun tick(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }else{
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
}