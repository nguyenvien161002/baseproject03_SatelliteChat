package com.example.satellitechat.activity.client.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.satellitechat.R
import com.example.satellitechat.adapter.SuggestedFriendAdapter
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_suggested_friends.view.*

class SuggestedFriendsFragment : Fragment() {

    private var currentUserId: String = ""
    private var listSuggestionsFriend = ArrayList<User>()
    private lateinit var usersRef: DatabaseReference
    private lateinit var relationshipRef: DatabaseReference
    private lateinit var suggestedFriendAdapter: SuggestedFriendAdapter
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_suggested_friends, container, false)

        preferenceManager = PreferenceManager(view.context)
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        relationshipRef = FirebaseDatabase.getInstance().getReference(Constants.RELATIONSHIPS_REF)
        currentUserId = preferenceManager.getCurrentId().toString()

        getDataSuggestedFriends(view)

        return view
    }

    private fun getDataSuggestedFriends(view: View) {
        view.suggestedFriendsRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        listSuggestionsFriend.clear()
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var friend: User = User()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    friend = dataSnapshot.getValue(User::class.java)!!
                    if (friend.userId != currentUserId) {
                        listSuggestionsFriend.add(friend)
                    }
                }
                suggestedFriendAdapter = SuggestedFriendAdapter(view.context, listSuggestionsFriend)
                view.suggestedFriendsRecyclerView.adapter = suggestedFriendAdapter
                view.progressBarSuggestionFriends.visibility = ProgressBar.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

}