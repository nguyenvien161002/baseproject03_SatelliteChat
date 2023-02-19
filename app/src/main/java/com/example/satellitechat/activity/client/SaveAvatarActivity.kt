package com.example.satellitechat.activity.client

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.example.satellitechat.R
import kotlinx.android.synthetic.main.activity_save_avatar.*

class SaveAvatarActivity : AppCompatActivity() {

    private var IMAGE_RESPONSE: Int = 2003

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_avatar)

        val byteArray = intent.getByteArrayExtra("BitmapAvatar")
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
        imageBeforeSave.setImageBitmap(bitmap)

        btnSaveAvatar.setOnClickListener {
            val intent = Intent()
            setResult(IMAGE_RESPONSE, intent)
            finish()
        }

        btnBackSaveAvatar.setOnClickListener {
            onBackPressed()
        }

    }

    override fun onBackPressed() {
        val intent = Intent(this@SaveAvatarActivity, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }

}