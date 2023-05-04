package com.example.satellitechat.activity.client.switchAccounts

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.satellitechat.R
import com.example.satellitechat.adapter.UserSwitchAdapter
import com.example.satellitechat.model.User
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_switch_accounts.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.satellitechat.activity.authentication.SignInActivity
import com.example.satellitechat.activity.client.MainActivity
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_switch_accounts.icon_backspace
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SwitchAccountsActivity : AppCompatActivity() {

    private var userSwitchList = ArrayList<User>()
    private var currentUserId: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var loginManager: LoginManager
    private lateinit var usersRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_accounts)

        // Init auth
        auth = Firebase.auth

        // Check user method sign in application
        preferenceManager = PreferenceManager(this@SwitchAccountsActivity)
        currentUserId = preferenceManager.getCurrentId().toString()
        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        // Logout account
        loginManager = LoginManager.getInstance()
        btnSignOut.setOnClickListener {
            auth.signOut()
            loginManager.logOut()
            preferenceManager.logOut()
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
        getUserLogged(currentUserId)
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

    @SuppressLint("SimpleDateFormat")
    private fun updateUserState() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val timeFormat = SimpleDateFormat("hh:mm:ss a")
        val currentTime: String = timeFormat.format(System.currentTimeMillis())
        val currentDate: String = dateFormat.format(System.currentTimeMillis())
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["time"] = currentTime
        hashMap["date"] = currentDate
        hashMap["type"] = "offline"
        usersRef.child(currentUserId).child("userState").updateChildren(hashMap)
        usersRef.child(currentUserId).child("fcmToken").setValue(null)
    }

}