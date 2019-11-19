package com.devssocial.localodge.shared

import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.*
import com.devssocial.localodge.models.Feedback
import com.devssocial.localodge.models.Report
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

    fun sendUserReport(
        userIdToReport: String,
        report: Report
    ): Completable {
        val ref = firestore
            .collection(COLLECTION_USERS)
            .document(userIdToReport)
            .collection(COLLECTION_REPORTS)
            .document(report.reportedByUserId)

        return RxFirebaseFirestore.set(ref, report)
    }

    fun sendPostReport(
        postIdToReport: String,
        report: Report
    ): Completable {
        val ref = firestore
            .collection(COLLECTION_POSTS)
            .document(postIdToReport)
            .collection(COLLECTION_REPORTS)
            .document(report.reportedByUserId)

        return RxFirebaseFirestore.set(ref, report)
    }

}