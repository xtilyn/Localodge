package com.devssocial.localodge.ui.login.repo

import com.androidhuman.rxfirebase2.auth.RxFirebaseAuth
import com.androidhuman.rxfirebase2.auth.RxFirebaseUser
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.USERS
import com.devssocial.localodge.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable
import io.reactivex.Single

class LoginRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    fun signOut() {
        mAuth.signOut()
    }

    fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Single<FirebaseUser> {
        return RxFirebaseAuth.signInWithEmailAndPassword(mAuth, email, password)
    }

    fun createUserWithEmailAndPassword(
        email: String,
        username: String,
        password: String
    ): Completable {
        return RxFirebaseAuth
            .createUserWithEmailAndPassword(mAuth, email, password)
            .flatMapCompletable { user: FirebaseUser ->
                return@flatMapCompletable createNewUserDocument(
                    userId = user.uid,
                    username = username,
                    email = email
                )
            }
    }

    fun sendEmailVerification(): Completable {
        return RxFirebaseUser.sendEmailVerification(mAuth.currentUser!!)
    }

    private fun createNewUserDocument(
        userId: String,
        username: String,
        email: String
    ): Completable {
        val ref = firestore
            .collection(USERS)
            .document(userId)

        val newUserDoc = User(
            userId = userId,
            username = username,
            email = email
        )
        return RxFirebaseFirestore.set(ref, newUserDoc)
    }
}