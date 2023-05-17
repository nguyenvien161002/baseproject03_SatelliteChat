package com.example.satellitechat.activity.client.post

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.MainActivity
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.example.satellitechat.utilities.time.TimeAndDateGeneral
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_status_post.*
import kotlinx.android.synthetic.main.bottom_sheet_navigation.*


class PostStatusActivity : AppCompatActivity() {

    private var currentUserId: String = ""
    private var checkPostPermission: String = ""
    private lateinit var dialog: Dialog
    private lateinit var usersRef: DatabaseReference
    private lateinit var statusesRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_post)

        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        statusesRef = FirebaseDatabase.getInstance().getReference(Constants.STATUSES_REF)
        preferenceManager = PreferenceManager(this@PostStatusActivity)
        currentUserId = preferenceManager.getCurrentId().toString()

        // Setup dialog "loading process"
        dialog = Dialog(this@PostStatusActivity)
        dialog.setContentView(R.layout.dialog_loading)
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.setCancelable(false)

        btnClosePostStatus.setOnClickListener {
            onBackPressed()
        }

        btnPostStatus.setOnClickListener {
            if (contentStatus.text.isNotEmpty()) {
                dialog.show()
                checkPostPermission =
                    preferenceManager.getString(Constants.STATUS_PERMISSION).toString()
                if (checkPostPermission != "") {
                    postStatus(contentStatus.text.toString(), checkPostPermission)
                } else {
                    postStatus(contentStatus.text.toString(), "public")
                }
                onBackPressed()
            }
        }

        contentStatus.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(contentStatus, InputMethodManager.SHOW_IMPLICIT)
        contentStatus.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val length: Int = s.toString().length
                textCount.text = "${60 - length}/60"
                // Nếu số từ vượt quá giới hạn 60, đặt màu chữ của TextView thành màu đỏ
                if (length >= 60) {
                    textCount.setTextColor(resources.getColor(R.color.red));
                } else {
                    // Nếu số từ không vượt quá giới hạn 60, đặt màu chữ của TextView thành màu đen
                    textCount.setTextColor(resources.getColor(R.color.black));
                    contentStatus.setTextColor(resources.getColor(R.color.black));
                    if (length == 0) {
                        btnPostStatus.setTextColor(resources.getColor(R.color.black_400));
                    } else {
                        btnPostStatus.setTextColor(resources.getColor(R.color.blue));
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        btnChoosePostPermission.setOnClickListener {
            showBottomDialog();
        }

        // Render từ dữ liệu cũ xem đã chọn permission chưa
        checkPostPermission = preferenceManager.getString(Constants.STATUS_PERMISSION).toString()
        if (checkPostPermission == "public") {
            postPermissionFrFollow.visibility = View.GONE
            postPermissionPublic.visibility = View.VISIBLE
        } else {
            postPermissionFrFollow.visibility = View.VISIBLE
            postPermissionPublic.visibility = View.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@PostStatusActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun postStatus(contentStatus: String, postPermission: String) {
        val hashMap: HashMap<String, Any> = HashMap()
        val pushStatusPost = statusesRef.push()
        preferenceManager.setString(Constants.STATUS_ID, pushStatusPost.key.toString())
        hashMap["statusId"] = pushStatusPost.key.toString()
        hashMap["posterId"] = currentUserId
        hashMap["expiry"] = "true"
        hashMap["contentStatus"] = contentStatus
        hashMap["postPermission"] = postPermission
        hashMap["timeStamp"] = TimeAndDateGeneral().getCurrentTimeAndDate()
        pushStatusPost.setValue(hashMap)
        dialog.dismiss()
    }

    private fun showBottomDialog() {
        val dialog = Dialog(this@PostStatusActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_navigation)

        // Chọn rồi nhưng muốn chọn lại thì get lại trong perfer để lấy ra cái đã chọn trc đó
        checkPostPermission = preferenceManager.getString(Constants.STATUS_PERMISSION).toString()
        Log.d("CHECK_POST_PER_SHOW_BOT_DIALOG", checkPostPermission)
        if (checkPostPermission == "public") {
            dialog.radioCheckPublic.setImageResource(R.drawable.bg_radio_checked)
            dialog.radioCheckFriFollow.setImageResource(R.drawable.icons8_circle_30)
        } else {
            dialog.radioCheckPublic.setImageResource(R.drawable.icons8_circle_30)
            dialog.radioCheckFriFollow.setImageResource(R.drawable.bg_radio_checked)
        }

        dialog.btnChoosePostPermissionFriFollow.setOnClickListener {
            preferenceManager.setString(Constants.STATUS_PERMISSION, "followers_you_follow_back")
            dialog.radioCheckFriFollow.setImageResource(R.drawable.bg_radio_checked)
            dialog.radioCheckPublic.setImageResource(R.drawable.icons8_circle_30)
        }

        dialog.btnChoosePostPermissionPublic.setOnClickListener {
            preferenceManager.setString(Constants.STATUS_PERMISSION, "public")
            dialog.radioCheckPublic.setImageResource(R.drawable.bg_radio_checked)
            dialog.radioCheckFriFollow.setImageResource(R.drawable.icons8_circle_30)
        }

        dialog.btnCancelChoosePostPermission.setOnClickListener {
            dialog.dismiss()
        }

        dialog.btnDoneChoosePostPermission.setOnClickListener {
            checkPostPermission =
                preferenceManager.getString(Constants.STATUS_PERMISSION).toString()
            if (checkPostPermission == "public") {
                postPermissionFrFollow.visibility = View.GONE
                postPermissionPublic.visibility = View.VISIBLE
            } else {
                postPermissionFrFollow.visibility = View.VISIBLE
                postPermissionPublic.visibility = View.GONE
            }
            dialog.dismiss()
        }

        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun getInfoUser() {
        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imgUserDb = snapshot.child("userImage").value
                if (imgUserDb == "") {
                    imageUser.setImageResource(R.drawable.avatar)
                    Glide.with(this@PostStatusActivity).load(R.drawable.avatar).into(imageUser)
                } else {
                    if (this@PostStatusActivity.isDestroyed) {
                        return
                    } else {
                        Glide.with(this@PostStatusActivity).load(imgUserDb).placeholder(R.drawable.avatar).into(imageUser)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PostStatusActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        getInfoUser()
    }
}