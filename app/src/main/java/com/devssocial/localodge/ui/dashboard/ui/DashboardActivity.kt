package com.devssocial.localodge.ui.dashboard.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import com.devssocial.localodge.*
import com.devssocial.localodge.models.User
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import com.esafirm.imagepicker.features.ImagePicker
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name



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

    private fun saveToSharedPref(user: User) {
        val sharedPref = getSharedPreferences(
            LOCALODGE_SHARED_PREF,
            Context.MODE_PRIVATE
        ) ?: return
        with(sharedPref.edit()) {
            putString(USERNAME, user.username)
            putString(USER_PROFILE_URL, user.profilePicUrl)
            apply()
        }
    }

}
