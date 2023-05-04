package com.example.satellitechat.adapter

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.adapter.diffUtil.MessageDiffCallback
import com.example.satellitechat.model.Image
import com.example.satellitechat.model.Message
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.facebook.FacebookSdk.getApplicationContext
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MessageAdapter (private var context: Context, private var messageList: ArrayList<Message>)
    : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    private var currentUserId = ""
    private var receiverId = ""
    private var itemBoxSender = 1
    private var itemBoxReceiver = 0
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var usersRef: DatabaseReference
    private lateinit var singleChatRef: DatabaseReference

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
        val message = messageList[position]

        // Render image sent by sender/receiver
        if (message.mediaFile.size != 0 && message.mediaFileType != "*/*") {
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
            holder.textMsgEmojiIcon.visibility = View.GONE
            holder.msgCallView.visibility = View.GONE
        } else if (message.mediaFile.size != 0 && message.mediaFileType == "*/*") { // Render file sent by sender/receiver
            holder.textMsgUser.visibility = View.VISIBLE
            holder.textMsgUser.text = message.message
            holder.textMsgUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_file_download, 0);
            // If you click on the file, it will download
            holder.textMsgUser.setOnClickListener {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = Uri.parse(message.mediaFile[1])
                val request = DownloadManager.Request(uri)
                val fileName = message.message
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                    .setTitle(fileName)
                    .setDescription("Downloading $fileName")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                downloadManager.enqueue(request)
                // Set the click listener for the message view to open the download manager
                val downloadManagerIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                context.startActivity(downloadManagerIntent)
            }
            holder.textMsgEmojiIcon.visibility = View.GONE
            holder.imageListRecyclerView.visibility = View.GONE
            holder.oneImageSendCard.visibility = View.GONE
            holder.msgCallView.visibility = View.GONE
        } else if (message.messageType == "audio_call" || message.messageType == "video_call") { // Render chat item when click button
            singleChatRef = FirebaseDatabase.getInstance().getReference("Single Chats").child(message.messageId)
            singleChatRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentTime = snapshot.child("timeStamp").child("time").value
                    val format = SimpleDateFormat("hh:mm:ss a", Locale.US)
                    val date = format.parse(currentTime as String)
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
                    val timeCall = date?.let { timeFormat.format(it) }
                    holder.timeMsgCall.text = timeCall
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                }

            })
            holder.msgCallView.visibility = View.VISIBLE                                         // call video or audio by sender/receiver
            if (message.messageType == "audio_call") {
                holder.typeMsgCall.text = context.resources.getString(R.string.audio_call)
            } else {
                holder.typeMsgCall.text = context.resources.getString(R.string.video_call)
            }
            holder.textMsgUser.visibility = View.GONE
            holder.textMsgEmojiIcon.visibility = View.GONE
            holder.imageListRecyclerView.visibility = View.GONE
            holder.oneImageSendCard.visibility = View.GONE
        } else if (message.messageType == "icon_like") {
            // Set visibility
            holder.textMsgUser.visibility = View.GONE
            holder.textMsgEmojiIcon.visibility = View.GONE
            holder.imageListRecyclerView.visibility = View.GONE
            holder.msgCallView.visibility = View.GONE
            holder.oneImageSendCard.visibility = View.VISIBLE
            // Set params
            val params = LinearLayout.LayoutParams(86, 86)
            holder.oneImageSendCard.layoutParams = params
            holder.oneImageSendCard.cardElevation = 0f
            Glide.with(context).load(R.drawable.icon_facebook_like).into(holder.oneImageSend)
        } else if (message.messageType == "emoji_icon") {
            // Set visibility
            holder.textMsgUser.visibility = View.GONE
            holder.imageListRecyclerView.visibility = View.GONE
            holder.msgCallView.visibility = View.GONE
            holder.oneImageSendCard.visibility = View.GONE
            holder.textMsgEmojiIcon.visibility = View.VISIBLE
            holder.textMsgEmojiIcon.text = message.message
        } else {
            holder.textMsgEmojiIcon.visibility = View.GONE
            holder.imageListRecyclerView.visibility = View.GONE
            holder.oneImageSendCard.visibility = View.GONE
            holder.msgCallView.visibility = View.GONE
            holder.textMsgUser.visibility = View.VISIBLE
            holder.textMsgUser.text = message.message
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
        return messageList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val imageUser: ShapeableImageView = view.findViewById(R.id.imageUser)
        val textMsgUser: TextView = view.findViewById(R.id.textMsgUser)
        val textMsgEmojiIcon: TextView = view.findViewById(R.id.textMsgEmojiIcon)
        val imageListRecyclerView: RecyclerView = view.findViewById(R.id.imageListRecyclerView)
        val oneImageSend: ImageView = view.findViewById(R.id.oneImageSend)
        val oneImageSendCard: CardView = view.findViewById(R.id.oneImageSendCard)
        val msgCallView: LinearLayout = view.findViewById(R.id.msgCallView)
        val typeMsgCall: TextView = view.findViewById(R.id.typeMsgCall)
        val timeMsgCall: TextView = view.findViewById(R.id.timeMsgCall)
    }

    override fun getItemViewType(position: Int): Int {
        preferenceManager = PreferenceManager(context)
        currentUserId = preferenceManager.getCurrentId().toString()
        receiverId = preferenceManager.getReceiverId().toString()

        return if (messageList[position].senderId == currentUserId) {
            itemBoxSender
        } else {
            itemBoxReceiver
        }
    }

    // Trong class MessageAdapter
    fun updateMessageList(newList: ArrayList<Message>) {
        val diffCallback = MessageDiffCallback(messageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messageList.clear()
        messageList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }


    private fun setSpanCountGridLayout (imageView: ImageView, recyclerView: RecyclerView, imageList: ArrayList<Image>, spanCount: Int) {
        recyclerView.visibility = View.VISIBLE
        imageView.visibility = View.GONE
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        recyclerView.adapter = ImageAdapter(context, imageList)
    }
}


