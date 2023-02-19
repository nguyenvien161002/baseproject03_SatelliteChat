package com.example.satellitechat.activity.client

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.adapter.UserSwitchAdapter
import com.example.satellitechat.model.User
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_switch_accounts.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.satellitechat.activity.authentication.SignInActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_switch_accounts.icon_backspace
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.android.synthetic.main.item_box_switch.*

class SwitchAccountsActivity : AppCompatActivity() {

    private var userSwitchList = ArrayList<User>()
    private var currentUserID: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var loginManager: LoginManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var usersRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_accounts)

        auth = Firebase.auth
        currentUserID = auth.currentUser!!.uid
        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        // Logout account
        loginManager = LoginManager.getInstance()
        btnSignOut.setOnClickListener {
            auth.signOut()
            loginManager.logOut()
            sharedPreferences = getSharedPreferences("is_sign_in", AppCompatActivity.MODE_PRIVATE)
            editor = sharedPreferences.edit()
            editor.putString("is_sign_in", "false")
            editor.apply()
            updateUserState()
            startActivity(Intent(this@SwitchAccountsActivity, SignInActivity::class.java))
            finish()
        }

        // Back step
        icon_backspace.setOnClickListener {
            val intent = Intent(this@SwitchAccountsActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

    }

    override fun onStart() {
        super.onStart()
        // Get user logged
        getUserLogged(currentUserID)
    }

    private fun getUserLogged(userId: String) {
        usersRef.child(userId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                userSwitchList.clear()
                val user = snapshot.getValue(User::class.java)
                userSwitchList.add(user!!)
                switchAccountsRecyclerView.layoutManager = LinearLayoutManager(this@SwitchAccountsActivity, RecyclerView.VERTICAL, false)
                val userSwitchAdapter = UserSwitchAdapter(this@SwitchAccountsActivity, userSwitchList)
                switchAccountsRecyclerView.adapter = userSwitchAdapter
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SwitchAccountsActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateUserState() {
        usersRef.child(currentUserID)
            .child("userState").child("type")
            .setValue("offline")
    }

}