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
import com.devssocial.localodge.LocalodgeActivity
import com.devssocial.localodge.R
import com.devssocial.localodge.models.User
import com.devssocial.localodge.ui.dashboard.ui.DashboardFragment.Companion.REQUEST_CHECK_SETTINGS
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.google.firebase.auth.FirebaseAuth
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import pub.devrel.easypermissions.EasyPermissions
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

        // get user data from firestore and update SharedPreferences
        if (dashboardViewModel.getCurrentUser() == null) {
            // wait until LocalodgeActivity's checkAuthState() method is called and finished
            disposables.add(
                Completable
                    .timer(4, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            getCurrentUserData { user: User ->
                                saveToSharedPref(user)
                            }
                        },
                        { error ->
                            handleError(TAG, error, resources.getString(R.string.generic_error_message))
                        }
                    )
            )
        } else {
            getCurrentUserData { user: User ->
                saveToSharedPref(user)
            }
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
