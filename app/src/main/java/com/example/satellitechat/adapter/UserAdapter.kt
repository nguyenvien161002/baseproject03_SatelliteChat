package com.example.satellitechat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.User

class UserAdapter(private val context: Context, private val userList: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.userOrGroupName.text = user.userName
        Glide.with(context).load(user.userImage).into(holder.imgUserOrGroup)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val userOrGroupName: TextView =  view.findViewById(R.id.userOrGroup_name)
        val imgUserOrGroup: ImageView = view.findViewById(R.id.imageUserOrGroup)
    }
}