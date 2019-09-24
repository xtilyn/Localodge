package com.devssocial.localodge.ui.dashboard.repo

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.CITIES
import com.devssocial.localodge.models.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.reactivex.Single
import java.util.*
import kotlin.collections.ArrayList

class DashboardRepository {

    companion object {
        private const val TAG = "DashboardRepo"
    }

    private val firestore = FirebaseFirestore.getInstance()

    fun loadDataAroundLocation(userLocation: Location, context: Context): Single<ArrayList<Post>> {
        val gcd = Geocoder(context, Locale.getDefault())
        val addresses = gcd.getFromLocation(userLocation.latitude, userLocation.longitude, 1)
        lateinit var cityName: String
        if (addresses.size > 0) {
            cityName = addresses[0].locality
            Log.d(TAG, "Found locale: $cityName")
        } else {
            return Single.error(Throwable("Unknown locale."))
        }

        val ref = firestore
            .collection(CITIES)
            .orderBy("promotedRating", Query.Direction.DESCENDING)
            .orderBy("createdDate", Query.Direction.DESCENDING)
        // TODO CONTINUE HERE GEOLOC QUERY

        return RxFirebaseFirestore.data(ref)
            .flatMap {
                Single.just(
                    it.value().documents.map { doc ->
                        doc.toObject(Post::class.java) ?: Post()
                    } as ArrayList<Post>
                )
            }
    }
}