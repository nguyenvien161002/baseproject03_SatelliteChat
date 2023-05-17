package com.example.satellitechat.activity.client.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import com.example.satellitechat.R
import kotlinx.android.synthetic.main.fragment_friends.view.*

class FriendsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        val blue = ContextCompat.getColor(view.context, R.color.blue)
        val white = ContextCompat.getColor(view.context, R.color.white)
        val black70 = ContextCompat.getColor(view.context, R.color.black_70)
        val black = ContextCompat.getColor(view.context, R.color.black)

        view.btnSuggestions.backgroundTintList = ColorStateList.valueOf(blue)
        view.btnSuggestions.setTextColor(white)
        replaceFragment(SuggestedFriendsFragment())

        view.btnSuggestions.setOnClickListener {
            view.btnFriendRequests.backgroundTintList = ColorStateList.valueOf(black70)
            view.btnFriendRequests.setTextColor(black)
            view.btnYourFriends.backgroundTintList = ColorStateList.valueOf(black70)
            view.btnYourFriends.setTextColor(black)
            view.btnSuggestions.backgroundTintList = ColorStateList.valueOf(blue)
            view.btnSuggestions.setTextColor(white)
            replaceFragment(SuggestedFriendsFragment())
        }

        view.btnFriendRequests.setOnClickListener {
            view.btnSuggestions.backgroundTintList = ColorStateList.valueOf(black70)
            view.btnSuggestions.setTextColor(black)
            view.btnYourFriends.backgroundTintList = ColorStateList.valueOf(black70)
            view.btnYourFriends.setTextColor(black)
            view.btnFriendRequests.backgroundTintList = ColorStateList.valueOf(blue)
            view.btnFriendRequests.setTextColor(white)
            replaceFragment(FriendRequestsFragment())
        }

        view.btnYourFriends.setOnClickListener {
            view.btnSuggestions.backgroundTintList = ColorStateList.valueOf(black70)
            view.btnSuggestions.setTextColor(black)
            view.btnFriendRequests.backgroundTintList = ColorStateList.valueOf(black70)
            view.btnFriendRequests.setTextColor(black)
            view.btnYourFriends.backgroundTintList = ColorStateList.valueOf(blue)
            view.btnYourFriends.setTextColor(white)
            replaceFragment(YourFriendsFragment())
        }
        return view
    }


    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
        transaction.replace(R.id.frameLayoutFriendsFragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

}