package com.example.satellitechat.activity.client.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.adapter.ImageAdapter
import com.example.satellitechat.adapter.MessageAdapter
import com.example.satellitechat.listeners.UsersListener
import com.example.satellitechat.model.Image
import com.example.satellitechat.model.Message
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.emoji.EmojiApp
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.example.satellitechat.utilities.time.CurrentTimeAndDate
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.vanniktech.emoji.EmojiPopup
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.fragment_chat.view.*
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity(), UsersListener {

    private var currentUserId: String = ""
    private var messageList = ArrayList<Message>()
    private var uriISRecycler = ArrayList<Image>()
    private var uriImageSelected = ArrayList<Uri>()
    private var receiverId: String = ""
    private var messageId: String = ""
    private var messageState: HashMap<String, Any> = getCurrentTimeAndDate()
    private lateinit var dialog: Dialog
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersRef: DatabaseReference
    private lateinit var singleChatsRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var imageAdapter: ImageAdapter

    @SuppressLint("SimpleDateFormat", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize firebase auth, firebase storage, firebase db ...
        preferenceManager = PreferenceManager(this@ChatActivity)
        currentUserId = preferenceManager.getCurrentId().toString()
        storage = FirebaseStorage.getInstance()
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        singleChatsRef = FirebaseDatabase.getInstance().getReference(Constants.MESSAGES_REF)

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

        // Get receiverId from intent
        receiverId = intent.getStringExtra("userIdReceiver").toString()
        if(receiverId != "null") {
            preferenceManager.setReceiverId(receiverId)
        } else {
            receiverId = intent.getStringExtra(Constants.RECEIVER_ID).toString()
            preferenceManager.setReceiverId(receiverId)
        }
        btnShowTools.setOnClickListener {
            inputContent.clearFocus()
        }
        // Coi lại đoạn gửi qua incoming activity mesageId

        // Set onclick button send message
        btnSendMsg.setOnClickListener {
            val drawable = btnSendMsg.drawable
            val otherDrawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.icon_facebook_like)
            if (drawable?.constantState?.equals(otherDrawable?.constantState) == true) {
                sendMessageSingleChat(
                    currentUserId, receiverId, "Đã gửi 1 biểu tượng cảm xúc",
                    "icon_like", messageState, "", HashMap()
                )
            } else {
                setSendMessage(currentUserId, receiverId)
            }
        }

        // Set style when focus input chat
        inputContent.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hasFocusInputMsg(1)
            } else {
                hasFocusInputMsg(0)
            }
        }

        // Set on click choose image send
        btnChooseImage.setOnClickListener {
            checkPermissions(Constants.IMAGE_PERMISSION)
        }

        // Set on click choose file send
        btnSendFiles.setOnClickListener {
            if(Environment.isExternalStorageManager()) {
                chooseFiles()
            } else {
                checkPermissions(Constants.FILE_PERMISSION)
            }
        }

        // Set on click audio call
        btnAudioCall.setOnClickListener {
            getReceiver(receiverId) { receiver ->
                initiateAudioMeeting(receiver!!)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                sendMessageSingleChat(
                    currentUserId, receiverId, "Video Call", "video_call",
                    messageState, "", HashMap()
                )
            }, 2000)
        }

        // Set on click video call
        btnVideoCall.setOnClickListener {
            getReceiver(receiverId) { receiver ->
                initiateVideoMeeting(receiver!!)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                sendMessageSingleChat(
                    currentUserId, receiverId, "Video Call", "video_call",
                    messageState, "", HashMap()
                )
            }, 2000)
        }

        val emojiPopup = EmojiPopup.Builder.fromRootView(rootChatActivity).build(inputContent)
        btnEmojiIcon.setOnClickListener {
            emojiPopup.toggle()
        }

    }

    override fun onStart() {
        super.onStart()
        initInfoActivity()
        setVisibilityBtnSend(0)
        // Render message old
        messageAdapter = MessageAdapter(this, ArrayList())
        contentChatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity, RecyclerView.VERTICAL, false)
            adapter = messageAdapter
            smoothScrollToPosition(messageList.size)
        }
        renderMessage(currentUserId, receiverId)
    }

    // Request permission camera or file
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.IMAGE_PERMISSION_REQUEST -> {
                // Agree to allow the use
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseImages()
                } else { // Do not agree to allow the use
                    Toast.makeText(this@ChatActivity, "Bạn đã từ chối quyền truy cập image", Toast.LENGTH_LONG).show()
                }
                return
            }
            Constants.FILE_PERMISSION_REQUEST -> {
                // Agree to allow the use
                if (Environment.isExternalStorageManager()) {
                    chooseFiles()
                } else if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    chooseFiles()
                } else { // Do not agree to allow the use
                    Toast.makeText(this@ChatActivity, "Bạn đã từ chối quyền truy cập file", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("Recycle", "Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.CHOOSE_IMAGE_REQUEST && resultCode != Constants.CHOOSE_IMAGE_RESPONSE) {
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

            // Set column for grid layout
            containerSelectedPhoto.visibility = View.VISIBLE
            setVisibilityBtnSend(1)

            if (uriISRecycler.size == 1) {
                listSelectedPhotoRecyclerView.visibility = View.GONE
                imageSelectedCard.visibility = View.VISIBLE
                val bitmap: Bitmap? =
                    MediaStore.Images.Media.getBitmap(contentResolver, data!!.data)
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
        } else {
            containerSelectedPhoto.visibility = View.GONE
        }

        if (requestCode == Constants.CHOOSE_FILE_REQUEST && resultCode != Constants.CHOOSE_FILE_RESPONSE) {
            if (data!!.data != null) {
                val uri = Uri.parse(data.data.toString())
                val cursor = baseContext.contentResolver.query(uri, null, null, null, null)
                cursor?.use { cur ->
                    if (cur.moveToFirst()) {
                        val nameIndex = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val fileName = cur.getString(nameIndex)
                        var uriFie: Uri? = data.data
                        storageReference = storage.reference.child(
                            "image/media_file/" + UUID.randomUUID().toString()
                        )
                        storageReference.putFile(uriFie!!)
                            .addOnSuccessListener { taskPush ->
                                taskPush.metadata!!.reference!!.downloadUrl
                                    .addOnSuccessListener { taskDownload ->
                                        val fileMedia: HashMap<String, String> = HashMap()
                                        fileMedia["1"] = taskDownload.toString()
                                        val messageState: HashMap<String, Any> = getCurrentTimeAndDate()
                                        sendMessageSingleChat(
                                            currentUserId, receiverId, fileName, "file",
                                           messageState, "*/*", fileMedia
                                        )
                                        Toast.makeText(this@ChatActivity, "Gửi file thành công!", Toast.LENGTH_LONG).show()
                                    }
                                    .addOnFailureListener { taskDownload ->
                                        Toast.makeText(this@ChatActivity, taskDownload.message, Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener {
                                dialog.dismiss()
                                Toast.makeText(this@ChatActivity, "Gửi file thất bại!", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            } else {
                Toast.makeText(this@ChatActivity, "Gửi file thất bại!", Toast.LENGTH_LONG).show()
            }
        }

        if (requestCode == Constants.FILE_PERMISSION_REQUEST && resultCode == Activity.RESULT_OK) {
            chooseFiles()
        }

    }

    private fun containsEmoji(text: String): Boolean {
        val regex = Regex("[\\p{So}]+")
        return regex.matches(text)
    }

    // Send images selected: Gửi đi ảnh đã chọn
    private fun sendImagesSelected() {
        var count = 0
        val mediaFile: HashMap<String, String> = HashMap()
        for (i in 0 until uriImageSelected.size) {
            storageReference = storage.reference.child("image/media_file/" + UUID.randomUUID().toString())
            storageReference.putFile(uriImageSelected[i])
                .addOnSuccessListener { taskPush ->
                    taskPush.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { taskDownload ->
                            count += 1;
                            mediaFile[count.toString()] = taskDownload.toString()
                            if (count == uriImageSelected.size) {
                                dialog.dismiss()
                                containerSelectedPhoto.visibility = View.GONE
                                setVisibilityBtnSend(0)
                                val message = "Gửi $count ảnh"
                                val messageState: HashMap<String, Any> = getCurrentTimeAndDate()
                                sendMessageSingleChat(
                                    currentUserId, receiverId, message, "file",
                                    messageState, "image", mediaFile
                                )
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

    // Check permissions camera: Kiểm tra quyền truy cập camera
    private fun checkPermissions(permission: String) {
        if (permission == Constants.IMAGE_PERMISSION) {
            if (ContextCompat.checkSelfPermission(this@ChatActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(
                    this@ChatActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    Constants.IMAGE_PERMISSION_REQUEST
                )
            } else {
                chooseImages()
            }
        } else if (permission == Constants.FILE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                startActivityForResult(intent, Constants.FILE_PERMISSION_REQUEST)
            } else if (ContextCompat.checkSelfPermission(this@ChatActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@ChatActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.FILE_PERMISSION_REQUEST
                )
            } else {
                chooseFiles()
            }

        }
    }

    // Choose Image: Chọn ảnh từ thư viện
    private fun chooseImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh gửi đi"), Constants.CHOOSE_IMAGE_REQUEST)
    }

    // Choose files:  Chọn file từ thư viện
    private fun chooseFiles() {
        val intent = Intent()
        intent.type = "application/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Chọn file gửi đi"), Constants.CHOOSE_FILE_REQUEST)
    }

    // Set before send message: Thiết lập trạng thái trước khi gửi tin nhắn
    @SuppressLint("SimpleDateFormat")
    private fun setSendMessage(currentUserId: String, receiverId: String) {
        val message = inputContent.text.toString()
        val messageType = if(containsEmoji(message)) "emoji_icon"  else  "msg"
        if (message == "" && containerSelectedPhoto.visibility == View.GONE) {
            Toast.makeText(this@ChatActivity, "Vui lòng nhập tin nhắn!", Toast.LENGTH_LONG).show()
        } else if (message != "" && containerSelectedPhoto.visibility == View.GONE) {
            // Set time for save data: Thiết lập thời gian trước khi gửi tin nhắn
            val messageState: HashMap<String, Any> = getCurrentTimeAndDate()
            val isSend = sendMessageSingleChat(
                currentUserId, receiverId, message, messageType,
                messageState, "", HashMap()
            )
            if (equals(isSend)) {
                Toast.makeText(this@ChatActivity, "Gửi tin nhắn thất bại!", Toast.LENGTH_LONG).show()
            } else {
                inputContent.setText("")
                inputContent.clearFocus()
            }
        } else if (message == "" && containerSelectedPhoto.visibility == View.VISIBLE) {
            dialog.show()
            sendImagesSelected()
            setVisibilityBtnSend(1)
        }
    }

    // Send message: Gửi tin nhắn (lưu vào DB)
    private fun sendMessageSingleChat(
        senderId: String,
        receiverId: String,
        message: String,
        messageType: String,
        messageState: HashMap<String, Any>,
        mediaFileType: String,
        mediaFile: HashMap<String, String> )
    {
        // Save data normal
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["senderId"] = senderId
        hashMap["receiverId"] = receiverId
        hashMap["message"] = message
        hashMap["messageType"] = messageType
        hashMap["messageRes"] = ""
        hashMap["timeStamp"] = messageState
        hashMap["mediaFileType"] = mediaFileType
        hashMap["mediaFile"] = mediaFile
        val pushMsg = singleChatsRef.push()
        hashMap["messageId"] = pushMsg.key.toString()
        messageId = pushMsg.key.toString()
        pushMsg.setValue(hashMap)
    }

    // Render sent messages: Hiển thị tin nhắn đã gửi ra giao diện người dùng
    private fun renderMessage(senderId: String, receiverId: String) {
        singleChatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val message = dataSnapshot.getValue(Message::class.java)!!
                    if (message.senderId == senderId && message.receiverId == receiverId ||
                        message.senderId == receiverId && message.receiverId == senderId)
                    {
                        messageList.add(message)
                    }
                }

                if (this@ChatActivity.isDestroyed) {
                    return
                } else {
                    messageAdapter.updateMessageList(messageList)
                    contentChatRecyclerView.smoothScrollToPosition(messageList.size)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    // Get info receiver from database: Lấy thông tin người nhận từ cơ sở dữ liệu
    private fun initInfoActivity() {
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
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.US)
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
                        val date = (timeOff / 60) / 24
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

    // Set visibility btn
    private fun setVisibilityBtnSend(const: Int) {
        if (const == 1) {
            btnSendMsg.setImageResource(R.drawable.ic_baseline_send)
        } else {
            btnSendMsg.setImageResource(R.drawable.icon_facebook_like)
        }
    }

    // Check focus input message
    private fun hasFocusInputMsg(isHas: Int) {
        if (isHas == 1) {
            val paramConInputContent =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 12.0f)
            val paramBtnSendMsg =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f)
            val paramInputContent =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 6.4f)
            val paramShowTools =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f)
            paramInputContent.setMargins(8, 0, 10, 0)
            containerToolsBarFooter.visibility = View.GONE
            containerInputContent.layoutParams = paramConInputContent
            setVisibilityBtnSend(1)
            btnSendMsg.layoutParams = paramBtnSendMsg
            btnShowTools.visibility = View.VISIBLE
            btnShowTools.layoutParams = paramShowTools
            boxInputContent.layoutParams = paramInputContent
            contentChatRecyclerView.smoothScrollToPosition(messageList.size)
        } else {
            val paramConInputContent =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 6.0f)
            val paramInputContent =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 6.0f)
            val paramBtnSendMsg =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
            containerToolsBarFooter.visibility = View.VISIBLE
            containerInputContent.layoutParams = paramConInputContent
            setVisibilityBtnSend(0)
            btnSendMsg.layoutParams = paramBtnSendMsg
            btnShowTools.visibility = View.GONE
            boxInputContent.layoutParams = paramInputContent
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCurrentTimeAndDate(): HashMap<String, Any> {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val timeFormat = SimpleDateFormat("hh:mm:ss a")
        val currentDate: String = dateFormat.format(System.currentTimeMillis())
        val currentTime: String = timeFormat.format(System.currentTimeMillis())
        val messageState: HashMap<String, Any> = HashMap()
        messageState["date"] = currentDate
        messageState["time"] = currentTime
        return messageState
    }

    // Get user from firebase
    private fun getReceiver(receiverId: String, onResult: (User?) -> Unit) {
        usersRef.child(receiverId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val receiver = snapshot.getValue(User::class.java)
                onResult(receiver)
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(null)
            }
        })
    }

    override fun initiateAudioMeeting(user: User) {
        if (user.fcmToken.trim().isEmpty()) {
            Toast.makeText(this@ChatActivity, "${user.userName} is not available for audio call", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(this@ChatActivity, OutGoingCallActivity::class.java)
            intent.putExtra("receiver", user)
            intent.putExtra("type", Constants.AUDIO_CALL)
            intent.putExtra("messageId", messageId)
            startActivity(intent)
            finish()
        }
    }

    override fun initiateVideoMeeting(user: User) {
        if (user.fcmToken.trim().isEmpty()) {
            Toast.makeText(this@ChatActivity, "${user.userName} is not available for video call", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(this@ChatActivity, OutGoingCallActivity::class.java)
            intent.putExtra("receiver", user)
            intent.putExtra("type", Constants.VIDEO_CALL)
            intent.putExtra("messageId", messageId)
            startActivity(intent)
            finish()
        }
    }

}

