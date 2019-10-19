package com.devssocial.localodge.utils

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.instaGone
import com.devssocial.localodge.extensions.instaVisible
import kotlinx.android.synthetic.main.dialog_confirm_action.view.*

object DialogHelper {

    fun showConfirmActionDialog(
        context: Context,
        title: String? = null,
        message: String? = null,

        positiveButtonText: String,
        positiveButtonCallback: (dialog: AlertDialog) -> Unit,

        negativeButtonText: String,
        negativeButtonCallback: (dialog: AlertDialog) -> Unit,

        positiveButtonTextColor: Int = android.R.color.holo_red_dark
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

        dialogView.dialogPositiveButton.setTextColor(
            ContextCompat.getColor(
                context,
                positiveButtonTextColor
            )
        )

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

}