package com.devssocial.localodge.shared

import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.COLLECTION_BLACKLIST
import com.devssocial.localodge.COLLECTION_FEEDBACK
import com.devssocial.localodge.models.Feedback
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable
import io.reactivex.Single

class LocalodgeRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun sendFeedback(userId: String, feedback: Feedback): Completable {
        val ref = firestore
            .collection(COLLECTION_FEEDBACK)
            .document(userId)

        return RxFirebaseFirestore.set(ref, feedback)
    }

    fun getBlacklist(): Single<HashSet<String>> {
        val ref = firestore
            .collection(COLLECTION_BLACKLIST)

        return RxFirebaseFirestore.data(ref)
            .flatMap { querySnap ->
                val result = hashSetOf<String>()
                result.addAll(
                    querySnap.value().documents.map { it.id }
                )
                Single.just(result)
            }
    }
}