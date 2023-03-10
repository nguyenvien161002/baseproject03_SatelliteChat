package com.example.satellitechat.activity.authentication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class GgAuthActivity : SignInActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        userRef = FirebaseDatabase.getInstance().getReference("Users")
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this@GgAuthActivity, options)
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResults(task)
            } else {
                Toast.makeText(this, "H???y ????ng nh???p b???ng google!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            getDataFromGg()
        } else {
            Toast.makeText(this, "H???y ????ng nh???p b???ng google!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDataFromGg() {
        val info: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (info != null) {
            val hashMap: HashMap<String, Any> = HashMap()
            hashMap["userId"] = info.id.toString()
            hashMap["userName"] = info.displayName.toString()
            hashMap["userEmail"] = info.email.toString()
            hashMap["userImage"] = info.photoUrl.toString()
            hashMap["password"] = ""
            hashMap["methodSignIn"] = "google"
            // Save data to firebase
            userRef.child(info.id.toString()).setValue(hashMap)
                .addOnCompleteListener(this) {
                    if (it.isSuccessful) {
                        sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
                        editor = sharedPreferences.edit()
                        editor.putString("is_sign_in", "true")
                        editor.putString("method_sign_in", "google")
                        editor.putString("userId", info.id.toString())
                        editor.apply()
                        val intent = Intent(this@GgAuthActivity, MainActivity::class.java)
                        startActivity(intent)
                        Toast.makeText(this@GgAuthActivity, "????ng nh???p th??nh c??ng!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
        }
    }
}