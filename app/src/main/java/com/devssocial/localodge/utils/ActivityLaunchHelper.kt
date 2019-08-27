package com.devssocial.localodge.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.devssocial.localodge.ui.dashboard.DashboardActivity
import com.devssocial.localodge.ui.login.LoginActivity

class ActivityLaunchHelper {

    companion object {

        fun goToUserProfile(context: Context? = null) {
            // TODO
        }

        fun goToLogin(activity: Activity? = null) {
            val intent = Intent(activity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity?.startActivity(intent)
            activity?.finish()
        }

        fun goToDashboard(activity: Activity? = null) {
            val intent = Intent(activity, DashboardActivity::class.java)
            activity?.startActivity(intent)
            activity?.finish()
        }

    }

}