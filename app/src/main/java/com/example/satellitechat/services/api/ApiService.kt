package com.example.satellitechat.services.api

import retrofit2.Call
import retrofit2.http.*


interface ApiService {

    @POST("send")
    fun sendRemoteMessage (
        @HeaderMap headerMap : HashMap<String, String>,
        @Body remoteMessage: String?
    ): Call<String>

}