package com.devssocial.localodge.utils.helpers

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.devssocial.localodge.R
import com.devssocial.localodge.REPORT_REASONS
import com.devssocial.localodge.enums.ReportType
import com.devssocial.localodge.extensions.instaGone
import com.devssocial.localodge.extensions.instaVisible
import com.devssocial.localodge.extensions.popHide
import com.devssocial.localodge.extensions.popShow
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.android.synthetic.main.dialog_confirm_action.view.*
import kotlinx.android.synthetic.main.dialog_info.view.*
import kotlinx.android.synthetic.main.dialog_media_viewer.view.*
import kotlinx.android.synthetic.main.dialog_report.view.*
import kotlinx.android.synthetic.main.dialog_report.view.close_dialog
import kotlinx.android.synthetic.main.dialog_sign_in_required.view.*


class DialogHelper(private val context: Context) {

    lateinit var dialogView: View
    lateinit var dialog: AlertDialog

    fun showMediaDialog(
        photoUrl: String?,
        videoUrl: String?
    ) {
        createDialog(R.layout.dialog_media_viewer)
        if (photoUrl != null) {
            dialogView.dialog_post_photo.instaVisible()
            Glide.with(context)
                .load(photoUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(dialogView.findViewById<PhotoView>(R.id.dialog_post_photo))
        } else {
            dialogView.dialog_post_photo.instaGone()
            dialogView.dialog_post_video.instaVisible()
            dialogView.dialog_video_progress.instaVisible()
            val exoPlayerHelper = ExoPlayerHelper(
                playerView = dialogView.dialog_post_video,
                onError = {
                    dialogView.error_msg?.popShow()
                },
                onPlayerBuffer = { isBuffering ->
                    if (isBuffering) dialogView.dialog_video_progress?.popShow()
                    else dialogView.dialog_video_progress?.popHide()
                }
            )
            exoPlayerHelper.initializePlayer(videoUrl!!)
            dialog.setOnDismissListener {
                exoPlayerHelper.killPlayer()
            }
        }
        dialog.show()
    }

    fun createDialog(resourceId: Int, style: Int = R.style.DefaultDialogAnimation) {
        dialog = AlertDialog.Builder(context).create()
        dialogView = LayoutInflater.from(context).inflate(resourceId, null)
        dialog.setView(dialogView)
        dialog.window?.attributes?.windowAnimations = style
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun createFullScreenDialog(resourceId: Int, style: Int = R.style.DefaultDialogAnimation) {
        dialog = AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).create()
        dialogView = LayoutInflater.from(context).inflate(resourceId, null)
        dialog.setView(dialogView)
        dialog.window?.attributes?.windowAnimations = style
//        val height = context.resources.displayMetrics.heightPixels
//        val width = context.resources.displayMetrics.widthPixels
//
//        dialog.window?.setLayout(width, height)
    }

    fun setCancelable(cancelable: Boolean) {
        if (cancelable) {
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
        } else {
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
        }
    }

    fun showConfirmActionDialog(
        title: String? = null,
        message: String? = null,

        positiveButtonText: String,
        positiveButtonCallback: (dialog: AlertDialog) -> Unit,

        negativeButtonText: String,
        negativeButtonCallback: (dialog: AlertDialog) -> Unit,

        cancelable: Boolean = true
    ) {
        createDialog(R.layout.dialog_confirm_action)

        if (title == null) {
            dialogView.dialogTitle.instaGone()
        } else {
            dialogView.dialogTitle.text = title
            dialogView.dialogTitle.instaVisible()
        }

        if (message == null) {
            dialogView.dialogMessage.instaGone()
        } else {
            dialogView.dialogMessage.text = message
            dialogView.dialogMessage.instaVisible()
        }

        dialogView.dialogPositiveButton.text = positiveButtonText
        dialogView.dialogNegativeButton.text = negativeButtonText
        dialogView.dialogPositiveButton.setOnClickListener { positiveButtonCallback(dialog) }
        dialogView.dialogNegativeButton.setOnClickListener { negativeButtonCallback(dialog) }

        if (!cancelable) {
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
        }

        dialog.show()
    }

    fun showReportDialog(
        context: Context,
        reportType: ReportType,
        onSendReport: (String, String) -> Unit
    ) {
        createDialog(R.layout.dialog_report)

        val title = context.getString(
            R.string.report_holder,
            if (reportType == ReportType.USER)
                context.getString(R.string.user)
            else
                context.getString(R.string.post)
        )
        dialogView.report_title.text = title

        val reasonsAdapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, REPORT_REASONS)
        dialogView.report_reasons_spinner.adapter = reasonsAdapter

        dialogView.close_dialog.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.send_report_button.setOnClickListener {
            dialog.dismiss()
            onSendReport(
                dialogView.report_reasons_spinner.selectedItem.toString(),
                dialogView.report_desc_edittext.text.toString()
            )
        }

        dialog.show()
    }

    fun showSignInRequiredDialog(activity: Activity?, message: String) {
        createDialog(
            R.layout.dialog_sign_in_required,
            R.style.DefaultDialogAnimation
        )
        dialogView.sign_in_message.text = message
        dialogView.close_dialog.setOnClickListener {
            dialog.dismiss()
        }
        dialogView.dialog_sign_in_button.setOnClickListener {
            activity?.let { act ->
                ActivityLaunchHelper.goToLogin(act)
            }
        }
        dialog.show()
    }

    fun showInfoDialog(message: String, onDismiss: () -> Unit) {
        createDialog(R.layout.dialog_info)
        dialogView.info_message.text = message
        dialogView.dismiss_info.setOnClickListener {
            dialog.dismiss()
            onDismiss()
        }
        dialog.show()
    }
}