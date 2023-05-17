package com.example.satellitechat.activity.client.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.satellitechat.R
import com.example.satellitechat.adapter.FriendRequestAdapter
import com.example.satellitechat.adapter.SuggestedFriendAdapter
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_friend_requests.view.*
import kotlinx.android.synthetic.main.fragment_suggested_friends.view.*

class FriendRequestsFragment : Fragment() {

    private var currentUserId: String = ""
    private var listFriendRequests = ArrayList<User>()
    private lateinit var usersRef: DatabaseReference
    private lateinit var relationshipRef: DatabaseReference
    private lateinit var friendRequestAdapter: FriendRequestAdapter
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_friend_requests, container, false)

        preferenceManager = PreferenceManager(view.context)
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        relationshipRef = FirebaseDatabase.getInstance().getReference(Constants.RELATIONSHIPS_REF)
        currentUserId = preferenceManager.getCurrentId().toString()

        getDataFriendRequests(view)

        return view
    }

    private fun getDataFriendRequests(view: View) {
        view.friendRequestsRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        view.progressBarFriendRequest.visibility = ProgressBar.VISIBLE
        listFriendRequests.clear()
        relationshipRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val requestReceiverId = dataSnapshot.child("requestReceiverId").value.toString()
                    val requestSenderId = dataSnapshot.child("requestSenderId").value.toString()
                    val requestStatus = dataSnapshot.child("requestStatus").value.toString()
                    if(requestReceiverId == currentUserId && requestStatus == "sent") {
                        usersRef.child(requestSenderId).addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val friend: User = snapshot.getValue<User>(User::class.java)!!
                                listFriendRequests.add(friend)
                                friendRequestAdapter = FriendRequestAdapter(view.context, listFriendRequests)
                                view.friendRequestsRecyclerView.adapter = friendRequestAdapter
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
                            }
                        })
                    }
                }
                view.progressBarFriendRequest.visibility = ProgressBar.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

}