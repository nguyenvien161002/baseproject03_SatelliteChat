package com.example.satellitechat.activity.client

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.profile.ProfileActivity
import com.example.satellitechat.activity.client.sidebar.ArchivesFragment
import com.example.satellitechat.activity.client.sidebar.ChatFragment
import com.example.satellitechat.activity.client.sidebar.MarketPlaceFragment
import com.example.satellitechat.activity.client.sidebar.MessageWaitingFragment
import com.example.satellitechat.activity.client.switch.SwitchAccountsActivity
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.example.satellitechat.utilities.time.CurrentTimeAndDate
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sidebar_header.view.*
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {

    private var backPressedTime: Long = 0
    private var currentUserId: String = ""
    private lateinit var sidebarHeader: View
    private lateinit var usersRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        preferenceManager = PreferenceManager(this@MainActivity)
        currentUserId = preferenceManager.getCurrentId().toString()
        checkPermissions(Constants.NOTIFICATION_PERMISSION)

        btnMenu.setOnClickListener {
            drawer_layout_main.openDrawer(GravityCompat.START)
        }

        sidebarHeader = sidebar_view.getHeaderView(0)

        val arrayTab = arrayOf(
            sidebarHeader.item1_sidebar, sidebarHeader.item2_sidebar,
            sidebarHeader.item3_sidebar, sidebarHeader.item4_sidebar
        )
        val arrayBtnTab = arrayOf(
            sidebarHeader.chat_sidebar, sidebarHeader.marketplace_sidebar,
            sidebarHeader.waiting_message_sidebar, sidebarHeader.archives_sidebar
        )

        // Init first Fragment when first logged in
        replaceFragment(ChatFragment())

        arrayBtnTab.forEach { btn ->
            btn.setOnClickListener {
                removeBackgroundTab(arrayTab)
                when(arrayBtnTab.indexOf(btn)) {
                    0 -> {
                        sidebarHeader.item1_sidebar.setBackgroundResource(R.drawable.bg_item_sidebar_radius)
                        sidebarHeader.item1_sidebar.setPadding(12)
                        replaceFragment(ChatFragment())
                    }

                    1 -> {
                        sidebarHeader.item2_sidebar.setBackgroundResource(R.drawable.bg_item_sidebar_radius)
                        sidebarHeader.item2_sidebar.setPadding(12)
                        replaceFragment(MarketPlaceFragment())
                    }

                    2 -> {
                        sidebarHeader.item3_sidebar.setBackgroundResource(R.drawable.bg_item_sidebar_radius)
                        sidebarHeader.item3_sidebar.setPadding(12)
                        replaceFragment(MessageWaitingFragment())
                    }

                    3 -> {
                        sidebarHeader.item4_sidebar.setBackgroundResource(R.drawable.bg_item_sidebar_radius)
                        sidebarHeader.item4_sidebar.setPadding(12)
                        replaceFragment(ArchivesFragment())
                    }
                }
            }
        }

        // Switch to profile activity
        sidebarHeader.icon_setting.setOnClickListener {
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            startActivityForResult(intent, 1)
            finish()
        }

        // Switch to switch activity
        sidebarHeader.box_account.setOnClickListener {
            val intent = Intent(this, SwitchAccountsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra("userId", currentUserId)
            startActivity(intent)
            finish()
        }

        btnVideoCallPlus.setOnClickListener {
            checkPermissions(Constants.CAMERA_PERMISSION)
        }

    }

    private fun checkPermissions(permission: String) {
        if (permission == Constants.CAMERA_PERMISSION) {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    Constants.CAMERA_PERMISSION_REQUEST
                )
            } else {
                activeCamera()
            }
        } else if (permission == Constants.NOTIFICATION_PERMISSION) {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    Constants.NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        val interval = currentTime - backPressedTime

        if (interval < 2000) {
            super.onBackPressed()
        } else {
            Toast.makeText(this, "Nhấn quay lại một lần nữa để thoát ứng dụng", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.CAMERA_PERMISSION_REQUEST -> {
                // Agree to allow the use
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    activeCamera()
                } else { // Do not agree to allow the use
                    Toast.makeText(this@MainActivity, "Bạn đã từ chối quyền truy cập camera", Toast.LENGTH_LONG).show()
                }
                return
            }
        }

    }

    // CAMERA CAPTURE CHAT FRAGMENT
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Constants.IMAGE_CAPTURE_REQUEST && resultCode != null) {
            val bitmap: Bitmap? = data!!.getParcelableExtra<Bitmap>("data")
        }
    }

    override fun onStart() {
        super.onStart()
        getUserForSidebar(currentUserId)
        val hashMap = getCurrentTimeAndDate()
        updateUserState("online", hashMap)
        sendFCMTokenToDatabase()
    }

    override fun onStop() {
        super.onStop()
        val hashMap = getCurrentTimeAndDate()
        hashMap["type"] = "offline"
        usersRef.child(currentUserId).child("userState").onDisconnect().setValue(hashMap)
    }

    private fun replaceFragment(fragment: Fragment) {
        val isOpen = drawer_layout_main.isDrawerOpen(GravityCompat.START)
        if(isOpen) {
            drawer_layout_main.closeDrawer(GravityCompat.START)
        }
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }

    private fun removeBackgroundTab(array: Array<ConstraintLayout>) {
        array.forEach {
            it.setBackgroundResource(0)
        }
    }

    private fun getUserForSidebar(currentUserId: String) {
        val userRef: DatabaseReference = usersRef.child(currentUserId)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val infoUser = snapshot.getValue(User::class.java)
                sidebarHeader.userName.text = infoUser!!.userName
                titleUsername.text = infoUser.userName
                preferenceManager.setNameAndImage(infoUser.userName, infoUser.userImage)
                if (infoUser.userImage == "") {
                    sidebarHeader.imageProfile.setImageResource(R.drawable.profile_image)
                    sidebarHeader.imageProfile.strokeWidth = 2F
                    sidebarHeader.imageProfile.setStrokeColorResource(R.color.primary)
                } else {
                    if (this@MainActivity.isDestroyed) {
                        return;
                    } else {
                        Glide.with(this@MainActivity).load(infoUser.userImage).into(sidebarHeader.imageProfile)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun activeCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, Constants.IMAGE_CAPTURE_REQUEST)
    }

    private fun updateUserState(state: String, hashMap: HashMap<String, Any>) {
        hashMap["type"] = state
        usersRef.child(currentUserId).child("userState").updateChildren(hashMap)
    }
    
    private fun sendFCMTokenToDatabase() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener( OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM fail", "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }
                // Get token success
                usersRef.child(currentUserId).child("fcmToken").setValue(task.result)
            })
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCurrentTimeAndDate(): java.util.HashMap<String, Any> {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val timeFormat = SimpleDateFormat("hh:mm:ss a")
        val currentDate: String = dateFormat.format(System.currentTimeMillis())
        val currentTime: String = timeFormat.format(System.currentTimeMillis())
        val messageState: java.util.HashMap<String, Any> = java.util.HashMap()
        messageState["date"] = currentDate
        messageState["time"] = currentTime
        return messageState
    }


}



