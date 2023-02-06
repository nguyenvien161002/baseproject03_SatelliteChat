package com.example.satellitechat.activity.client.sidebar

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.satellitechat.R
import com.example.satellitechat.activity.authentication.SignInActivity
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var loginManager: LoginManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_chat, container, false)
        val btnSignOut = root.findViewById<Button>(R.id.btnSignOut)
        auth = Firebase.auth
        loginManager = LoginManager.getInstance()
        btnSignOut.setOnClickListener {
            auth.signOut()
            loginManager.logOut()
            sharedPreferences = this.requireActivity().getSharedPreferences("is_sign_in", AppCompatActivity.MODE_PRIVATE)
            editor = sharedPreferences.edit()
            editor.putString("is_sign_in", "false")
            editor.apply()
            startActivity(Intent(activity, SignInActivity::class.java))
            requireActivity().finish()
        }
        return root;
    }

}

