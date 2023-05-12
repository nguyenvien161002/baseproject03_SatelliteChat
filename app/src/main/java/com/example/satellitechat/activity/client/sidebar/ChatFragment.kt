package com.example.satellitechat.activity.client.sidebar

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.post.PostStatusActivity
import com.example.satellitechat.adapter.UserChatAdapter
import com.example.satellitechat.adapter.UserOnlineAdapter
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_chat.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.io.path.fileVisitor

class ChatFragment : Fragment() {

    private val userChatList = ArrayList<User>()
    private val userOnlineList = ArrayList<User>()
    private var currentUserId: String = ""
    private var statusId: String = ""
    private lateinit var userChatAdapter: UserChatAdapter
    private lateinit var userOnlineAdapter: UserOnlineAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference
    private lateinit var statusesRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false);
        auth = Firebase.auth
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        statusesRef = FirebaseDatabase.getInstance().getReference(Constants.STATUSES_REF)
        preferenceManager = PreferenceManager(requireContext())
        currentUserId = preferenceManager.getCurrentId().toString()
        view.progressBarChatBox.visibility = ProgressBar.VISIBLE

        view.btnLeaveANote.setOnClickListener {
            val intent = Intent(view.context, PostStatusActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Get img user for contentStatusContainer
        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)!!
                if (isAdded) {
                    Glide.with(view).load(user.userImage).placeholder(R.drawable.profile_image).into(view.imageUser)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })


        // Get user chat box
        view.userRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userChatList.clear()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if(user!!.userId != currentUserId) {
                        userChatList.add(user)
                    }
                }
                userChatAdapter = UserChatAdapter(view.context, userChatList)
                view.userRecyclerView.adapter = userChatAdapter
                view.progressBarChatBox.visibility = ProgressBar.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        // Get users online
        view.userOnlineRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userOnlineList.clear()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)
                    val type = dataSnapshot.child("userState").child("type").value
                    if(user!!.userId != currentUserId && type == "online") {
                        userOnlineList.add(user)
                    }
                }
                userOnlineAdapter = UserOnlineAdapter(view.context, userOnlineList)
                view.userOnlineRecyclerView.adapter = userOnlineAdapter
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        // Get status posted for user
        statusId = preferenceManager.getString(Constants.STATUS_ID).toString()
        statusesRef.child(statusId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userId = snapshot.child("posterId").value
                if (userId == currentUserId) {
                    val contentStatus = snapshot.child("contentStatus").value
                    val expiry = snapshot.child("expiry").value
                    val statusIdDb = snapshot.child("statusId").value.toString()
                    Handler(Looper.getMainLooper()).postDelayed({
                        statusesRef.child(statusIdDb).child("expiry").setValue("false")
                    }, 24*60*60*1000)
                    if (expiry == "true") { // check xem đã đăng qua 24h chưa
                        view.btnLeaveANote.visibility = View.GONE
                        view.contentStatusContainer.visibility = View.VISIBLE
                        view.labelLeaveANote.text = "Your note"
                        view.contentStatus.text = contentStatus as CharSequence?
                    } else {
                        view.btnLeaveANote.visibility = View.VISIBLE
                        view.contentStatusContainer.visibility = View.GONE
                    }
                } else {
                    view.btnLeaveANote.visibility = View.VISIBLE
                    view.contentStatusContainer.visibility = View.GONE
                }

            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        view.contentStatusContainer.setOnClickListener {
            Log.d("DELETE_STATUS", "DELETE_STATUS")
            Toast.makeText(view.context, "DELETE_STATUS", Toast.LENGTH_LONG).show()
        }

        return view;
    }

}



