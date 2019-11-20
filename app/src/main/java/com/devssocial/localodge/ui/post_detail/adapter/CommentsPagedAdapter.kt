package com.devssocial.localodge.ui.post_detail.adapter

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devssocial.localodge.interfaces.ListItemListener
import com.devssocial.localodge.models.CommentViewItem
import com.devssocial.localodge.shared.DiffCallback

class CommentsPagedAdapter(listener: ListItemListener) :
    PagedListAdapter<CommentViewItem, CommentsPagedAdapter.CommentViewHolder>(DiffCallback) {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(item: CommentViewItem) {
            // TODO
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}