package com.devssocial.localodge.ui.dashboard.repo

import android.content.Context
import android.location.Location
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.*
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.shared.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.imperiumlabs.geofirestore.GeoFirestore
class PostsRepository(context: Context) {

    companion object {
        private const val TAG = "PostsRepository"
    }

    val postsDao = LocalodgeRoomDatabase.getDatabase(context).postDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val geoFirestore = GeoFirestore(firestore.collection(COLLECTION_POSTS))

    private val userRepo = UserRepository(context)

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

    fun getPostDetail(postId: String): Single<PostViewItem> {
        val rootRef = firestore
            .collection(COLLECTION_POSTS)
            .document(postId)
        val getPostDetail = RxFirebaseFirestore.data(rootRef)
            .flatMap {
                val post = it.value().toObject(Post::class.java)
                val postViewItem = post!!.mapProperties(PostViewItem())

                userRepo.getUserData(post.posterUserId)
                    .flatMap { user ->
                        postViewItem.posterProfilePic = user.profilePicUrl
                        postViewItem.posterUsername = user.username
                        Single.just(postViewItem)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())

            }

        val commentsRef = firestore
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_COMMENTS)
        val getComments = RxFirebaseFirestore.data(commentsRef)
            .flatMap {
                Single.just(
                    it.value().documents.map { doc ->
                        doc.id
                    }.toHashSet()
                )
            }
            .onErrorResumeNext {
                if (it.message == NO_VALUE) return@onErrorResumeNext Single.just(hashSetOf())
                else Single.error(it)
            }

        return Single.zip(
            getPostDetail,
            getComments,
            BiFunction { postViewItem, comments ->
                postViewItem.comments = comments
                postViewItem
            }
        )
    }
}