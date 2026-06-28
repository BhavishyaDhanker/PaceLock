package com.example.pacelock.Configration

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pacelock.Data.CoachingSettings
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("coaching_settings")



class ConfigurationRepository(private val context: Context) {

    companion object{
        val KEY_TTS = booleanPreferencesKey("tts_enabled")
        val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
        val KEY_METRONOME = booleanPreferencesKey("metronome_enabled")
        val KEY_BPM = intPreferencesKey("metronome_bpm")
        val KEY_METRONOME_SOUND = booleanPreferencesKey("metronome_sound")
        val KEY_TARGET_PACE = floatPreferencesKey("target_pace")
    }


    val settingsFlow = context.dataStore.data.map{
        CoachingSettings(
            ttsEnabled = it[KEY_TTS] ?: true,
            hapticsEnabled = it[KEY_HAPTICS] ?: false,
            metronomeEnabled = it[KEY_METRONOME] ?: false,
            metronomeBpm = it[KEY_BPM] ?: 160,
            metronomeUseSound = it[KEY_METRONOME_SOUND] ?: true,
            targetPacePerSecPerKm = it[KEY_TARGET_PACE] ?: 0f
        )

    }

    suspend fun updateTTS(flag : Boolean){
        context.dataStore.edit {
            it[KEY_TTS] = flag
        }
    }

    suspend fun updateHaptics(flag : Boolean){
        context.dataStore.edit {
            it[KEY_HAPTICS] = flag
        }
    }

    suspend fun updateMetronome(flag : Boolean){
        context.dataStore.edit {
            it[KEY_METRONOME] = flag
        }
    }

    suspend fun updateBPM(bpm: Int){
        context.dataStore.edit {
            it[KEY_BPM] = bpm
        }
    }

    suspend fun updateMetronomeSound(flag : Boolean){
        context.dataStore.edit {
            it[KEY_METRONOME_SOUND] = flag
        }
    }

    suspend fun updateTargetPacePerSecPerKm(flag: Float){
        context.dataStore.edit{
            it[KEY_TARGET_PACE] = flag
        }
    }

}