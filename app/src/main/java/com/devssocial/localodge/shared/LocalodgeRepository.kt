package com.devssocial.localodge.shared

import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.COLLECTION_FEEDBACK
import com.devssocial.localodge.models.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable

class LocalodgeRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun sendFeedback(userId: String, feedback: Feedback): Completable {
        val ref = firestore
            .collection(COLLECTION_FEEDBACK)
            .document(userId)

        return RxFirebaseFirestore.set(ref, feedback)
    }

}