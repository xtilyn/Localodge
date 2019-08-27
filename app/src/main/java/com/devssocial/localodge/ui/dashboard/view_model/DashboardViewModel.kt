package com.devssocial.localodge.ui.dashboard.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.devssocial.localodge.models.User
import com.devssocial.localodge.ui.dashboard.repo.DashboardRepository
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DashboardRepository()

    var onBackPressed = BehaviorSubject.create<Boolean>()
    var isDrawerOpen = false

    fun getCurrentUser(): FirebaseUser? = repo.getCurrentUser()

    fun getUserData(userId: String): Single<User> = repo.getUserData(userId)
}