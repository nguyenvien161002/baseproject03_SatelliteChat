package com.example.satellitechat.services.remote

import com.example.satellitechat.utilities.constants.Constants
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class RetrofitService {

    private var retrofit: Retrofit? = null

    fun getClient (): Retrofit? {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(Constants.GG_API_FCM_BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
        }
        return retrofit
    }

}