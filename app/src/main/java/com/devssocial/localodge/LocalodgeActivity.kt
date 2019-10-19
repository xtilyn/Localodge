package com.devssocial.localodge

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.shared.UserRepository.Companion.AUTH_BROADCAST
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.google.firebase.auth.FirebaseAuth
import es.dmoral.toasty.Toasty
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * Parent activity for all activities except LoginActivity
 */
@SuppressLint("Registered")
open class LocalodgeActivity : AppCompatActivity() {

    private val broadcastReceiver by lazy {
        object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                logOut()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(broadcastReceiver, IntentFilter(AUTH_BROADCAST))
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    fun logOut() {
        val sharedPref = getSharedPreferences(LOCALODGE_SHARED_PREF, Context.MODE_PRIVATE) ?: return
        sharedPref.edit().clear().apply()
        FirebaseAuth.getInstance().signOut()
        ActivityLaunchHelper.goToLogin(this)
    }

    fun logAndShowError(TAG: String, error: Throwable?, msgToUser: String) {
        Toasty.error(this, msgToUser, Toast.LENGTH_SHORT, true).show()
        if (error != null) Log.e(TAG, error.message ?: "error occurred", error)
    }

}