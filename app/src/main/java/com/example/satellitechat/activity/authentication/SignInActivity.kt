package com.example.satellitechat.activity.authentication

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_sign_in.*

open class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var dialog: Dialog

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
        sharedPreferences = getSharedPreferences("remember_me", MODE_PRIVATE)
        val getEmail = sharedPreferences.getString("user_email", "")
        val getPassword = sharedPreferences.getString("password", "")
        if(getEmail != null && getPassword != null) {
            inputEmail.setText(getEmail)
            inputPassword.setText(getPassword)
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
                                setSignIn(auth.currentUser!!.uid)
                                startActivity(Intent(this, MainActivity::class.java))
                                dialog.dismiss()
                                Toast.makeText(this, "????ng nh???p th??nh c??ng!", Toast.LENGTH_SHORT).show()
                            } else {
                                dialog.dismiss()
                                Toast.makeText(this, "Vui l??ng v??o email ???? ????ng k?? x??c th???c t??i kho???n!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            dialog.dismiss()
                            inputEmail.setText("")
                            inputPassword.setText("")
                            Toast.makeText(this, "Email ho???c m???t kh???u kh??ng ch??nh x??c vui l??ng th??? l???i!", Toast.LENGTH_SHORT).show()
                        }
                    }

                if(remember_me.isChecked) {
                    setRememberMe(email, password)
                } else {
                    setRememberMe("", "")
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
                inputEmail.error = "Vui l??ng nh???p email m?? b???n ???? ????ng k??!";
            } else {
                auth.sendPasswordResetEmail(inputEmail.text.toString())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Vui l??ng ki???m tra email c???a b???n!", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener{
                        Toast.makeText(this, "C?? l???i x???y ra, vui l??ng th??? l???i!", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
        val getIsSignIn = sharedPreferences.getString("is_sign_in", "")
        if(getIsSignIn == "true") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setRememberMe(email: String, password: String) {
        sharedPreferences = getSharedPreferences("remember_me", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        editor.putString("user_email", email)
        editor.putString("password", password)
        editor.apply()
    }

    private fun setSignIn(userId: String) {
        sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        editor.putString("is_sign_in", "true")
        editor.putString("method_sign_in", "account")
        editor.putString("userId", userId)
        editor.apply()
    }

    private fun validator(email: EditText, password: EditText): Boolean {
        val error = "Vui l??ng nh???p tr?????ng n??y!"
        if(email.text.toString().isEmpty()) {
            email.error = error
            return false
        } else {
            if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
                email.error = "Vui l??ng nh???p ????ng ?????nh d???ng email!"
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