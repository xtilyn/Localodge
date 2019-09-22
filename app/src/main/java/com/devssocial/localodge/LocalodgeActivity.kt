package com.devssocial.localodge

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.google.firebase.auth.FirebaseAuth
import es.dmoral.toasty.Toasty
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * Parent activity for all activities except LoginActivity
 */
@SuppressLint("Registered")
open class LocalodgeActivity : AppCompatActivity() {

    private val userRepo: UserRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // make sure user is logged in
        checkAuthState()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    private fun checkAuthState() {
        // Check to make sure the user is logged in. Otherwise, proceed to Login
        val mAuth = FirebaseAuth.getInstance()
        if (mAuth.currentUser == null) {
            userRepo.logOut()
            ActivityLaunchHelper.goToLogin(this)
        }
    }

    fun handleError(TAG: String, error: Throwable?, msgToUser: String) {
        Toasty.error(this, msgToUser, Toast.LENGTH_SHORT, true).show()
        if (error != null) Log.e(TAG, error.message ?: "error occurred", error)
    }

}