package com.devssocial.localodge.ui.dashboard.repo

import android.location.Location
import com.devssocial.localodge.POSTS
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.imperiumlabs.geofirestore.GeoFirestore
class DashboardRepository {

    companion object {
        private const val TAG = "DashboardRepo"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val geoFirestore = GeoFirestore(firestore.collection(POSTS))

    fun loadDataAroundLocation(
        userLocation: Location,
        callback: GeoFirestore.SingleGeoQueryDataEventCallback
    ) {
        geoFirestore.getAtLocation(
            GeoPoint(userLocation.latitude, userLocation.longitude),
            100.0,
            callback
        )
    }
}