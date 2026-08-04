package com.example.pacelock.com.example.pacelock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.pacelock.HomeActivity
import com.example.pacelock.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { true }

        auth = FirebaseAuth.getInstance()


        checkSessionAndRoute()
    }

    private fun checkSessionAndRoute() {
        val currentUser = auth.currentUser

        val intent = if (currentUser != null) {

            Intent(this, HomeActivity::class.java)
        } else {

            Intent(this, LoginActivity::class.java)
        }

        // Adding flags to clear the backstack so the user can't hit "Back" to return to the Splash Screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        finish()
    }
}