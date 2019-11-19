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
import com.devssocial.localodge.utils.LocationHelper
import kotlinx.android.synthetic.main.list_item_user_post.view.*

class PostViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        fun bindItem(
            item: PostViewItem,
            itemView: View,
            userLocation: Location
        ) {
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

            itemView.user_post_timestamp?.text = DateUtils.convertMessageTimestamp(
                item.createdDate!!.seconds * 1000L)

            itemView.user_post_description.text = item.postDescription
            itemView.user_post_username.text = item.posterUsername
            itemView.user_post_comment.text = itemView.context
                .resources.getString(R.string.user_post_comments, item.comments.size.toString())

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

    fun toggleLike() {
        val currDrawable =
            itemView.user_post_like?.compoundDrawables?.get(0)?.constantState ?: return
        val starOutline =
            itemView.context.resources.getDrawable(R.drawable.ic_star_border, null)
                .constantState
        if (currDrawable == starOutline) {
            itemView.user_post_like?.setCompoundDrawables(
                itemView.context.resources.getDrawable(R.drawable.ic_star_filled, null),
                null,
                null,
                null
            )
        } else {
            itemView.user_post_like?.setCompoundDrawables(
                itemView.context.resources.getDrawable(R.drawable.ic_star_border, null),
                null,
                null,
                null
            )
        }
    }
}