package com.example.satellitechat.activity.client.saveAvatar

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.profile.ProfileActivity
import com.example.satellitechat.utilities.constants.Constants
import kotlinx.android.synthetic.main.activity_save_avatar.*

class SaveAvatarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_avatar)

        val byteArray = intent.getByteArrayExtra("BitmapAvatar")
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
        imageBeforeSave.setImageBitmap(bitmap)

        btnSaveAvatar.setOnClickListener {
            val intent = Intent()
            setResult(Constants.CHOOSE_IMAGE_RESPONSE, intent)
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