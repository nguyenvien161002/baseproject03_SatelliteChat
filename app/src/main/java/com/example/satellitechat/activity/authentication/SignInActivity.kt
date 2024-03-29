package com.example.satellitechat.activity.authentication

import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.MainActivity
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_sign_in.*

open class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dialog: Dialog
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Initialize Firebase Auth
        auth = Firebase.auth
        dialog = Dialog(this@SignInActivity)
        dialog.setContentView(R.layout.dialog_loading)
        if(dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.setCancelable(false)

        // Go through the sign up interface
        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        // Check user has been every password not
        preferenceManager = PreferenceManager(this@SignInActivity)
        val isRememberMe = preferenceManager.getRememberMe()
        if(isRememberMe.size != 0) {
            inputEmail.setText(isRememberMe[0])
            inputPassword.setText(isRememberMe[1])
        }

        // Authentication sign in account
        btnSignIn.setOnClickListener {
            val email = inputEmail.text.toString()
            val password = inputPassword.text.toString()
            val isValid = validator(inputEmail, inputPassword)
            if(isValid) {
                dialog.show()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) {
                        if(it.isSuccessful) {
                            val isVerify = auth.currentUser?.isEmailVerified
                            if(isVerify == true) {
                                preferenceManager.setSignIn(auth.currentUser!!.uid)
                                startActivity(Intent(this, MainActivity::class.java))
                                dialog.dismiss()
                                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                            } else {
                                dialog.dismiss()
                                Toast.makeText(this, "Vui lòng vào email đã đăng kí xác thực tài khoản!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            dialog.dismiss()
                            inputEmail.setText("")
                            inputPassword.setText("")
                            Toast.makeText(this, "Email hoặc mật khẩu không chính xác vui lòng thử lại!", Toast.LENGTH_SHORT).show()
                        }
                    }

                if(remember_me.isChecked) {
                    preferenceManager.setRememberMe(email, password)
                } else {
                    preferenceManager.setRememberMe("", "")
                }
            }
        }

        // Authentication sign in with google
        btnSignInGg.setOnClickListener {
            val intent = Intent(this, GgAuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        // Authentication sign in with facebook
        btnSignInFb.setOnClickListener {
            val intent = Intent(this, FbAuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        // Authentication forgot password
        forgot_password.setOnClickListener {
            if (inputEmail.text.toString().isEmpty()) {
                inputEmail.error = "Vui lòng nhập email mà bạn đã đăng ký!";
            } else {
                auth.sendPasswordResetEmail(inputEmail.text.toString())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Vui lòng kiểm tra email của bạn!", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener{
                        Toast.makeText(this, "Có lỗi xảy ra, vui lòng thử lại!", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(preferenceManager.getIsSignIn() == "true") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun validator(email: EditText, password: EditText): Boolean {
        val error = "Vui lòng nhập trường này!"
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
        return true
    }
}