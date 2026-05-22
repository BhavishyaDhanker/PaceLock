package com.example.pacelock

import android.app.Application
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class MyAap: Application() {

    override fun onCreate() {
        super.onCreate()

        Configuration.getInstance().apply {
            load(this@MyAap, PreferenceManager.getDefaultSharedPreferences(this@MyAap))
            userAgentValue = packageName
        }
    }
}