package com.devssocial.localodge.ui.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProviders
import com.devssocial.localodge.*
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.User
import com.devssocial.localodge.room_models.UserRoom
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers


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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.menu_settings -> {
                ActivityLaunchHelper.goToSettings(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        getCurrentUserDataFromFirebase { user: User ->
            saveToRoom(user)
        }
    }

    override fun onBackPressed() {
        if (dashboardViewModel.isDrawerOpen) dashboardViewModel.onBackPressed.onNext(true)
        else super.onBackPressed()
    }

    private fun getCurrentUserDataFromFirebase(onSuccess: (User) -> Unit) {
        val userId = dashboardViewModel.userRepo.getCurrentUser()?.uid ?: return
        disposables.add(
            dashboardViewModel.userRepo.getUserData(userId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { error ->
                        logAndShowError(
                            TAG,
                            error,
                            resources.getString(R.string.generic_error_message)
                        )
                    },
                    onSuccess = { user: User ->
                        onSuccess(user)
                    }
                )
        )
    }

    private fun saveToRoom(user: User) {
        val userRoom = user.mapProperties(UserRoom()).apply {
            if (user.joinedDate != null)
                joinedDate = user.joinedDate!!.seconds * 1000
        }
        disposables.add(
            dashboardViewModel.userRepo.userDao
                .insert(userRoom)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        )
    }

}
