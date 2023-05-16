package com.example.satellitechat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.adapter.diffUtil.UserOnlineDiffCallback
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.*

class UserOnlineAdapter (private val context: Context, private val userOnlineList: ArrayList<User>) :
    RecyclerView.Adapter<UserOnlineAdapter.ViewHolder>() {

    private lateinit var statusesRef: DatabaseReference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_online, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userOnlineList[position]

        statusesRef = FirebaseDatabase.getInstance().getReference(Constants.STATUSES_REF)
        statusesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val posterId = dataSnapshot.child("posterId").value
                    val expiry = dataSnapshot.child("expiry").value
                    if (user.userId == posterId && expiry == "true") {
                        val contentStatus = dataSnapshot.child("contentStatus").value
                        holder.contentStatusContainer.visibility = View.VISIBLE
                        holder.contentStatus.text = contentStatus.toString()
                    } else {
                        holder.contentStatusContainer.visibility = View.GONE
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        })


        holder.userNameOnline.text = user.userName
        if(user.userImage == "") {
            holder.imageUserOnline.strokeWidth = 1F
            holder.imageUserOnline.setStrokeColorResource(R.color.black_200)
            Glide.with(context).load(user.userImage).placeholder(R.drawable.profile_image).into(holder.imageUserOnline)
        } else {
            Glide.with(context).load(user.userImage).into(holder.imageUserOnline)
        }

    }

    override fun getItemCount(): Int {
        return userOnlineList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val userNameOnline: TextView =  view.findViewById(R.id.userNameOnline)
        val imageUserOnline: ShapeableImageView = view.findViewById(R.id.imageUserOnline)
        val contentStatusContainer: ConstraintLayout = view.findViewById(R.id.contentStatusContainer)
        val contentStatus: TextView = view.findViewById(R.id.contentStatus)
    }

    // Trong class MessageAdapter
    fun updateUserOnlineList(newList: ArrayList<User>) {
        val diffCallback = UserOnlineDiffCallback(userOnlineList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        userOnlineList.clear()
        userOnlineList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }


}