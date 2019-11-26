package com.devssocial.localodge.utils.helpers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.devssocial.localodge.R
import com.devssocial.localodge.enums.ReportType
import com.devssocial.localodge.interfaces.PostOptionsListener
import com.devssocial.localodge.models.PostViewItem
import kotlinx.android.synthetic.main.popup_user_post_more_options.view.*

class PostsHelper(private val listener: PostOptionsListener) {

    fun showMoreOptionsPopup(
        context: Context?,
        viewAnchor: View,
        current: PostViewItem,
        position: Int?
    ) {
        if (context == null) return
        val popupView = LayoutInflater.from(context)
            .inflate(R.layout.popup_user_post_more_options, null)
        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupView.popup_user_post_report_post.setOnClickListener {
            DialogHelper(context)
                .showReportDialog(context, ReportType.POST) { reason, desc ->
                listener.onReportPost(current.objectID, reason, desc)
            }
        }
        popupView.popup_user_post_report_user.setOnClickListener {
            DialogHelper(context)
                .showReportDialog(context, ReportType.USER) { reason, desc ->
                listener.onReportUser(current.posterUserId, reason, desc)
            }
        }
        popupView.popup_user_post_block_user.setOnClickListener {
            DialogHelper(context).showConfirmActionDialog(
                context.resources.getString(R.string.are_you_sure),
                context.resources.getString(R.string.are_you_sure_you_want_to_block_this_user),
                context.resources.getString(R.string.block_user),
                { dialog ->
                    dialog.dismiss()
                    listener.onBlockUser(current.posterUserId)
                },
                context.resources.getString(R.string.cancel),
                { dialog ->
                    dialog.dismiss()
                }
            )
        }
        popupView.popup_user_post_hide.setOnClickListener {
            DialogHelper(context).showConfirmActionDialog(
                null,
                context.resources.getString(R.string.confirm_hide_post),
                context.resources.getString(R.string.yes),
                { dialog ->
                    dialog.dismiss()
                    listener.onBlockPost(current, position)
                },
                context.resources.getString(R.string.cancel),
                { dialog ->
                    dialog.dismiss()
                }
            )
        }
        popup.showAsDropDown(viewAnchor)
    }

}