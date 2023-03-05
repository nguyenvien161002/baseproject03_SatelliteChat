package com.example.satellitechat.activity.client.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.adapter.ImageAdapter
import com.example.satellitechat.adapter.SingleChatAdapter
import com.example.satellitechat.model.Image
import com.example.satellitechat.model.SingleChat
import com.example.satellitechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.fragment_chat.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ChatActivity : AppCompatActivity() {

    private var currentUserId: String = ""
    private var CHOOSE_IMAGE_SEND_REQUEST: Int = 1000
    private var CHOOSE_IMAGE_SEND_RESPONSE: Int = 1001
    private var IMAGE_PERMISSION: Int = 3001
    private var SEND_IMAGE_CHOOSE: Int = 0
    private var singleChatList = ArrayList<SingleChat>()
    private var uriISRecycler = ArrayList<Image>()
    private var uriImageSelected = ArrayList<Uri>()
    private var receiverId: String = ""
    private lateinit var dialog: Dialog
    private lateinit var singleChatAdapter: SingleChatAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference
    private lateinit var singleChatsRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var imageAdapter: ImageAdapter

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = Firebase.auth
        sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("userId", "").toString()
        storage = FirebaseStorage.getInstance()
        usersRef = FirebaseDatabase.getInstance().getReference("Users")
        singleChatsRef = FirebaseDatabase.getInstance().getReference("Single Chats")

        iconBackChatActivity.setOnClickListener {
            onBackPressed()
        }

        // Setup dialog "loading process"
        dialog = Dialog(this@ChatActivity)
        dialog.setContentView(R.layout.dialog_loading)
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.setCancelable(false)

        receiverId = intent.getStringExtra("userIdReceiver").toString()
        editor = sharedPreferences.edit()
        editor.putString("receiverId", receiverId)
        editor.apply()

        btnShowTools.setOnClickListener {
            inputContent.clearFocus()
        }

        // Set onclick button send message
        btnSendMsg.setOnClickListener {
            setSendMessage(currentUserId, receiverId)
        }

        // Set onclick button send icon "like"
        btnIconLike.setOnClickListener {
            val currentTimeAndDate = getCurrentTimeAndDate()
            val hashMapIcon: HashMap<String, String> = HashMap()
            hashMapIcon["id"] = "1"
            hashMapIcon["name"] = "icon_like"
            sendMessageSingleChat(currentUserId, receiverId, "Đã gửi 1 biểu tượng cảm xúc", hashMapIcon, currentTimeAndDate, HashMap())
        }

        // Render message old
        linearLayoutManager = LinearLayoutManager(this@ChatActivity, RecyclerView.VERTICAL, false)
        contentChatRecyclerView.layoutManager = linearLayoutManager
        renderMessage(currentUserId, receiverId)

        // Set style when focus input chat
        inputContent.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val paramConInputContent = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 12.0f)
                val paramBtnSendMsg = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f)
                val paramInputContent = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 6.4f)
                val paramShowTools = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f)
                paramInputContent.setMargins(8, 0, 10, 0)
                containerToolsBarFooter.visibility = View.GONE
                containerInputContent.layoutParams = paramConInputContent
                btnIconLike.visibility = View.GONE
                btnSendMsg.visibility = View.VISIBLE
                btnSendMsg.layoutParams = paramBtnSendMsg
                btnShowTools.visibility = View.VISIBLE
                btnShowTools.layoutParams = paramShowTools
                inputContent.layoutParams = paramInputContent
                contentChatRecyclerView.smoothScrollToPosition(singleChatList.size)
            } else {
                val paramConInputContent = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 6.0f)
                val paramInputContent = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 6.0f)
                containerToolsBarFooter.visibility = View.VISIBLE
                containerInputContent.layoutParams = paramConInputContent
                btnIconLike.visibility = View.VISIBLE
                btnSendMsg.visibility = View.GONE
                btnShowTools.visibility = View.GONE
                inputContent.layoutParams = paramInputContent
            }
        }

        // Set on click choose image send
        btnChooseImage.setOnClickListener {
            checkPermissions()
        }

    }

    override fun onStart() {
        super.onStart()
        getInfoReceiver()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == IMAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chooseImages()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHOOSE_IMAGE_SEND_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data!!.clipData != null) {
                val numberImage = data.clipData!!.itemCount
                for (i in 0 until numberImage) {
                    uriISRecycler.add(Image(data.clipData!!.getItemAt(i).uri))
                    uriImageSelected.add(data.clipData!!.getItemAt(i).uri)
                }
            } else {
                val imageUrl = data.data
                uriISRecycler.add(Image(imageUrl))
                uriImageSelected.add(imageUrl!!)
            }
        }

        // Set column for grid layout
        containerSelectedPhoto.visibility = View.VISIBLE
        btnSendMsg.visibility = View.VISIBLE
        btnIconLike.visibility = View.GONE
        if (uriISRecycler.size == 1) {
            listSelectedPhotoRecyclerView.visibility = View.GONE
            imageSelectedCard.visibility = View.VISIBLE
            val bitmap: Bitmap?  = MediaStore.Images.Media.getBitmap(contentResolver, data!!.data)
            imageSelectedChat.setImageBitmap(bitmap)
        } else if (uriISRecycler.size == 2) {
            listSelectedPhotoRecyclerView.visibility = View.VISIBLE
            imageSelectedCard.visibility = View.GONE
            renderImagesSelected(2)
        } else {
            listSelectedPhotoRecyclerView.visibility = View.VISIBLE
            imageSelectedCard.visibility = View.GONE
            renderImagesSelected(3)
        }

    }

    // Send images selected
    private fun sendImagesSelected() {
        var count = 0
        val imageHashMap: HashMap<String, String> = HashMap()
        for (i in 0 until uriImageSelected.size) {
            storageReference = storage.reference.child("image/media_file/" + UUID.randomUUID().toString())
            storageReference.putFile(uriImageSelected[i])
                .addOnSuccessListener { taskPush ->
                    taskPush.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { taskDownload ->
                            count += 1;
                            imageHashMap[count.toString()] = taskDownload.toString()
                            if (count == uriImageSelected.size) {
                                dialog.dismiss()
                                containerSelectedPhoto.visibility = View.GONE
                                btnIconLike.visibility = View.VISIBLE
                                btnSendMsg.visibility = View.GONE
                                val message = "Gửi $count ảnh"
                                val messageState: HashMap<String, Any> = getCurrentTimeAndDate()
                                sendMessageSingleChat(currentUserId, receiverId, message, HashMap(), messageState, imageHashMap)
                                // Sent success then remove image old
                                uriISRecycler.clear()
                                uriImageSelected.clear()
                                // Display notify when sent success
                                Toast.makeText(this@ChatActivity, "Gửi ảnh thành công!", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { taskDownload ->
                            Toast.makeText(this@ChatActivity, taskDownload.message, Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    dialog.dismiss()
                    Toast.makeText(this@ChatActivity, "Gửi ảnh thất bại!", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Render images selected
    @SuppressLint("NotifyDataSetChanged")
    private fun renderImagesSelected(spanCount: Int) {
        listSelectedPhotoRecyclerView.visibility = View.VISIBLE
        imageSelectedCard.visibility = View.GONE
        listSelectedPhotoRecyclerView.layoutManager = GridLayoutManager(this@ChatActivity, spanCount)
        imageAdapter = ImageAdapter(this@ChatActivity, uriISRecycler)
        listSelectedPhotoRecyclerView.adapter = imageAdapter
        imageAdapter.notifyDataSetChanged()
    }

    // Check permissions camera
    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this@ChatActivity, android.Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(
                this@ChatActivity, arrayOf(android.Manifest.permission.CAMERA),
                IMAGE_PERMISSION
            )
        } else {
            chooseImages()
        }
    }

    // Choose Image
    private fun chooseImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh gửi đi"), CHOOSE_IMAGE_SEND_REQUEST)
    }

    // Set before send message
    @SuppressLint("SimpleDateFormat")
    private fun setSendMessage(currentUserId: String, receiverId: String) {
        val message = inputContent.text.toString()
        if (message == "" && containerSelectedPhoto.visibility == View.GONE) {
            Toast.makeText(this@ChatActivity, "Vui lòng nhập tin nhắn!", Toast.LENGTH_LONG).show()
        }
        else if (message != "" && containerSelectedPhoto.visibility == View.GONE) {
            // Set time for save data
            val messageState: HashMap<String, Any> = getCurrentTimeAndDate()
            val isSend = sendMessageSingleChat(currentUserId, receiverId, message, HashMap(), messageState, HashMap())
            if (equals(isSend)) {
                Toast.makeText(this@ChatActivity, "Gửi tin nhắn thất bại!", Toast.LENGTH_LONG).show()
            } else {
                inputContent.setText("")
                inputContent.clearFocus()
                contentChatRecyclerView.smoothScrollToPosition(singleChatList.size)
            }
        } else if (message == "" && containerSelectedPhoto.visibility == View.VISIBLE) {
            dialog.show()
            sendImagesSelected()
        }
    }

    // Send message
    private fun sendMessageSingleChat (
        senderId: String,
        receiverId: String,
        message: String,
        messageIcon: HashMap<String, String>,
        messageState: HashMap<String, Any>,
        mediaFile: HashMap<String, String>)
    {
        // Save data normal
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["senderId"] = senderId
        hashMap["receiverId"] = receiverId
        hashMap["message"] = message
        hashMap["timeStamp"] = messageState
        hashMap["mediaFile"] = mediaFile
        hashMap["messageIcon"] = messageIcon
        singleChatsRef.push().setValue(hashMap)
    }

    // Get current time/date
    @SuppressLint("SimpleDateFormat")
    private fun getCurrentTimeAndDate(): HashMap<String, Any> {
        val dateFormat = SimpleDateFormat("E, dd/MM/yyyy")
        val timeFormat = SimpleDateFormat("hh:mm:ss a")
        val currentDate: String = dateFormat.format(System.currentTimeMillis())
        val currentTime: String = timeFormat.format(System.currentTimeMillis())
        val messageState: HashMap<String, Any> = HashMap()
        messageState["date"] = currentDate
        messageState["time"] = currentTime
        return messageState
    }

    // Render sent messages
    private fun renderMessage(senderId: String, receiverId: String) {
        singleChatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                singleChatList.clear()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val singleChat = dataSnapshot.getValue(SingleChat::class.java)!!
                    if (singleChat.senderId == senderId && singleChat.receiverId == receiverId ||
                        singleChat.senderId == receiverId && singleChat.receiverId == senderId) {
                        singleChatList.add(singleChat)
                    }
                }

                if (this@ChatActivity.isDestroyed) {
                    return
                } else {
                    singleChatAdapter = SingleChatAdapter(this@ChatActivity, singleChatList)
                    contentChatRecyclerView.adapter = singleChatAdapter
                    contentChatRecyclerView.smoothScrollToPosition(singleChatList.size)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, error.message, Toast.LENGTH_LONG).show()
                println(error.message)
            }
        })
    }

    // Get info receiver from database
    private fun getInfoReceiver() {
        usersRef.child(receiverId).addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val receiver = snapshot.getValue(User::class.java)
                userNameReceiver.text = receiver!!.userName
                if (receiver.userImage == "") {
                    imageReceiver.setImageResource(R.drawable.profile_image)
                    imageReceiver.strokeWidth = 1F
                    imageReceiver.setStrokeColorResource(R.color.black_200)
                } else {
                    if (this@ChatActivity.isDestroyed) {
                        return
                    } else {
                        Glide.with(this@ChatActivity).load(receiver.userImage).into(imageReceiver)
                    }
                }

                // Get data from firebase
                val typeReceiver = snapshot.child("userState").child("type").value
                val timeReceiver = snapshot.child("userState").child("time").value
                val dateReceiver = snapshot.child("userState").child("date").value
                // Check status user online/offline
                if (typeReceiver != "online") {
                    // Logic calculate the number of hours exited
                    // Date exited
                    val timeFullDb = dateReceiver.toString() + " " + timeReceiver.toString()
                    val dateFormat = SimpleDateFormat("E, dd/MM/yyyy hh:mm:ss a", Locale.US)
                    val dateExited = dateFormat.parse(timeFullDb)!!
                    val dateExitedToTime = dateExited.time / 60000
                    // Current date
                    val currentDateFormat = dateFormat.parse(dateFormat.format(System.currentTimeMillis()))!!
                    val currentDateToTime = currentDateFormat.time / 60000
                    val timeOff = currentDateToTime - dateExitedToTime
                    val second = (currentDateFormat.time - dateExited.time) / 1000
                    if (second in 0..60) {
                        stateReceiver.text = "Hoạt động $second giây trước"
                    } else if (timeOff in 1..59) {
                        stateReceiver.text = "Hoạt động $timeOff phút trước"
                    } else if (timeOff in 60..1440) {
                        val hours = timeOff / 60
                        stateReceiver.text = "Hoạt động $hours giờ trước"
                    } else if (timeOff >= 1441) {
                        val date = (timeOff/60)/24
                        stateReceiver.text = "Hoạt động $date ngày trước"
                    }
                    iconStateReceiver.visibility = View.GONE
                } else {
                    iconStateReceiver.visibility = View.VISIBLE
                    stateReceiver.text = typeReceiver as CharSequence?
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}

