package com.devssocial.localodge.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.devssocial.localodge.R
import com.devssocial.localodge.interfaces.ListItemListener
import com.devssocial.localodge.data_objects.AdapterPayload
import com.devssocial.localodge.models.Location
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.view_holders.PostViewHolder
import kotlinx.android.synthetic.main.list_item_user_post.view.*

class PostsAdapter(
    val userId: String?,
    val data: ArrayList<PostViewItem>,
    private val listener: ListItemListener,
    private val userLocation: Location
) :
    RecyclerView.Adapter<PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user_post, parent, false)
        return PostViewHolder(v)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val current = data[position]
        PostViewHolder.bindItem(current, holder.itemView, userLocation, current.likes.containsKey(userId))

        val currPos = holder.adapterPosition
        val onClick = View.OnClickListener { v ->
            v?.let { listener.onItemClick(it, currPos) }
        }
        holder.itemView.user_post_profile_pic.setOnClickListener(onClick)
        holder.itemView.user_post_username.setOnClickListener(onClick)
        holder.itemView.user_post_media_content_container.setOnClickListener(onClick)
        holder.itemView.user_post_like.setOnClickListener(onClick)
        holder.itemView.user_post_comment.setOnClickListener(onClick)
        holder.itemView.user_post_more_options.setOnClickListener(onClick)
        holder.itemView.user_post_like_checkbox.setOnClickListener(onClick)
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                is List<*> -> {
                    holder.toggleLike(data[position], (payloads[0] as List<*>)[1] as Boolean)
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun updateList(newData: ArrayList<PostViewItem>) {
        val oldSize = data.size
        data.clear()
        notifyItemRangeRemoved(0, oldSize)
        data.addAll(newData)
        notifyItemRangeInserted(0, newData.size)
    }

    fun appendToList(moreData: ArrayList<PostViewItem>) {
        val lastItemIndex = data.size - 1
        data.addAll(moreData)
        notifyItemRangeInserted(lastItemIndex, moreData.size)
    }

    fun clear() {
        val oldSize = data.size
        data.clear()
        notifyItemRangeRemoved(0, oldSize)
    }

}