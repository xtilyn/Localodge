package com.devssocial.localodge.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.instaGone
import com.devssocial.localodge.extensions.instaVisible
import kotlinx.android.synthetic.main.dialog_confirm_action.view.*

class DialogHelper(private val context: Context) {

    companion object {
        fun showConfirmActionDialog(
            context: Context,
            title: String? = null,
            message: String? = null,

            positiveButtonText: String,
            positiveButtonCallback: (dialog: AlertDialog) -> Unit,

            negativeButtonText: String,
            negativeButtonCallback: (dialog: AlertDialog) -> Unit
        ) {
            val dialog = AlertDialog.Builder(context).create()
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_action, null)
            dialog.setView(dialogView)

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

            dialog.window?.attributes?.windowAnimations = R.style.DefaultDialogAnimation
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()
        }
    }

    lateinit var dialogView: View
    lateinit var dialog: AlertDialog

    fun createDialog(resourceId: Int, style: Int? = null) {
        dialog = AlertDialog.Builder(context).create()
        dialogView = LayoutInflater.from(context).inflate(resourceId, null)
        dialog.setView(dialogView)
        if (style != null) {
            dialog.window?.attributes?.windowAnimations = style
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
}