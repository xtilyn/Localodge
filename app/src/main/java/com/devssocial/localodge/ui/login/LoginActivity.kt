package com.devssocial.localodge.ui.login

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.devssocial.localodge.R
import com.devssocial.localodge.ui.login.view_model.LoginViewModel
import io.github.inflationx.viewpump.ViewPumpContextWrapper


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginViewModel = ViewModelProviders.of(this)[LoginViewModel::class.java]
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onBackPressed() {
        if (loginViewModel.isRegisterVisible) loginViewModel.onBackPressed.onNext(true)
        else super.onBackPressed()
    }
}
