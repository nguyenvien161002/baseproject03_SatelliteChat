package com.example.satellitechat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.satellitechat.R
import com.example.satellitechat.model.Image
import kotlin.collections.ArrayList

class ImageAdapter(private val context: Context, private val imageList: ArrayList<Image>) :
    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = imageList[position]
        if (image.url != null) {
            Glide.with(context).load(image.url).into(holder.imageView)
        } else {
            Glide.with(context).load(image.uri).into(holder.imageView)
        }

    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val imageView: ImageView = view.findViewById<ImageView>(R.id.imageSelected)
    }

}