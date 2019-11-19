package com.devssocial.localodge.ui.dashboard.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.devssocial.localodge.models.Notification
import com.devssocial.localodge.shared.LocalodgeRepository
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.ui.dashboard.repo.PostsRepository
import io.reactivex.subjects.BehaviorSubject

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // repositories
    private val context = application.baseContext
    val postsRepo = PostsRepository(context)
    val userRepo = UserRepository(context)
    val localodgeRepo = LocalodgeRepository()

    var onBackPressed = BehaviorSubject.create<Boolean>()
    var isDrawerOpen = false
    lateinit var unreadNotifications: ArrayList<Notification>

    fun isUserLoggedIn(): Boolean = userRepo.getCurrentUser() != null
}