package com.devssocial.localodge.ui.dashboard.repo

import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.NO_VALUE
import com.devssocial.localodge.USERS
import com.devssocial.localodge.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Single

class DashboardRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = mAuth.currentUser

    fun getUserData(userId: String): Single<User> {
        val ref = firestore
            .collection(USERS)
            .document(userId)

        return RxFirebaseFirestore.data(ref)
            .flatMap {
                if (it.value().exists()) {
                    Single.just(it.value().toObject(User::class.java))
                } else {
                    Single.error(Exception(NO_VALUE))
                }
            }
    }
}