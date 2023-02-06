package com.example.satellitechat.activity.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.satellitechat.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.activity_sign_up.btnSignIn
import kotlinx.android.synthetic.main.activity_sign_up.btnSignInFb
import kotlinx.android.synthetic.main.activity_sign_up.btnSignInGg
import kotlinx.android.synthetic.main.activity_sign_up.btnSignUp
import kotlinx.android.synthetic.main.activity_sign_up.inputEmail
import kotlinx.android.synthetic.main.activity_sign_up.inputPassword

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Go through the sign in interface
        btnSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        // Authentication sign up account
        btnSignUp.setOnClickListener {
            val username = inputUsername.text.toString()
            val email = inputEmail.text.toString()
            val password = inputPassword.text.toString()
            val isValid = validator(inputUsername, inputEmail, inputPassword, inputConfirmPassword)
            if(isValid) {
                onCreateUser(username, email, password);
            }
        }

        // Authentication sign in with google
        btnSignInGg.setOnClickListener {
            val intent = Intent(this, GgAuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // Authentication sign in with facebook
        btnSignInFb.setOnClickListener {
            val intent = Intent(this, FbAuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

    }

    private fun onCreateUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if(it.isSuccessful) {
                    val user:FirebaseUser? = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener(this) {
                            if(it.isSuccessful) {
                                val userId:String = user.uid
                                val hashMap:HashMap<String, String> = HashMap()
                                hashMap["userId"] = userId
                                hashMap["username"] = username
                                hashMap["email"] = email
                                hashMap["password"] = password
                                databaseRef = FirebaseDatabase.getInstance().reference.child("Users").child(userId)
                                databaseRef.setValue(hashMap).addOnCompleteListener(this) {
                                    if(it.isSuccessful) {
                                        startActivity(Intent(this, SignInActivity::class.java))
                                        Toast.makeText(this, "Vui lòng vào email xác thực tài khoản!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                println("Dòng 97:" + it.exception)
                            }
                        }
                } else {
                    println(it.exception.toString());
                    Toast.makeText(this, it.exception.toString(), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validator(username: EditText, email: EditText, password: EditText, confirmPassword: EditText): Boolean {
        val error: String = "Vui lòng nhập trường này!"
        if(username.text.toString().isEmpty()) {
            username.error = error
            return false
        }
        if(email.text.toString().isEmpty()) {
            email.error = error
            return false
        } else {
            if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
                email.error = "Vui lòng nhập đúng định dạng email!"
                return false
            }
        }
        if(password.text.toString().isEmpty()) {
            password.error = error
            return false
        }
        if(confirmPassword.text.toString().isEmpty()) {
            confirmPassword.error = error
            return false
        }
        if(password.text.toString() != confirmPassword.text.toString()) {
            Toast.makeText(this, "Mật khẩu không trùng khớp vui lòng nhập lại!", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
}