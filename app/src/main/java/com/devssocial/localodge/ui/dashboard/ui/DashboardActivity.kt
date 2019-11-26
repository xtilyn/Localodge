package com.devssocial.localodge.ui.dashboard.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import com.devssocial.localodge.*
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.User
import com.devssocial.localodge.room_models.UserRoom
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.helpers.ActivityLaunchHelper
import com.devssocial.localodge.utils.helpers.DialogHelper
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (dashboardViewModel.isUserLoggedIn()) {
            menuInflater.inflate(R.menu.menu_dashboard_toolbar, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
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
        when {
            dashboardViewModel.isDrawerOpen -> dashboardViewModel.onBackPressed.onNext(true)
            dashboardViewModel.newPostCallback?.hasData() == true -> {
                DialogHelper(this)
                    .showConfirmActionDialog(
                        getString(R.string.discard_changes),
                        getString(R.string.are_you_sure_you_want_to_discard_changes),
                        getString(R.string.discard),
                        { dialog ->
                            dialog.dismiss()
                            super.onBackPressed()
                        },
                        getString(R.string.cancel),
                        { dialog -> dialog.dismiss() },
                        false
                    )
            }
            else -> super.onBackPressed()
        }
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
