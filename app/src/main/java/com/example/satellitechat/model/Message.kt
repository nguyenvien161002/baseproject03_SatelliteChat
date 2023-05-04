package com.example.satellitechat.model

data class Message (
    var messageId: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var messageType: String = "",
    var messageRes: String = "",
    var messageState: Any = "",
    var mediaFileType: String = "",
    var mediaFile: ArrayList<String> = ArrayList(),
)