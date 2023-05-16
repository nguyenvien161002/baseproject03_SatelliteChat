package com.example.satellitechat.adapter.diffUtil

import androidx.recyclerview.widget.DiffUtil
import com.example.satellitechat.model.Message

class MessageDiffCallback (private val oldList: ArrayList<Message>, private val newList: ArrayList<Message>)
    : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].messageId == newList[newItemPosition].messageId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].messageId == newList[newItemPosition].messageId
    }
}