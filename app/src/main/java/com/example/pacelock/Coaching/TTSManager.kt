package com.example.pacelock.Coaching

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(context: Context): TextToSpeech.OnInitListener{

    private val tts : TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var pendingQueue = mutableListOf<String>()


    override fun onInit(status: Int) {
        if(status == TextToSpeech.SUCCESS){
            val result = tts.setLanguage(Locale.US)
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.e("TTS", "Language not supported")
                return
            }

            tts.setSpeechRate(0.95f)
            isReady = true
            pendingQueue.forEach { speak(it) }
            pendingQueue.clear()
        }else{
            Log.e("TTS", "TTS initialization failed")
        }
    }

    private fun speak(message: String) {
        if(isReady){
            tts.speak(
                message,
                TextToSpeech.QUEUE_ADD,
                null,   // We don't need any bundle
                System.currentTimeMillis().toString()
            )
        }else{
            pendingQueue.add(message)
        }
    }

    fun stop(){ if(tts.isSpeaking) tts.stop()}

    fun shutdown(){
        tts.stop()
        tts.shutdown()
    }

}