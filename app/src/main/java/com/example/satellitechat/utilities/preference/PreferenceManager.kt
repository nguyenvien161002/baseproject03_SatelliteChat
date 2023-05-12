package com.example.satellitechat.utilities.preference

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.satellitechat.utilities.constants.Constants

class PreferenceManager (context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun setSignIn(userId: String) {
        editor.putString(Constants.IS_SIGN_IN, "true")
        editor.putString(Constants.METHOD_SIGN_IN, "account")
        editor.putString(Constants.USER_ID, userId)
        editor.apply()
    }

    fun setNameAndImage(userName: String, userImage: String) {
        editor.putString(Constants.USER_NAME, userName)
        editor.putString(Constants.USER_IMAGE, userImage)
        editor.apply()
    }

    fun getIsSignIn(): String? {
        return sharedPreferences.getString(Constants.IS_SIGN_IN, "")
    }

    fun setRememberMe(email: String, password: String) {
        editor.putString(Constants.USER_EMAIL, email)
        editor.putString(Constants.USER_PASSWORD, password)
        editor.apply()
    }

    fun getRememberMe(): ArrayList<String> {
        val arrayList: ArrayList<String> = ArrayList()
        arrayList.add(sharedPreferences.getString(Constants.USER_EMAIL, "")!!)
        arrayList.add(sharedPreferences.getString(Constants.USER_PASSWORD, "")!!)
        return arrayList
    }

    fun getCurrentId(): String? {
        return sharedPreferences.getString(Constants.USER_ID, "")
    }

    fun getReceiverId(): String? {
        return sharedPreferences.getString(Constants.RECEIVER_ID, "")
    }

    fun setReceiverId(receiverId: String) {
        editor.putString(Constants.RECEIVER_ID, receiverId)
        editor.apply()
    }

    fun logOut() {
        editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    fun setString(key: String, string: String) {
        editor.putString(key, string)
        editor.apply()
    }

    fun getString(key: String): String? {
        return sharedPreferences.getString(key, "")
    }

    fun getAllPreference() {
        Log.d("ALL_PREFERENCE", sharedPreferences.all.toString())
    }
}