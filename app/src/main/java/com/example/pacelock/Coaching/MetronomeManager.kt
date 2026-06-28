package com.example.pacelock.Coaching

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.pacelock.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MetronomeManager(
    private val context: Context,
    private val hapticsManager: HapticsManager
) {

    private val metronomeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var metronomeJob: Job? = null

    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0
    private var soundLoaded = false

    init{
        setupSoundPool()
    }

    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)  /* sonification means that this
                                                                        audio file is to be treated as alert
                                                                        and not music or any other media*/
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            soundLoaded = status == 0
        }
        clickSoundId = soundPool?.load(context, R.raw.metronome_tick, 1)?: 0
    }

    fun start(bpm: Int, useSound: Boolean){
        stop()
        val intervalMills = (60000L / bpm)

        metronomeJob = metronomeScope.launch {
            while(true){
                tick(useSound)
                delay(intervalMills.milliseconds)
            }
        }
    }

    fun stop() {
        metronomeJob?.cancel()
        metronomeJob = null
    }

    fun shutdown(){
        stop()
        soundPool?.release()
        soundPool = null
        metronomeScope.cancel()
    }


    private fun tick(useSound: Boolean){
        if(useSound && soundLoaded){
            soundPool?.play(clickSoundId, 1f, 1f, 1, 0, 1f)
        }else{
            hapticsManager.tick()
        }

    }
}