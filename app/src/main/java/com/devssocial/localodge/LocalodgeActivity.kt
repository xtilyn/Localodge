package com.devssocial.localodge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devssocial.localodge.shared.UserRepository.Companion.AUTH_BROADCAST
import com.devssocial.localodge.utils.helpers.ActivityLaunchHelper
import com.google.firebase.auth.FirebaseAuth
import es.dmoral.toasty.Toasty
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception

/**
 * Parent activity for all activities except LoginActivity
 */
abstract class LocalodgeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LocalodgeActivity"
    }

    private val broadcastReceiver by lazy {
        object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val sharedPref = getSharedPreferences(LOCALODGE_SHARED_PREF, Context.MODE_PRIVATE) ?: return
                val isTrial = sharedPref.getBoolean(TRIAL_ACCOUNT_REQUESTED, false)
                if (!isTrial) logOut()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(broadcastReceiver, IntentFilter(AUTH_BROADCAST))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    fun logOut() {
        Completable.create {  e ->
            try {
                LocalodgeRoomDatabase.getDatabase(this).clearAllTables()
                e.onComplete()
            } catch (error: Exception) {
                Log.e(TAG, error.message, error)
                e.onError(error)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe()
        FirebaseAuth.getInstance().signOut()
        ActivityLaunchHelper.goToLogin(this)
        val sharedPref = getSharedPreferences(LOCALODGE_SHARED_PREF, Context.MODE_PRIVATE) ?: return
        sharedPref.edit().clear().apply()
    }

    fun logAndShowError(TAG: String, error: Throwable?, msgToUser: String) {
        Toasty.error(this, msgToUser, Toast.LENGTH_SHORT, true).show()
        if (error != null) Log.e(TAG, error.message ?: "error occurred", error)
    }

}