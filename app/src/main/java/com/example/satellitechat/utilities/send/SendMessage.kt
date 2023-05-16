package com.example.satellitechat.utilities.send

import com.example.satellitechat.utilities.constants.Constants
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.HashMap

class SendMessage {
    private var messagesRef: DatabaseReference = FirebaseDatabase.getInstance().getReference(Constants.MESSAGES_REF)
    private var messageId = ""
    // Send message: Gửi tin nhắn (lưu vào DB)
    fun sendMessage(senderId: String, receiverId: String, message: String, messageType: String, messageState: HashMap<String, Any>, mediaFileType: String, mediaFile: HashMap<String, String>) {
        // Save data normal
        val hashMap: HashMap<String, Any> = HashMap()
        val pushMsg = messagesRef.push()
        messageId = pushMsg.key.toString()
        hashMap["messageId"] = messageId
        hashMap["senderId"] = senderId
        hashMap["receiverId"] = receiverId
        hashMap["message"] = message
        hashMap["messageType"] = messageType
        hashMap["messageRes"] = ""
        hashMap["timeStamp"] = messageState
        hashMap["mediaFileType"] = mediaFileType
        hashMap["mediaFile"] = mediaFile
        pushMsg.setValue(hashMap)
    }

    fun getMessageId(): String {
        return messageId
    }
}