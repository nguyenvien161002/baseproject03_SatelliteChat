package com.example.satellitechat.activity.client.sidebar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.satellitechat.R
import com.example.satellitechat.adapter.UserChatAdapter
import com.example.satellitechat.adapter.UserOnlineAdapter
import com.example.satellitechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

import kotlinx.android.synthetic.main.fragment_chat.view.*

class ChatFragment : Fragment() {

    private val userChatList = ArrayList<User>()
    private val userOnlineList = ArrayList<User>()
    private lateinit var userChatAdapter: UserChatAdapter
    private lateinit var userOnlineAdapter: UserOnlineAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false);
        auth = Firebase.auth
        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        // Get user chat box
        view.userRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userChatList.clear()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if(user!!.userId != auth.currentUser!!.uid) {
                        userChatList.add(user)
                    }
                }
                userChatAdapter = UserChatAdapter(view.context, userChatList)
                view.userRecyclerView.adapter = userChatAdapter
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
                    if(user!!.userId != auth.currentUser!!.uid && type == "online") {
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

        return view;
    }


}



