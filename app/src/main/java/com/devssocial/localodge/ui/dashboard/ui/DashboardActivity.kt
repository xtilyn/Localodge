package com.devssocial.localodge.ui.dashboard.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import com.devssocial.localodge.R
import com.devssocial.localodge.ui.dashboard.ui.DashboardFragment.Companion.REQUEST_CHECK_SETTINGS
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.google.firebase.auth.FirebaseAuth
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import pub.devrel.easypermissions.EasyPermissions

class DashboardActivity : AppCompatActivity() {

    private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        dashboardViewModel = ViewModelProviders.of(this)[DashboardViewModel::class.java]

        // make sure user is logged in
        checkAuthState()

        // status bar config
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onBackPressed() {
        if (dashboardViewModel.isDrawerOpen) dashboardViewModel.onBackPressed.onNext(true)
        else super.onBackPressed()
    }

    private fun checkAuthState() {
        // Check to make sure the user is logged in. Otherwise, proceed to Login
        val mAuth = FirebaseAuth.getInstance()
        if (mAuth.currentUser == null) ActivityLaunchHelper.goToLogin(this)
    }
}
