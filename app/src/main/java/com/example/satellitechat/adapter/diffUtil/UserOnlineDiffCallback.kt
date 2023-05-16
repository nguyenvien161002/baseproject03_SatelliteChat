package com.example.satellitechat.adapter.diffUtil

import androidx.recyclerview.widget.DiffUtil
import com.example.satellitechat.model.User

class UserOnlineDiffCallback (private val oldList: ArrayList<User>, private val newList: ArrayList<User>)
    : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].userId == newList[newItemPosition].userId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].userId == newList[newItemPosition].userId
    }
}