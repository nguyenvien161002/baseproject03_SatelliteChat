package com.example.satellitechat.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.chat.ChatActivity
import com.example.satellitechat.model.SingleChat
import com.example.satellitechat.model.User
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UserChatAdapter (private val context: Context, private val userList: ArrayList<User>) :
    RecyclerView.Adapter<UserChatAdapter.ViewHolder>() {

    private var currentUserId = ""
    private lateinit var usersRef: DatabaseReference
    private lateinit var singleChatsRef: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.userOrGroupName.text = user.userName

        // Check if the userName in the database is empty or not
        if (user.userImage == "") {
            holder.imgUserOrGroup.strokeWidth = 1F
            holder.imgUserOrGroup.setStrokeColorResource(R.color.black_200)
            Glide.with(context).load(user.userImage).placeholder(R.drawable.profile_image).into(holder.imgUserOrGroup)
        } else {
            Glide.with(context).load(user.userImage).into(holder.imgUserOrGroup)
        }

        // Set on click a box chat in main activity
        holder.itemBoxChat.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("userIdReceiver", user.userId)
            context.startActivity(intent)
        }

        // Show the last message in recent conversations
        lastMessage(user.userId, holder.lastMessage, holder.lastTime)

        // Show user online/offline
        iconUserOnline(user.userId, holder.iconOnline)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val userOrGroupName: TextView =  view.findViewById(R.id.userOrGroup_name)
        val imgUserOrGroup: ShapeableImageView = view.findViewById(R.id.imageUserOrGroup)
        val itemBoxChat: LinearLayout = view.findViewById(R.id.itemBoxChat)
        val lastMessage: TextView = view.findViewById(R.id.lastMessage)
        val lastTime: TextView = view.findViewById(R.id.lastTime)
        val iconOnline: ImageView = view.findViewById(R.id.iconOnlineUserOrGroup)
    }

    private fun iconUserOnline(userId: String, iconOnline: ImageView) {
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)!!
                val isOnline = snapshot.child("userState").child("type").value
                if (user.userId == userId && isOnline == "online") {
                    iconOnline.setImageResource(R.drawable.ic_baseline_circle)
                    iconOnline.setBackgroundResource(R.drawable.bg_icon_online_2_rounded)
                } else {
                    iconOnline.setImageResource(0)
                    iconOnline.setBackgroundResource(0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun lastMessage(receiverId: String, lastMessage: TextView, lastTime: TextView) {
        sharedPreferences = context.getSharedPreferences("is_sign_in", AppCompatActivity.MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("userId", "").toString()
        singleChatsRef = FirebaseDatabase.getInstance().getReference("Single Chats")
        singleChatsRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val singleChat = dataSnapshot.getValue(SingleChat::class.java)!!
                    if (singleChat.senderId == currentUserId && singleChat.receiverId == receiverId ||
                        singleChat.senderId == receiverId && singleChat.receiverId == currentUserId) {
                        if (singleChat.senderId == currentUserId) {
                            val iSend = "Bạn: " + singleChat.message
                            lastMessage.text = iSend
                        } else {
                            lastMessage.text = singleChat.message
                        }
                        val timeData = dataSnapshot.child("timeStamp").child("time").value
                        val dateData = dataSnapshot.child("timeStamp").child("date").value
                        val timeFull = dateData.toString() + " " + timeData.toString()
                        val simpleDateFormat = SimpleDateFormat("E, dd/MM/yyyy hh:mm:ss a", Locale.US)
                        val timeParse = simpleDateFormat.parse(timeFull)!!
                        val time = timeParse.time
                        val timeFormat = SimpleDateFormat("hh:mm")
                        val timeSend = timeFormat.format(time)
                        lastTime.text = timeSend
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}