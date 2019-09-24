package com.devssocial.localodge.shared

import android.content.Context
import android.content.Intent
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.NO_VALUE
import com.devssocial.localodge.USERS
import com.devssocial.localodge.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Single

class UserRepository(private val context: Context) {

    companion object {
        const val AUTH_BROADCAST = "com.devssocial.localodge.auth_broadcast"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? {
        val user = mAuth.currentUser
        if (user == null) {
            // send broadcast to LocalodgeActivity
            context.sendBroadcast(
                Intent().apply {
                    this.action = AUTH_BROADCAST
                }
            )
        }
        return user
    }

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