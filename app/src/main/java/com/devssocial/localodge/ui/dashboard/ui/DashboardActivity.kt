package com.devssocial.localodge.ui.dashboard.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import com.devssocial.localodge.LocalodgeActivity
import com.devssocial.localodge.R
import com.devssocial.localodge.models.User
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class DashboardActivity : LocalodgeActivity() {

    companion object {
        private const val TAG = "DashboardActivity"
    }

    private lateinit var dashboardViewModel: DashboardViewModel

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        dashboardViewModel = ViewModelProviders.of(this)[DashboardViewModel::class.java]

        // status bar config
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun onStart() {
        super.onStart()

        getCurrentUserData { user: User ->
            saveToSharedPref(user)
        }
    }

    override fun onBackPressed() {
        if (dashboardViewModel.isDrawerOpen) dashboardViewModel.onBackPressed.onNext(true)
        else super.onBackPressed()
    }

    private fun getCurrentUserData(onSuccess: (User) -> Unit) {
        // TODO CONTINUE HERE
    }

    private fun saveToSharedPref(user: User) {

    }

}
