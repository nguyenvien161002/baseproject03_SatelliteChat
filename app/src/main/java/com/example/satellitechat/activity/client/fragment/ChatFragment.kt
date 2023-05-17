package com.example.satellitechat.activity.client.fragment

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.activity.client.post.PostStatusActivity
import com.example.satellitechat.adapter.UserChatAdapter
import com.example.satellitechat.adapter.UserOnlineAdapter
import com.example.satellitechat.model.User
import com.example.satellitechat.utilities.constants.Constants
import com.example.satellitechat.utilities.preference.PreferenceManager
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.bottom_sheet_status_view.*
import kotlinx.android.synthetic.main.fragment_chat.view.*
import kotlin.collections.ArrayList

class ChatFragment : Fragment() {

    private var statusId: String = ""
    private var currentUserId: String = ""
    private val userChatList = ArrayList<User>()
    private val userOnlineList = ArrayList<User>()
    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var userChatAdapter: UserChatAdapter
    private lateinit var userOnlineAdapter: UserOnlineAdapter
    private lateinit var handlerDeleteStatusTimeOut: Runnable
    private lateinit var usersRef: DatabaseReference
    private lateinit var statusesRef: DatabaseReference
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false);
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
        statusesRef = FirebaseDatabase.getInstance().getReference(Constants.STATUSES_REF)
        preferenceManager = PreferenceManager(requireContext())
        currentUserId = preferenceManager.getCurrentId().toString()
        view.progressBarChatBox.visibility = ProgressBar.VISIBLE

        view.btnLeaveANote.setOnClickListener {
            val intent = Intent(view.context, PostStatusActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Get img user for contentStatusContainer
        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)!!
                if (isAdded) {
                    Glide.with(view).load(user.userImage).placeholder(R.drawable.profile_image).into(view.imageUser)
                    Glide.with(view).load(user.userImage).placeholder(R.drawable.profile_image).into(view.imageUserPosted)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })


        // Get user chat box
        view.userRecyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userChatList.clear()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if(user!!.userId != currentUserId) {
                        userChatList.add(user)
                    }
                }
                userChatAdapter = UserChatAdapter(view.context, userChatList)
                view.userRecyclerView.adapter = userChatAdapter
                view.progressBarChatBox.visibility = ProgressBar.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        // Get users online
        userOnlineAdapter = UserOnlineAdapter(view.context, ArrayList())
        view.userOnlineRecyclerView.apply {
            layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
            adapter = userOnlineAdapter
        }
        var user: User = User()
        var type: String = String()
//        var posterId : String =  String()
//        var expiry: String = String()
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userOnlineList.clear()
                for (dataSnapshotUser: DataSnapshot in snapshot.children) {
                    user = dataSnapshotUser.getValue(User::class.java)!!
                    type = dataSnapshotUser.child("userState").child("type").value.toString()
                    if(user.userId != currentUserId && type == "online") {
                        userOnlineList.add(user)
                    }
                }
                userOnlineAdapter.updateUserOnlineList(userOnlineList)
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        // Get status posted for user
        statusId = preferenceManager.getString(Constants.STATUS_ID).toString()
        statusesRef.child(statusId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userId = snapshot.child("posterId").value
                if (userId == currentUserId) {
                    val contentStatus = snapshot.child("contentStatus").value
                    val expiry = snapshot.child("expiry").value
                    val statusIdDb = snapshot.child("statusId").value.toString()
                    handlerDeleteStatusTimeOut = Runnable {
                        statusesRef.child(statusIdDb).child("expiry").setValue("false")
                    }
                    handler.postDelayed(handlerDeleteStatusTimeOut, 24*60*60*1000)
                    if (expiry == "true") { // check xem đã đăng qua 24h chưa
                        view.btnLeaveANote.visibility = View.GONE
                        view.contentStatusContainer.visibility = View.VISIBLE
                        view.labelLeaveANote.text = "Your note"
                        view.contentStatus.text = contentStatus as CharSequence?
                    } else {
                        view.btnLeaveANote.visibility = View.VISIBLE
                        view.contentStatusContainer.visibility = View.GONE
                    }
                    view.contentStatusContainer.setOnClickListener {
                        showBottomDialogStatus(view, statusId, contentStatus.toString())
                    }
                } else {
                    view.btnLeaveANote.visibility = View.VISIBLE
                    view.contentStatusContainer.visibility = View.GONE
                }

            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        return view;
    }

    private fun showBottomDialogStatus(view: View, statusId: String, contentStatus: String) {
        val dialog = Dialog(view.context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_status_view)

        // Get imageUser from DB
        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)!!
                if (isAdded) {
                    Glide.with(view).load(user.userImage).placeholder(R.drawable.profile_image).into(dialog.imageUserPostedStatus)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(view.context, error.message, Toast.LENGTH_LONG).show()
            }
        })

        dialog.contentDialogStatusView.text = contentStatus

        dialog.btnLeaveANewNote.setOnClickListener {
            val intent = Intent(view.context, PostStatusActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.btnDeleteNote.setOnClickListener {
            statusesRef.child(statusId).removeValue()
            handler.removeCallbacks(handlerDeleteStatusTimeOut)
            dialog.dismiss()
        }

        dialog.btnShareWithFriend.setOnClickListener {
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

}



