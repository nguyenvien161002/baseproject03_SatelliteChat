package com.example.satellitechat.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.Image
import com.example.satellitechat.model.SingleChat
import com.facebook.FacebookSdk.getApplicationContext
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.*

class SingleChatAdapter (private val context: Context, private val singleChatList: ArrayList<SingleChat>) :
    RecyclerView.Adapter<SingleChatAdapter.ViewHolder>() {

    private var currentUserId = ""
    private var receiverId = ""
    private var itemBoxSender = 1
    private var itemBoxReceiver = 0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usersRef: DatabaseReference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reference layout
        return if (viewType == itemBoxSender) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_msg_sender, parent, false)
            ViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_box_msg_receiver, parent, false)
            ViewHolder(view)
        }

    }

    @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = singleChatList[position]

        // Render image sent by sender/receiver
        if (message.mediaFile.size != 0) {
            val imageList: ArrayList<Image> = ArrayList()
            for (i in 1 until message.mediaFile.size) {
                imageList.add(Image(Uri.EMPTY, message.mediaFile[i]))
            }
            when (imageList.size) {
                1 -> {
                    holder.imageListRecyclerView.visibility = View.GONE
                    holder.oneImageSendCard.visibility = View.VISIBLE
                    Glide.with(context).load(message.mediaFile[1]).into(holder.oneImageSend)
                }
                2 -> {
                    setSpanCountGridLayout(holder.oneImageSend, holder.imageListRecyclerView, imageList, 2)
                }
                else -> {
                    setSpanCountGridLayout(holder.oneImageSend, holder.imageListRecyclerView, imageList, 3)
                }
            }
            holder.textMsgUser.visibility = View.GONE
        } else {
            holder.textMsgUser.text = message.message
        }

        // Render icon sent by sender/receiver
        if (message.messageIcon.size != 0) {
            if (message.messageIcon["id"] == "1") {
                val params = LinearLayout.LayoutParams(60, 60)
                holder.oneImageSendCard.layoutParams = params
                holder.oneImageSendCard.cardElevation = 0f
                holder.imageListRecyclerView.visibility = View.GONE
                holder.oneImageSendCard.visibility = View.VISIBLE
                holder.textMsgUser.visibility = View.GONE
                Glide.with(context).load(R.drawable.icon_facebook_like).into(holder.oneImageSend)
            }
        }

        // Get url image profile of sender
        if (message.senderId == currentUserId) {
            usersRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
            usersRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userImage = snapshot.child("userImage").value
                    if(userImage == "") {
                        holder.imageUser.strokeWidth = 1F
                        holder.imageUser.setStrokeColorResource(R.color.black_200)
                        Glide.with(getApplicationContext()).load(userImage).placeholder(R.drawable.profile_image).into(holder.imageUser)
                    } else {
                        Glide.with(getApplicationContext()).load(userImage).into(holder.imageUser)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                }
            })
        }
        else { // Get url image profile of receiver
            usersRef = FirebaseDatabase.getInstance().getReference("Users").child(receiverId)
            usersRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userImage = snapshot.child("userImage").value
                    if(userImage == "") {
                        holder.imageUser.strokeWidth = 1F
                        holder.imageUser.setStrokeColorResource(R.color.black_200)
                        Glide.with(getApplicationContext()).load(userImage).placeholder(R.drawable.profile_image).into(holder.imageUser)
                    } else {
                        Glide.with(getApplicationContext()).load(userImage).into(holder.imageUser)

                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun getItemCount(): Int {
        return singleChatList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val imageUser: ShapeableImageView = view.findViewById(R.id.imageUser)
        val textMsgUser: TextView = view.findViewById(R.id.textMsgUser)
        val imageListRecyclerView: RecyclerView = view.findViewById(R.id.imageListRecyclerView)
        val oneImageSend: ImageView = view.findViewById(R.id.oneImageSend)
        val oneImageSendCard: CardView = view.findViewById(R.id.oneImageSendCard)
    }

    override fun getItemViewType(position: Int): Int {
        sharedPreferences = context.getSharedPreferences("is_sign_in", AppCompatActivity.MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("userId", "").toString()
        receiverId = sharedPreferences.getString("receiverId", "").toString()
        return if (singleChatList[position].senderId == currentUserId) {
            itemBoxSender
        } else {
            itemBoxReceiver
        }
    }
    
    private fun setSpanCountGridLayout (
        imageView: ImageView,
        recyclerView: RecyclerView,
        imageList: ArrayList<Image>,
        spanCount: Int)
    {
        recyclerView.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        recyclerView.adapter = ImageAdapter(context, imageList)
    }
}


