package com.example.satellitechat.activity.client

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.example.satellitechat.R
import com.example.satellitechat.activity.authentication.SignInActivity
import com.example.satellitechat.activity.client.sidebar.ArchivesFragment
import com.example.satellitechat.activity.client.sidebar.ChatFragment
import com.example.satellitechat.activity.client.sidebar.MarketPlaceFragment
import com.example.satellitechat.activity.client.sidebar.MessageWaitingFragment
import com.example.satellitechat.adapter.UserAdapter
import com.example.satellitechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.android.synthetic.main.item_box_chat.*
import kotlinx.android.synthetic.main.sidebar_header.view.*


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private var backPressTime: Long = 0;
    private lateinit var fToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = Firebase.auth

        btnMenu.setOnClickListener {
            drawer_layout_main.openDrawer(GravityCompat.START)
        }

        val sidebarHeader = sidebar_view.getHeaderView(0)
        val arrayTab = arrayOf(sidebarHeader.item1_sidebar, sidebarHeader.item2_sidebar, sidebarHeader.item3_sidebar, sidebarHeader.item4_sidebar)
        val arrayBtnTab = arrayOf(sidebarHeader.chat_sidebar, sidebarHeader.marketplace_sidebar, sidebarHeader.waiting_message_sidebar, sidebarHeader.archives_sidebar)
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

        sidebarHeader.icon_setting.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

    }

    override fun onStart() {
        super.onStart()
        sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
        val getIsSignIn = sharedPreferences.getString("is_sign_in", "")
        if(getIsSignIn == "false") {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
        if((backPressTime + 2000) > System.currentTimeMillis()) {
            fToast.cancel()
            super.onBackPressed()
            return
        } else {
            fToast = Toast.makeText(this, "Nhấn quay lại một lần nữa để thoát ứng dụng", Toast.LENGTH_LONG)
            fToast.show()
        }
        backPressTime = System.currentTimeMillis()
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

}



