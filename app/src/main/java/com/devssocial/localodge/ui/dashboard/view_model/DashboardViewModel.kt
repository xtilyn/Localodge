package com.devssocial.localodge.ui.dashboard.view_model

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.models.User
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.ui.dashboard.repo.DashboardRepository
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.imperiumlabs.geofirestore.GeoFirestore

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // repositories
    private val context = application.baseContext
    private val repo = DashboardRepository()
    private val userRepo = UserRepository(context)

    var onBackPressed = BehaviorSubject.create<Boolean>()
    var isDrawerOpen = false

    fun getCurrentUser(): FirebaseUser? = userRepo.getCurrentUser()

    fun getUserData(userId: String): Single<User> = userRepo.getUserData(userId)

    fun loadDataAroundLocation(
        userLocation: Location,
        callback: GeoFirestore.SingleGeoQueryDataEventCallback
    ) {
        repo.loadDataAroundLocation(userLocation, callback)
    }
}