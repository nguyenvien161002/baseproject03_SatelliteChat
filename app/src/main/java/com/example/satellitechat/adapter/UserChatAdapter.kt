package com.example.satellitechat.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.ChatActivity
import com.example.satellitechat.model.User
import com.google.android.material.imageview.ShapeableImageView

class UserChatAdapter (private val context: Context, private val userList: ArrayList<User>) :
    RecyclerView.Adapter<UserChatAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.userOrGroupName.text = user.userName

        if(user.userImage == "") {
            holder.imgUserOrGroup.strokeWidth = 1F
            holder.imgUserOrGroup.setStrokeColorResource(R.color.black_200)
            Glide.with(context).load(user.userImage).placeholder(R.drawable.profile_image).into(holder.imgUserOrGroup)
        } else {
            Glide.with(context).load(user.userImage).into(holder.imgUserOrGroup)
        }

        holder.itemBoxChat.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val userOrGroupName: TextView =  view.findViewById(R.id.userOrGroup_name)
        val imgUserOrGroup: ShapeableImageView = view.findViewById(R.id.imageUserOrGroup)
        val itemBoxChat: LinearLayout = view.findViewById(R.id.itemBoxChat)
    }
}