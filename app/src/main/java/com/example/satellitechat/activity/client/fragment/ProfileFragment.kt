package com.example.satellitechat.activity.client.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.Status
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_profile.view.*

class ProfileFragment : Fragment() {

    private var currentUserId: String = ""
    private var listPostStatus = ArrayList<Status>()
    private lateinit var usersRef: DatabaseReference
    private lateinit var statusesRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        preferenceManager = PreferenceManager(view.context)
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        statusesRef = FirebaseDatabase.getInstance().getReference(Constants.STATUSES_REF)
        currentUserId = preferenceManager.getCurrentId().toString()

        // Get info user from database
        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)!!
                if (isAdded) {
                    view.userNameProfile.text = user.userName
                    Glide.with(view.context).load(user.userImage).placeholder(R.drawable.profile_image).into(view.imageProfile)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        // Get posts status from database
        statusesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listPostStatus.clear()
                var posterId = ""
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val status = snapshot.getValue(Status::class.java)!!
                    posterId = dataSnapshot.child("posterId").value.toString()
                    if (currentUserId == posterId) {
                        listPostStatus.add(status)
                    }
                }
                view.labelNumberOfPostsStatus.text = listPostStatus.size.toString()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        return view
    }
}