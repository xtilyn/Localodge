package com.devssocial.localodge.shared

import android.content.Context
import android.content.Intent
import android.util.Log
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.NO_VALUE
import com.devssocial.localodge.COLLECTION_USERS
import com.devssocial.localodge.daos.UserDao_Impl
import com.devssocial.localodge.models.User
import com.devssocial.localodge.utils.FirebasePathProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import java.io.FileInputStream

class UserRepository(private val context: Context) {

    companion object {
        private const val TAG = "UserRepository"
        const val AUTH_BROADCAST = "com.devssocial.localodge.auth_broadcast"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private var storage = FirebaseStorage.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    // TODO CONTINUE HERE INITIALIZE ROOM USER DAO

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

    fun getCurrentUserId(): String? = getCurrentUser()?.uid

    fun getUserData(userId: String): Single<User> {
        val ref = firestore
            .collection(COLLECTION_USERS)
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

    fun updateProfilePicInStorage(path: String): Observable<Pair<Double, String>> {
        val userId = getCurrentUserId() ?: return Observable.just(Pair(0.0, "error"))
        val storageRef = storage.reference.child(FirebasePathProvider.getProfilePicPath(userId))
        val stream = FileInputStream(File(path))
        return Observable.create { emitter ->
            val uploadTask = storageRef.putStream(stream)
            uploadTask
                .addOnProgressListener {
                    Log.d(TAG, "bytes transfered ${it.bytesTransferred}")
                    Log.d(TAG, "bytes totalByteCount ${it.totalByteCount}")
                    val percentage =
                        (100.0 * it.bytesTransferred.toDouble()) / it.totalByteCount.toDouble()
                    if (percentage <= 99 && percentage > 0) {
                        emitter.onNext(Pair(percentage, ""))
                    }
                }
                .addOnCompleteListener { result ->
                    result.exception?.let {
                        emitter.onError(it)
                    }
                }.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            emitter.onError(it)
                        }
                    }
                    return@continueWithTask storageRef.downloadUrl
                }.addOnCompleteListener {
                    emitter.onNext(Pair(100.0, it.result.toString()))
                    emitter.onComplete()
                }
            emitter.setCancellable { uploadTask.cancel() }
        }
    }

    fun updateProfilePicInFirestore(url: String): Completable {
        val userId = getCurrentUserId() ?: return Completable.complete()
        val ref = firestore
            .collection(COLLECTION_USERS)
            .document(userId)

        return RxFirebaseFirestore.update(ref, mapOf("profilePicUrl" to url))
    }

}