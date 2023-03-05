package com.example.satellitechat.model

data class SingleChat (
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var messageState: Any = "",
    var mediaFile: ArrayList<String> = ArrayList(),
    var messageIcon: HashMap<String, String> = HashMap()
)