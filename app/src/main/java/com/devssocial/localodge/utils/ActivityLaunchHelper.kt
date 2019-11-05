package com.devssocial.localodge.utils

import android.app.Activity
import android.content.Intent
import com.devssocial.localodge.ui.dashboard.ui.DashboardActivity
import com.devssocial.localodge.ui.login.ui.LoginActivity
import com.devssocial.localodge.ui.post_detail.PostDetailActivity
import com.devssocial.localodge.ui.settings.SettingsActivity

class ActivityLaunchHelper {

    companion object {

        // INTENT KEYS
        private const val CONTENT_ID = "userId"

        fun goToLogin(activity: Activity?) {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity?.startActivity(intent)
            activity?.finish()
        }

        fun goToDashboard(activity: Activity?) {
            val intent = Intent(activity, DashboardActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()
        }

        fun goToPostDetail(activity: Activity?, postId: String) {
            goToSimpleActivity(activity, postId, PostDetailActivity::class.java)
        }

        fun goToSettings(activity: Activity?) {
            goToSimpleActivity(activity, null, SettingsActivity::class.java)
        }

        private fun goToSimpleActivity(
            activity: Activity?,
            contentId: String? = null,
            jClass: Class<*>,
            finishAfter: Boolean = false
        ) {
            val intent = Intent(activity, jClass)
            contentId?.let { intent.putExtra(CONTENT_ID, it) }
            activity?.startActivity(intent)
            if (finishAfter) activity?.finish()
        }

    }

}