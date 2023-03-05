package com.example.satellitechat.activity.client.profile

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.MainActivity
import com.example.satellitechat.activity.client.saveAvatar.SaveAvatarActivity
import com.example.satellitechat.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

class ProfileActivity : AppCompatActivity() {

    private var IMAGE_REQUEST: Int = 2002
    private var IMAGE_RESPONSE: Int = 2003
    private var currentUserId: String = ""
    private lateinit var filePath: Uri
    private lateinit var bitmap: Bitmap
    private lateinit var dialog: Dialog
    private lateinit var auth: FirebaseAuth
    private lateinit var byteArray: ByteArray
    private lateinit var storage: FirebaseStorage
    private lateinit var usersRef: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var byteArrayOutputStream: ByteArrayOutputStream

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = Firebase.auth
        sharedPreferences = getSharedPreferences("is_sign_in", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("userId", "").toString()
        storage = FirebaseStorage.getInstance()
        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        // Setup dialog "loading process"
        dialog = Dialog(this@ProfileActivity)
        dialog.setContentView(R.layout.dialog_loading)
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.setCancelable(false)

        icon_backspace.setOnClickListener {
            onBackPressed()
        }

        btnChooseAvatar.setOnClickListener {
            chooseImageProfile()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST && resultCode != IMAGE_RESPONSE) {
            filePath = data!!.data!!
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                byteArray = byteArrayOutputStream.toByteArray()
                val intent = Intent(this@ProfileActivity, SaveAvatarActivity::class.java)
                intent.putExtra("BitmapAvatar", byteArray)
                startActivityForResult(intent, IMAGE_REQUEST)
            } catch (error: IOException) {
                Toast.makeText(this@ProfileActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }
        if (requestCode == IMAGE_REQUEST && resultCode == IMAGE_RESPONSE) {
            dialog.show()
            uploadImageDatabase()
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this@ProfileActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        getDataUser()
    }

    private fun chooseImageProfile() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh đại diện"), IMAGE_REQUEST)
    }

    private fun uploadImageDatabase() {
        storageReference = storage.reference.child("image/avatar/" + UUID.randomUUID().toString())
        storageReference.putFile(filePath)
            .addOnSuccessListener { taskPut ->
                dialog.dismiss()
                imageProfile.setImageBitmap(bitmap)
                taskPut.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { taskDownload ->
                        val hashMap: HashMap<String, Any> = HashMap()
                        hashMap["userImage"] = taskDownload.toString()
                        usersRef.child(currentUserId).updateChildren(hashMap)
                    }
                    .addOnFailureListener { taskDownload ->
                        Toast.makeText(this@ProfileActivity, taskDownload.message, Toast.LENGTH_LONG).show()
                    }
                Toast.makeText(this@ProfileActivity, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(this@ProfileActivity, "Cập nhật ảnh đại diện thất bại!", Toast.LENGTH_LONG).show()
            }
    }

    private fun getDataUser() {
        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                userNameProfile.text = user!!.userName
                if (user.userImage == "") {
                    imageProfile.setImageResource(R.drawable.profile_image)
                    imageProfile.strokeWidth = 4F
                    imageProfile.setStrokeColorResource(R.color.primary)
                } else {
                    if (this@ProfileActivity.isDestroyed) {
                        return
                    } else {
                        Glide.with(this@ProfileActivity).load(user.userImage).into(imageProfile)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

}

