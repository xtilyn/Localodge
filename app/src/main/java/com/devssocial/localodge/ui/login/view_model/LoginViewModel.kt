package com.devssocial.localodge.ui.login.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.devssocial.localodge.ERROR_EMAIL_ALREADY_IN_USE
import com.devssocial.localodge.ERROR_USER_DISABLED
import com.devssocial.localodge.ERROR_USER_NOT_FOUND
import com.devssocial.localodge.R
import com.devssocial.localodge.ui.login.repo.LoginRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

class LoginViewModel (application: Application) : AndroidViewModel(application) {

    private val repo = LoginRepository()
    private val context = application.baseContext

    var isRegisterVisible = false
    var onBackPressed = BehaviorSubject.create<Boolean>()

    fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Single<FirebaseUser> = repo.signInWithEmailAndPassword(email, password)

    fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is FirebaseAuthInvalidCredentialsException -> {
                context.resources.getString(R.string.invalid_credentials)
            }
            is FirebaseAuthInvalidUserException -> {
                when (error.errorCode) {
                    ERROR_USER_DISABLED -> {
                        context.resources.getString(R.string.user_disabled_error)
                    }
                    ERROR_USER_NOT_FOUND -> {
                        context.resources.getString(R.string.user_not_found_error)
                    }
                    else -> {
                        context.resources.getString(R.string.generic_error_message)
                    }
                }
            }
            is FirebaseAuthUserCollisionException -> {
                when (error.errorCode) {
                    ERROR_EMAIL_ALREADY_IN_USE -> {
                        context.resources.getString(R.string.email_already_in_use_error)
                    }
                    else -> {
                        context.resources.getString(R.string.generic_error_message)
                    }
                }
            }
            else -> {
                context.resources.getString(R.string.generic_error_message)
            }
        }
    }

    fun createUserWithEmailAndPassword(
        email: String,
        username: String,
        password: String
    ): Completable = repo.createUserWithEmailAndPassword(email, username, password)

    fun signOut() = repo.signOut()

    fun sendEmailVerification(): Completable = repo.sendEmailVerification()

    fun sendPasswordResetEmail(email: String): Completable = repo.sendPasswordResetEmail(email)
}