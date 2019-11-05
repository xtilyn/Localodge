package com.devssocial.localodge.ui.login.ui


import android.content.Context
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.devssocial.localodge.LOCALODGE_SHARED_PREF
import com.devssocial.localodge.R
import com.devssocial.localodge.TRIAL_ACCOUNT_REQUESTED
import com.devssocial.localodge.extensions.isEmail
import com.devssocial.localodge.ui.login.view_model.LoginViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.google.firebase.auth.FirebaseUser
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_sign_in.*

class SignInFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private val disposables = CompositeDisposable()

    private val signInFragmentClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.register_button -> {
                toggleRegister(true)
            }
            R.id.back_button -> {
                toggleRegister(false)
            }
            R.id.continue_button -> {
                val sharedPref = activity?.getSharedPreferences(LOCALODGE_SHARED_PREF, Context.MODE_PRIVATE) ?: return@OnClickListener
                sharedPref.edit {
                    this.putBoolean(TRIAL_ACCOUNT_REQUESTED, true)
                    this.commit()
                }
                goToDashboard()
            }
            R.id.sign_in_button -> {
                if (loginViewModel.isRegisterVisible) {
                    createAnAccount()
                } else {
                    signInRequested()
                }
            }
            R.id.forgot_password -> {
               findNavController().navigate(R.id.action_signInFragment_to_forgotPasswordFragment)
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
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup widgets
        register_button?.setOnClickListener(signInFragmentClickListener)
        sign_in_button?.setOnClickListener(signInFragmentClickListener)
        continue_button?.setOnClickListener(signInFragmentClickListener)
        forgot_password?.setOnClickListener(signInFragmentClickListener)
    }

    override fun onStart() {
        super.onStart()

        // observe activity's onBackPressedEvent
        disposables.add(
            loginViewModel
                .onBackPressed
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    toggleRegister(false)
                }
        )
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    private fun toggleRegister(show: Boolean) {
        if (context == null) return
        // boolean flag used by activity
        loginViewModel.isRegisterVisible = show

        if (show) {
            sign_in_root_layout?.background = ContextCompat.getDrawable(context!!, R.drawable.city)
            sign_in_text_view?.text = resources.getString(R.string.register)
            subtitle_text_view?.visibility = View.GONE
            localodge_title_text_view?.text = resources.getString(R.string.create_an_account)
            back_button?.setOnClickListener(signInFragmentClickListener)
            updateConstraints(R.layout.fragment_sign_in_alt)
        }
        else {
            sign_in_root_layout?.background = ContextCompat.getDrawable(context!!, R.drawable.city)
            localodge_title_text_view?.text = resources.getString(R.string.localodge)
            subtitle_text_view?.visibility = View.VISIBLE
            sign_in_text_view?.text = resources.getString(R.string.sign_in)
            updateConstraints(R.layout.fragment_sign_in)
        }

        sign_in_button?.setOnClickListener(signInFragmentClickListener)
    }

    private fun updateConstraints(@LayoutRes id: Int) {
        val newConstraintSet = ConstraintSet()
        newConstraintSet.clone(context, id)
        newConstraintSet.applyTo(sign_in_root_layout)
        val transition = ChangeBounds()
        transition.interpolator = OvershootInterpolator()
        TransitionManager.beginDelayedTransition(sign_in_root_layout, transition)
    }

    private fun createAccountSuccessful() {
        loginViewModel.signOut()
        updateConstraints(R.layout.fragment_sign_in_alt_3)
        sign_in_root_layout?.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
        sign_in_button?.setOnClickListener(null)
        sign_in_text_view?.text = resources.getString(R.string.email_confirmation_sent)
    }

    private fun goToDashboard() {
        updateConstraints(R.layout.fragment_sign_in_alt_3)
        sign_in_root_layout?.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
        sign_in_text_view?.text = ""
        sign_in_button?.setOnClickListener(null)
        ActivityLaunchHelper.goToDashboard(activity!!)
    }

    private fun signInRequested() {
        val email = email_edit_text.text.toString().trim()
        val pwd = password_edit_text.text.toString()

        if (validateInputs(email, pwd)) signInUser(email, pwd)
    }

    private fun validateInputs(
        email: String,
        password: String,
        username: String? = null
    ): Boolean {
        return when {
            email.isEmpty() -> {
                showError(resources.getString(R.string.email_required))
                false
            }
            password.isEmpty() -> {
                showError(resources.getString(R.string.password_required))
                false
            }
            !email.isEmail -> {
                showError(resources.getString(R.string.invalid_email))
                false
            }
            password.length < 4 -> {
                showError(resources.getString(R.string.invalid_password))
                false
            }
            username != null -> {
                when {
                    username.isEmpty() -> {
                        showError(resources.getString(R.string.username_required))
                        false
                    }
                    username.length < 4 -> {
                        showError(resources.getString(R.string.username_too_short))
                        false
                    }
                    else -> {
                        true
                    }
                }
            }
            else -> true
        }
    }

    private fun signInUser(email: String, password: String) {
        showProgress(true)
        disposables.add(
            loginViewModel.signInWithEmailAndPassword(email, password)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { error ->
                        handleError(error)
                    },
                    onSuccess = { user: FirebaseUser ->
                        if (!user.isEmailVerified) {
                            loginViewModel.signOut()
                            Toasty.warning(
                                context!!,
                                resources.getString(R.string.email_not_verified),
                                Toast.LENGTH_SHORT,
                                true
                            ).show()
                            showProgress(false)
                        } else {
                            goToDashboard()
                        }
                    }
                )
        )
    }

    private fun createAnAccount() {
        val email = email_edit_text.text.toString().trim()
        val username = username_edit_text.text.toString().trim()
        val pwd = password_edit_text.text.toString()

        if (!validateInputs(email, pwd, username)) return
        showProgress(true)
        disposables.add(
            loginViewModel
                .createUserWithEmailAndPassword(email, username, pwd)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { error ->
                        handleError(error)
                    },
                    onComplete = {
                        sendEmailVerification()
                    }
                )
        )
    }

    private fun sendEmailVerification() {
        disposables.add(
            loginViewModel.sendEmailVerification()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { error ->
                        handleError(error)
                    },
                    onComplete = {
                        showProgress(false)
                        createAccountSuccessful()
                    }
                )
        )
    }

    private fun showError(message: String) {
        Toasty.error(context!!, message, Toast.LENGTH_SHORT, true).show()
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            loading_overlay.visibility = View.VISIBLE
        } else {
            loading_overlay.visibility = View.GONE
        }
    }

    private fun handleError(error: Throwable) {
        Log.e(this::class.java.simpleName, error.message!!, error)
        val message = loginViewModel.getErrorMessage(error)
        showError(message)
        showProgress(false)
        loginViewModel.signOut()
    }
}
