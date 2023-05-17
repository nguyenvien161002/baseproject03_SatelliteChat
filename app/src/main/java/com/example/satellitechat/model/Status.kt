package com.example.satellitechat.model

data class Status (
    var statusId: String = "",
    var statusContent: String = "",
    var statusExpiry: String = "",
    var postPermission: String = "",
    var posterId: String = "",
    var timeStamp: Any = "",
)