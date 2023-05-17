package com.example.satellitechat.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.example.satellitechat.utilities.time.TimeAndDateGeneral
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.*
import kotlin.collections.ArrayList

class FriendRequestAdapter(private val context: Context, private val listFriendRequests: ArrayList<User>) :
    RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    private var currentUserId: String = ""
    private lateinit var relationshipsRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = listFriendRequests[position]
        preferenceManager = PreferenceManager(context)
        relationshipsRef = FirebaseDatabase.getInstance().getReference(Constants.RELATIONSHIPS_REF)
        currentUserId = preferenceManager.getCurrentId().toString()

        holder.friendName.text = friend.userName
        holder.suggestionFriend.text = "Send friend request to you"
        holder.btnConfirmFriend.text = "Confirm"
        if(friend.userImage == "") {
            holder.imageFriend.strokeWidth = 1F
            holder.imageFriend.setStrokeColorResource(R.color.black_200)
        }
        Glide.with(context).load(friend.userImage).placeholder(R.drawable.profile_image).into(holder.imageFriend)

        holder.btnConfirmFriend.setOnClickListener {
            val relationshipId = "${friend.userId}_${currentUserId}"
            val hashMap: HashMap<String, Any> = HashMap()
            hashMap["requestStatus"] = "Accepted"
            hashMap["requestAcceptedTime"] = TimeAndDateGeneral().getCurrentTimeAndDate()
            relationshipsRef.child(relationshipId).updateChildren(hashMap)
            holder.btnConfirmFriend.visibility = View.GONE
            holder.btnRemoveFriend.visibility = View.GONE
            holder.btnCancelRequest.visibility = View.VISIBLE
            holder.suggestionFriend.text = "accepted"
        }

    }

    override fun getItemCount(): Int {
        return listFriendRequests.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val friendName: TextView = view.findViewById<TextView>(R.id.friendName)
        val imageFriend: ShapeableImageView = view.findViewById<ShapeableImageView>(R.id.imageFriend)
        val btnConfirmFriend: TextView = view.findViewById<TextView>(R.id.btnAddFriend)
        val btnRemoveFriend: TextView = view.findViewById<TextView>(R.id.btnRemoveFriend)
        val btnCancelRequest: TextView = view.findViewById<TextView>(R.id.btnCancelRequest)
        val suggestionFriend: TextView = view.findViewById<TextView>(R.id.suggestionFriend)
    }

}