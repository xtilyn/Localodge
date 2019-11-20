package com.devssocial.localodge.shared

import androidx.recyclerview.widget.DiffUtil
import com.devssocial.localodge.models.CommentViewItem

object DiffCallback : DiffUtil.ItemCallback<CommentViewItem>() {
    override fun areItemsTheSame(oldItem: CommentViewItem, newItem: CommentViewItem) =
        oldItem.objectID == newItem.objectID

    override fun areContentsTheSame(
        oldItem: CommentViewItem, newItem: CommentViewItem
    ) = oldItem == newItem
}