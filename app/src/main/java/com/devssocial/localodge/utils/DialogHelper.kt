package com.devssocial.localodge.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.devssocial.localodge.R
import com.devssocial.localodge.REPORT_REASONS
import com.devssocial.localodge.enums.ReportType
import com.devssocial.localodge.extensions.instaGone
import com.devssocial.localodge.extensions.instaVisible
import kotlinx.android.synthetic.main.dialog_confirm_action.view.*
import kotlinx.android.synthetic.main.dialog_report.view.*

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
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_confirm_action, null)
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

            showDialog(dialog)
        }

        fun showReportDialog(
            context: Context,
            reportType: ReportType,
            onSendReport:(String, String) -> Unit
        ) {
            val dialog = AlertDialog.Builder(context).create()
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_report, null)
            dialog.setView(dialogView)

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

            showDialog(dialog)
        }

        private fun showDialog(dialog: AlertDialog) {
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