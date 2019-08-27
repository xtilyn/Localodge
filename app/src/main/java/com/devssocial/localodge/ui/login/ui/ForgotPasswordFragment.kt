package com.devssocial.localodge.ui.login.ui


import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.ViewModelProviders

import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.isEmail
import com.devssocial.localodge.ui.login.view_model.LoginViewModel
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_forgot_password.*

class ForgotPasswordFragment : Fragment() {

    private val disposables = CompositeDisposable()
    private lateinit var loginViewModel: LoginViewModel

    private val clickListener = View.OnClickListener {  view ->
        when (view.id) {
            R.id.back_button -> {
                activity?.onBackPressed()
            }
            R.id.send_email_verification_button -> {
                val email = email_edit_text?.text.toString().trim()
                when {
                    !email.isEmail -> showError(resources.getString(R.string.invalid_email))
                    email.isEmpty() -> showError(resources.getString(R.string.email_required))
                    else -> sendPasswordResetEmail(email)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginViewModel = ViewModelProviders.of(activity!!)[LoginViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup widgets
        back_button?.setOnClickListener(clickListener)
        send_email_verification_button?.setOnClickListener(clickListener)
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    private fun sendPasswordResetEmail(email: String) {
        showProgress(true)
        disposables.add(
            loginViewModel.sendPasswordResetEmail(email)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        Log.e(this::javaClass.name, it.message!!, it)
                        if (it is FirebaseAuthInvalidUserException) {
                            onPasswordResetSent()
                            return@subscribeBy
                        }
                        showError(resources.getString(R.string.password_reset_failed))
                        showProgress(false)
                    },
                    onComplete = {
                        onPasswordResetSent()
                        showProgress(false)
                    }
                )
        )
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            loading_overlay.visibility = View.VISIBLE
        } else {
            loading_overlay.visibility = View.GONE
        }
    }

    private fun onPasswordResetSent() {
        updateConstraints(R.layout.fragment_forgot_password_alt)
        enter_email_text?.text = resources.getString(R.string.password_reset_sent)
    }

    private fun updateConstraints(@LayoutRes id: Int) {
        val newConstraintSet = ConstraintSet()
        newConstraintSet.clone(context, id)
        newConstraintSet.applyTo(forgot_password_root_layout)
        val transition = ChangeBounds()
        transition.interpolator = OvershootInterpolator()
        TransitionManager.beginDelayedTransition(forgot_password_root_layout, transition)
    }

    private fun showError(message: String) {
        Toasty.error(
            context!!,
            message,
            Toast.LENGTH_SHORT, true
        ).show()
    }

}
