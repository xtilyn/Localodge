package com.devssocial.localodge.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.instaGone
import com.devssocial.localodge.extensions.instaVisible
import com.devssocial.localodge.models.Location
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.utils.DateUtils
import com.devssocial.localodge.utils.helpers.LocationHelper
import kotlinx.android.synthetic.main.list_item_user_post.view.*

class PostViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        fun bindItem(
            item: PostViewItem,
            itemView: View,
            userLocation: Location,
            isLikeFilled: Boolean
        ) {
            if (itemView.context == null) return

            Glide.with(itemView.context)
                .load(item.posterProfilePic)
                .into(itemView.user_post_profile_pic)

            if (item.photoUrl == null && item.videoUrl == null)
                itemView.user_post_media_content_container.instaGone()

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

            if (item.timestamp != null) {
                itemView.user_post_timestamp?.text = DateUtils.convertMessageTimestamp(
                    item.timestamp!!.seconds * 1000L)
            }

            if (item.postDescription.isNotBlank()) {
                itemView.user_post_description.text = item.postDescription
                itemView.user_post_description.instaVisible()
            }
            itemView.user_post_username.text = item.posterUsername
            itemView.comments_text.text = item.commentsCount.toString()
            itemView.likes_text.text = item.likes.size.toString()
            itemView.user_post_like_checkbox.isChecked = isLikeFilled

            if (item.rating > 0) {
                itemView.user_post_promoted_text.instaVisible()
            } else {
                itemView.user_post_promoted_text.instaGone()
            }

            val locationFormatted = "${
            LocationHelper.distFrom(
                userLocation.lat.toFloat(),
                userLocation.lng.toFloat(),
                item._geoloc.lat.toFloat(),
                item._geoloc.lng.toFloat()
            )
            } km"
            itemView.distance_text.text = locationFormatted
        }
    }

    fun toggleLike(current: PostViewItem, toggleCheck: Boolean) {
        if (toggleCheck) {
            val toggleVal = !itemView.user_post_like_checkbox.isChecked
            itemView.user_post_like_checkbox.isChecked = toggleVal
        }
        itemView.likes_text.text = (current.likes.size).toString()
    }
}