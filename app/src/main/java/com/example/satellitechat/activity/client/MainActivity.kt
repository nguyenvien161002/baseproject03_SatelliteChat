package com.example.satellitechat.activity.client

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
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
import com.example.satellitechat.activity.client.switchAccounts.SwitchAccountsActivity
import com.example.satellitechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sidebar_header.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    private var BACK_PRESS_TIME: Long = 0;
    private var IMAGE_CAPTURE_REQUEST: Int = 3000
    private var IMAGE_PERMISSION: Int = 3001
    private var currentUserId: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var fToast: Toast
    private lateinit var sidebarHeader: View
    private lateinit var usersRef: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        usersRef = FirebaseDatabase.getInstance().getReference("Users")
        sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("userId", "").toString()

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
                        title_component.text = getString(R.string.chat_part)
                        sidebarHeader.item1_sidebar.setBackgroundResource(R.drawable.bg_item_sidebar_radius)
                        sidebarHeader.item1_sidebar.setPadding(12)
                        replaceFragment(ChatFragment())
                    }

                    1 -> {
                        title_component.text = getString(R.string.marketplace)
                        sidebarHeader.item2_sidebar.setBackgroundResource(R.drawable.bg_item_sidebar_radius)
                        sidebarHeader.item2_sidebar.setPadding(12)
                        replaceFragment(MarketPlaceFragment())
                    }

                    2 -> {
                        title_component.text = getString(R.string.waiting_message)
                        sidebarHeader.item3_sidebar.setBackgroundResource(R.drawable.bg_item_sidebar_radius)
                        sidebarHeader.item3_sidebar.setPadding(12)
                        replaceFragment(MessageWaitingFragment())
                    }

                    3 -> {
                        title_component.text = getString(R.string.archives)
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

        btnCameraPhoto.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA), IMAGE_PERMISSION)
            } else {
                activeCamera()
            }
        }

    }

    override fun onBackPressed() {
        if((BACK_PRESS_TIME + 2000) > System.currentTimeMillis()) {
            fToast.cancel()
            super.onBackPressed()
            return
        } else {
            fToast = Toast.makeText(this, "Nhấn quay lại một lần nữa để thoát ứng dụng", Toast.LENGTH_LONG)
            fToast.show()
        }
        BACK_PRESS_TIME = System.currentTimeMillis()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == IMAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            activeCamera()
        }
    }

    // CAMERA CAPTURE CHAT FRAGMENT
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == IMAGE_CAPTURE_REQUEST && resultCode != null) {
            val bitmap: Bitmap? = data!!.getParcelableExtra<Bitmap>("data")
        }
    }

    override fun onStart() {
        super.onStart()
        getUserForSidebar(currentUserId)
        val hashMap = getCurrentCalender()
        updateUserState("online", hashMap)
    }

    override fun onStop() {
        super.onStop()
        val hashMap = getCurrentCalender()
        hashMap["type"] = "offline"
        usersRef.child(currentUserId).child("userState").onDisconnect().setValue(hashMap)
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCurrentCalender(): HashMap<String, Any> {
        val dateFormat = SimpleDateFormat("E, dd/MM/yyyy")
        val timeFormat = SimpleDateFormat("hh:mm:ss a")
        val currentTime: String = timeFormat.format(System.currentTimeMillis())
        val currentDate: String = dateFormat.format(System.currentTimeMillis())
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["time"] = currentTime
        hashMap["date"] = currentDate
        return hashMap
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
        startActivityForResult(intent, IMAGE_CAPTURE_REQUEST)
    }

    private fun updateUserState(state: String, hashMap: HashMap<String, Any>) {
        hashMap["type"] = state
        usersRef.child(currentUserId).child("userState").updateChildren(hashMap)
    }

}



