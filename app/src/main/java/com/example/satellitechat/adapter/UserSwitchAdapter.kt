package com.example.satellitechat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.User
import com.google.android.material.imageview.ShapeableImageView

class UserSwitchAdapter(private val context: Context, private val userSwitchList: ArrayList<User>) :
    RecyclerView.Adapter<UserSwitchAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_switch, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userSwitchList[position]
        holder.userNameSwitch.text = user.userName

        if(user.userImage == "") {
            holder.imageUserSwitch.strokeWidth = 1F
            holder.imageUserSwitch.setStrokeColorResource(R.color.black_200)
            Glide.with(context).load(user.userImage).placeholder(R.drawable.profile_image).into(holder.imageUserSwitch)
        } else {
            Glide.with(context).load(user.userImage).into(holder.imageUserSwitch)
        }
    }

    override fun getItemCount(): Int {
        return userSwitchList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val userNameSwitch: TextView =  view.findViewById(R.id.userNameSwitch)
        val imageUserSwitch: ShapeableImageView = view.findViewById(R.id.imageUserSwitch)
    }

}


