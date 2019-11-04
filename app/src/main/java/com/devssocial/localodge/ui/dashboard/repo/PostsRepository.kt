package com.devssocial.localodge.ui.dashboard.repo

import android.content.Context
import android.location.Location
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.COLLECTION_POSTS
import com.devssocial.localodge.FIELD_LIKES
import com.devssocial.localodge.LocalodgeRoomDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import io.reactivex.Completable
import org.imperiumlabs.geofirestore.GeoFirestore
class PostsRepository(context: Context) {

    companion object {
        private const val TAG = "PostsRepository"
    }

    val postsDao = LocalodgeRoomDatabase.getDatabase(context).postDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val geoFirestore = GeoFirestore(firestore.collection(COLLECTION_POSTS))

    fun loadDataAroundLocation(
        userLocation: Location,
        radius: Double,
        callback: GeoFirestore.SingleGeoQueryDataEventCallback
    ) {
        geoFirestore.getAtLocation(
            GeoPoint(userLocation.latitude, userLocation.longitude),
            radius,
            callback
        )
    }

    fun updateLikes(postId: String, newLikes: HashSet<String>): Completable {
        val ref = firestore
            .collection(COLLECTION_POSTS)
            .document(postId)

        return RxFirebaseFirestore.update(ref, mapOf<String, Set<String>>(FIELD_LIKES to newLikes))
    }
}