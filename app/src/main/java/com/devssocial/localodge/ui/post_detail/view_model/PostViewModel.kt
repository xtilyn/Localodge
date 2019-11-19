package com.devssocial.localodge.ui.post_detail.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.ui.dashboard.repo.PostsRepository

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.baseContext
    val postsRepo = PostsRepository(context)
    val userRepo = UserRepository(context)

}