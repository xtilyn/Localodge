package com.devssocial.localodge.ui.post_detail.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devssocial.localodge.R
import com.devssocial.localodge.data_objects.AdapterPayload
import com.devssocial.localodge.extensions.instaGone
import com.devssocial.localodge.extensions.instaVisible
import com.devssocial.localodge.extensions.popShow
import com.devssocial.localodge.interfaces.ListItemListener
import com.devssocial.localodge.models.CommentViewItem
import com.devssocial.localodge.shared.DiffCallback
import com.devssocial.localodge.utils.DateUtils

class CommentsPagedAdapter(
    private val listener: ListItemListener,
    private val currentUserId: String
) :
    PagedListAdapter<CommentViewItem, CommentsPagedAdapter.CommentViewHolder>(DiffCallback) {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        fun bindItem(item: CommentViewItem?) {
            if (item == null) return

            val displayName = itemView.findViewById<TextView>(R.id.comment_poster_username)
            val commentTextView = itemView.findViewById<TextView>(R.id.comment_body)
            val profilePic = itemView.findViewById<ImageView>(R.id.comment_profile_pic)
            val toggleCommentButton = itemView.findViewById<TextView>(R.id.comment_toggle_text)
            val timestamp = itemView.findViewById<TextView>(R.id.comment_timestamp)
            val commentPhotoContainer = itemView.findViewById<CardView>(R.id.comment_photo_container)
            val commentPhoto = itemView.findViewById<ImageView>(R.id.comment_photo)

            Glide.with(itemView.context)
                .load(item.postedByProfilePic)
                .into(profilePic)

            if (item.photoUrl != null) {
                commentPhotoContainer.instaVisible()
                Glide.with(itemView.context)
                    .load(item.photoUrl)
                    .into(commentPhoto)
                commentPhotoContainer.setOnClickListener(this)
            } else {
                commentPhotoContainer.instaGone()
                commentPhoto.setImageDrawable(null)
                commentPhotoContainer.setOnClickListener(null)
            }

            displayName.text = item.postedByUsername

            if (item.body.isNotBlank()) {
                commentTextView.text = item.body
                commentTextView.instaVisible()
            } else {
                commentTextView.text = ""
                commentTextView.instaGone()
            }

            timestamp.instaVisible()
            timestamp.text = DateUtils.convertMessageTimestamp(item.timestamp!!.seconds * 1000L)


            if (item.isExpanded) {
                toggleCommentButton.text = itemView.context.getText(R.string.see_less)
                toggleCommentButton.instaVisible()
            } else {
                val vto = commentTextView.viewTreeObserver
                var isInitialized = false
                vto.addOnGlobalLayoutListener {

                    if (!isInitialized) {
                        val commentLineCount = commentTextView.layout?.lineCount

                        // check if comment is ellipsized
                        if ((commentLineCount != null && commentLineCount > 0
                                    && commentTextView.layout.getEllipsisCount(commentLineCount - 1) > 0)
                            || item.isCommentTooLong
                        ) {
                            toggleCommentButton.text = itemView.context.getText(R.string.see_more)
                            toggleCommentButton.instaVisible()
                            item.isCommentTooLong = true
                        } else {
                            toggleCommentButton.instaGone()
                        }

                        isInitialized = true
                    }

                }
            }

            // configure clickable attrs based on current user id
            if (currentUserId != item.postedBy) {
                val outValue = TypedValue()
                itemView.context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                itemView.background = itemView.context.getDrawable(outValue.resourceId)
                itemView.setOnLongClickListener {
                    listener.onItemLongPress(
                        it,
                        adapterPosition
                    )
                }
            } else {
                itemView.background = null
                itemView.setOnLongClickListener(null)
            }

            // setup listeners
            toggleCommentButton.setOnClickListener(this)
        }

        /**
         * Toggle 'see more' || 'see less' button for comments > 3 lines
         */
        fun toggleCommentText(comment: CommentViewItem?) {
            if (comment == null) return
            val toggleCommentButton = itemView.findViewById<TextView>(R.id.comment_toggle_text)
            val commentText = itemView.findViewById<TextView>(R.id.comment_body)

            if (!comment.isCommentTooLong) return
            if (comment.isExpanded) {
                // collapse comment
                commentText.maxLines = 3
                toggleCommentButton.text = itemView.context.getString(R.string.see_more)
                toggleCommentButton.instaVisible()
            } else {
                // expand comment
                commentText.maxLines = Integer.MAX_VALUE
                toggleCommentButton.text = itemView.context.getString(R.string.see_less)
                toggleCommentButton.instaVisible()
            }
            comment.isExpanded = !comment.isExpanded
        }

        override fun onClick(v: View?) {
            v?.let {
                listener.onItemClick(it, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_comment, parent, false)
        return CommentViewHolder(v)
    }

    override fun onBindViewHolder(
        holder: CommentViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                AdapterPayload.EXPAND_OR_COLLAPSE -> {
                    holder.toggleCommentText(currentList?.get(position))
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bindItem(currentList?.get(position))
    }

}