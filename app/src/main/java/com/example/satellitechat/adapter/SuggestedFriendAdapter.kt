package com.example.satellitechat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.Relationship
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.example.satellitechat.utilities.time.TimeAndDateGeneral
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.*

class SuggestedFriendAdapter(
    private val context: Context,
    private val listSuggestionFriends: ArrayList<User>
) :
    RecyclerView.Adapter<SuggestedFriendAdapter.ViewHolder>() {

    private var currentUserId: String = ""
    private lateinit var relationshipsRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = listSuggestionFriends[position]
        preferenceManager = PreferenceManager(context)
        relationshipsRef = FirebaseDatabase.getInstance().getReference(Constants.RELATIONSHIPS_REF)
        currentUserId = preferenceManager.getCurrentId().toString()

        setButtonAndInfoSuggestionFriend(holder, friend)

        holder.btnAddFriend.setOnClickListener {
            handlerButtonAddFriend(holder, friend)
        }

        holder.btnRemoveFriend.setOnClickListener {
            handlerButtonRemoveSuggested(holder, friend, position)
        }

        holder.btnCancelRequest.setOnClickListener {
            handlerButtonCancelRequest(holder, friend)
        }

    }

    override fun getItemCount(): Int {
        return listSuggestionFriends.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val friendName: TextView = view.findViewById<TextView>(R.id.friendName)
        val imageFriend: ShapeableImageView = view.findViewById<ShapeableImageView>(R.id.imageFriend)
        val btnAddFriend: TextView = view.findViewById<TextView>(R.id.btnAddFriend)
        val btnRemoveFriend: TextView = view.findViewById<TextView>(R.id.btnRemoveFriend)
        val btnCancelRequest: TextView = view.findViewById<TextView>(R.id.btnCancelRequest)
        val suggestionFriend: TextView = view.findViewById<TextView>(R.id.suggestionFriend)
        val itemBoxSuggestedFriend: LinearLayout = view.findViewById<LinearLayout>(R.id.itemBoxSuggestedFriend)
    }

    private fun setButtonAndInfoSuggestionFriend(holder: ViewHolder, friend: User) {
        holder.friendName.text = friend.userName
        Glide.with(context).load(friend.userImage).placeholder(R.drawable.profile_image)
            .into(holder.imageFriend)
        relationshipsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var relationship = Relationship()
                var requestStatus = String()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    relationship = dataSnapshot.getValue<Relationship>(Relationship::class.java)!!
                    requestStatus = dataSnapshot.child("requestStatus").value.toString()
                    if (relationship.requestReceiverId == friend.userId && requestStatus == "sent") {
                        holder.btnAddFriend.visibility = View.GONE
                        holder.btnRemoveFriend.visibility = View.GONE
                        holder.btnCancelRequest.visibility = View.VISIBLE
                        holder.suggestionFriend.text = "Request sent"
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handlerButtonRemoveSuggested(holder: ViewHolder, friend: User, position: Int) {
        val hashMap: HashMap<String, Any> = HashMap()
        val relationshipId = "${currentUserId}_${friend.userId}"
        hashMap["relationshipId"] = relationshipId
        hashMap["requestSenderId"] = currentUserId
        hashMap["requestReceiverId"] = friend.userId
        hashMap["requestStatus"] = "removed"
        hashMap["requestSentTime"] = TimeAndDateGeneral().getCurrentTimeAndDate()
        hashMap["requestAcceptedTime"] = ""
        hashMap["requestRejectedTime"] = ""
        hashMap["requestCancelledTime"] = ""
        relationshipsRef.child(relationshipId).setValue(hashMap)
        holder.itemBoxSuggestedFriend.visibility = View.GONE
        if (position != RecyclerView.NO_POSITION) {
            listSuggestionFriends.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private fun handlerButtonAddFriend(holder: ViewHolder, friend: User) {
        val hashMap: HashMap<String, Any> = HashMap()
        val relationshipId = "${currentUserId}_${friend.userId}"
        hashMap["relationshipId"] = relationshipId
        hashMap["requestSenderId"] = currentUserId
        hashMap["requestReceiverId"] = friend.userId
        hashMap["requestStatus"] = "sent"
        hashMap["requestSentTime"] = TimeAndDateGeneral().getCurrentTimeAndDate()
        hashMap["requestAcceptedTime"] = ""
        hashMap["requestRejectedTime"] = ""
        hashMap["requestCancelledTime"] = ""
        relationshipsRef.child(relationshipId).setValue(hashMap)
        holder.btnAddFriend.visibility = View.GONE
        holder.btnRemoveFriend.visibility = View.GONE
        holder.btnCancelRequest.visibility = View.VISIBLE
        holder.suggestionFriend.text = "Request sent"
    }

    private fun handlerButtonCancelRequest(holder: ViewHolder, friend: User) {
        val relationshipId = "${currentUserId}_${friend.userId}"
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["requestStatus"] = "cancelled"
        hashMap["requestCancelledTime"] = TimeAndDateGeneral().getCurrentTimeAndDate()
        relationshipsRef.child(relationshipId).updateChildren(hashMap)
        holder.btnAddFriend.visibility = View.VISIBLE
        holder.btnRemoveFriend.visibility = View.VISIBLE
        holder.btnCancelRequest.visibility = View.GONE
        holder.suggestionFriend.text = "Suggested friend"
    }

}