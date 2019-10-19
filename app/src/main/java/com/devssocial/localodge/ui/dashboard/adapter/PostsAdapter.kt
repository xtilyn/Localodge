package com.devssocial.localodge.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devssocial.localodge.R
import com.devssocial.localodge.callbacks.ListItemListener
import com.devssocial.localodge.extensions.instaVisible
import com.devssocial.localodge.models.PostViewItem
import kotlinx.android.synthetic.main.list_item_user_post.view.*

class PostsAdapter(private val data: ArrayList<PostViewItem>, private val listener: ListItemListener) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        override fun onClick(v: View?) {
            v?.let { listener.onItemClick(v, adapterPosition) }
        }

        fun bindItem(item: PostViewItem) {
            if (itemView.context == null) return

            Glide.with(itemView.context)
                .load(item.posterProfilePic)
                .into(itemView.user_post_profile_pic)

            item.photoUrl?.let {
                Glide.with(itemView.context)
                    .load(it)
                    .into(itemView.user_post_image_content)
                itemView.user_post_media_content_container.instaVisible()
            }

            item.videoUrl?.let {
                Glide.with(itemView.context)
                    .load(it)
                    .into(itemView.user_post_image_content)
                itemView.user_post_media_content_container.instaVisible()
            }

            itemView.user_post_description.text = item.postDescription
            itemView.user_post_username.text = item.posterUsername
            itemView.user_post_comment.text = itemView.context
                .resources.getString(R.string.user_post_comments, item.comments.size.toString())

            // listeners
            itemView.user_post_profile_pic.setOnClickListener(this)
            itemView.user_post_username.setOnClickListener(this)
            itemView.user_post_media_content_container.setOnClickListener(this)
            itemView.user_post_like.setOnClickListener(this)
            itemView.user_post_comment.setOnClickListener(this)
            itemView.user_post_more_options.setOnClickListener(this)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_user_post, parent, false)
        return PostViewHolder(v)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val current = data[position]
        holder.bindItem(current)
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
        data.addAll( moreData)
        notifyItemRangeInserted(lastItemIndex, moreData.size)
    }

    fun clear() {
        val oldSize = data.size
        data.clear()
        notifyItemRangeRemoved(0, oldSize)
    }

}