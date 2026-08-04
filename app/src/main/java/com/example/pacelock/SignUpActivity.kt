package com.example.pacelock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        val etAthleteName = findViewById<EditText>(R.id.etAthleteName)
        val etPaceGoal = findViewById<EditText>(R.id.etPaceGoal)
        val etCommunication = findViewById<EditText>(R.id.etCommunication)
        val etCipher = findViewById<EditText>(R.id.etCipher)
        val btnInitializeSession = findViewById<AppCompatButton>(R.id.btnInitializeSession)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        btnInitializeSession.setOnClickListener {
            val name = etAthleteName.text.toString().trim()
            val pace = etPaceGoal.text.toString().trim()
            val email = etCommunication.text.toString().trim()
            val password = etCipher.text.toString().trim()


            if (name.isEmpty() || pace.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please configure all elite parameters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {


                        val userId = auth.currentUser?.uid

                        if (userId != null) {

                            val userProfile = hashMapOf(
                                "name" to name,
                                "paceGoal" to pace,
                                "email" to email,
                                "createdAt" to System.currentTimeMillis()
                            )


                            firestore.collection("users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    // Successfully written to Firestore
                                    Toast.makeText(this, "Elite status confirmed.", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    // Auth succeeded but database write failed
                                    Toast.makeText(this, "Failed to sync profile: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        // Auth completely failed
                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        Log.e("SignUp", "Firebase Auth failed", task.exception)
                    }
                }
        }
        
        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}