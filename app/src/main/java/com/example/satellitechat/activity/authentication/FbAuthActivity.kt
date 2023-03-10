package com.example.satellitechat.activity.authentication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import com.example.satellitechat.activity.client.MainActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import org.json.JSONException
import java.util.*


class FbAuthActivity : SignInActivity() {

    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        userRef = FirebaseDatabase.getInstance().getReference("Users")
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().logInWithReadPermissions(this@FbAuthActivity, listOf("email", "public_profile"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                val graphRequest = GraphRequest.newMeRequest(loginResult.accessToken) { jsonObject, graphResponse ->
                    try {
                        // Adding all user info one by one into TextView.
                        val hashMap: HashMap<String, String> = HashMap()
                        hashMap["userId"] = jsonObject.getString("id")
                        hashMap["userName"] = jsonObject.getString("name")
                        hashMap["userEmail"] = jsonObject.getString("email")
                        hashMap["userImage"] = "https://graph.facebook.com/me/picture?access_token=" + loginResult.accessToken.token
                        hashMap["password"] = ""
                        hashMap["methodSignIn"] = "facebook"
                        userRef.child(jsonObject.getString("id")).setValue(hashMap)
                            .addOnCompleteListener(this@FbAuthActivity) {
                                if (it.isSuccessful) {
                                    sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
                                    editor = sharedPreferences.edit()
                                    editor.putString("is_sign_in", "true")
                                    editor.putString("method_sign_in", "google")
                                    editor.putString("userId", jsonObject.getString("id"))
                                    editor.apply()
                                }
                            }
                            .addOnFailureListener(this@FbAuthActivity) {
                                Toast.makeText(this@FbAuthActivity, it.message, Toast.LENGTH_LONG).show()
                            }
                        handleFacebookAccessToken(loginResult.accessToken)
                    } catch (e: JSONException) {
                        Toast.makeText(this@FbAuthActivity, e.message, Toast.LENGTH_LONG).show()
                        println(e.message)
                    }
                }

                val bundle = Bundle()
                bundle.putString(
                    "fields",
                    "id,name,link,email,gender,last_name,first_name"
                )
                graphRequest.parameters = bundle
                graphRequest.executeAsync()
            }

            override fun onCancel() {
                Toast.makeText(this@FbAuthActivity, "H???y ????ng nh???p b???ng facebook!", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@FbAuthActivity, error.message, Toast.LENGTH_SHORT).show()
                println(error.message)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this@FbAuthActivity, MainActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(baseContext, "????ng nh???p th??nh c??ng!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(baseContext, task.exception.toString(), Toast.LENGTH_SHORT).show()
                }
            }
    }

}