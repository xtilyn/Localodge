package com.devssocial.localodge.ui.dashboard.repo

import android.content.Context
import android.location.Location
import android.util.Log
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.*
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.*
import com.devssocial.localodge.shared.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.extension.setLocation

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
        return RxFirebaseFirestore.data(rootRef)
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
    }

    fun getComments(
        postId: String,
        limit: Long?,
        startAfter: Timestamp?
    ): Single<ArrayList<CommentViewItem>> {
        var commentsRef = firestore
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_COMMENTS)
            .orderBy(FIELD_TIMESTAMP)

        if (startAfter != null) commentsRef = commentsRef.startAfter(startAfter)
        if (limit != null) commentsRef = commentsRef.limit(limit)

        return RxFirebaseFirestore.data(commentsRef)
            .flatMap {
                val comments = it.value().documents.map { doc ->
                    doc.toObject(Comment::class.java)!!.mapProperties(CommentViewItem())
                }
                val getCommentPosterDetails = comments.map { c ->
                    userRepo.getUserData(c.postedBy)
                }
                Single.zip(getCommentPosterDetails) { res ->
                    val commenters = res.map { user -> user as User }
                    for (i in comments.indices) {
                        comments[i].postedByProfilePic = commenters[i].profilePicUrl
                        comments[i].postedByUsername = commenters[i].username
                    }
                    comments as ArrayList
                }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
            }
            .onErrorResumeNext {
                if (it.message == NO_VALUE) return@onErrorResumeNext Single.just(arrayListOf())
                else Single.error(it)
            }
    }

    fun postComment(postId: String, comment: String): Completable {
        val userId = userRepo.getCurrentUserId() ?: return Completable.complete()
        val ref = firestore
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_COMMENTS)
            .document()

        val commentObj = Comment(
            objectID = ref.id,
            postedBy = userId,
            body = comment
        )
        return RxFirebaseFirestore.set(ref, commentObj)
    }

    fun createPost(post: Post): Single<String> {
        val userId = userRepo.getCurrentUserId() ?: return Single.just("")
        val ref = firestore.collection(COLLECTION_POSTS).document()
        val geoFirestore = GeoFirestore(firestore.collection(COLLECTION_POSTS))
        post.apply {
            posterUserId = userId
        }

        return RxFirebaseFirestore.set(ref, post)
            .andThen(SingleSource<String> { observer ->
                geoFirestore.setLocation(ref.id, GeoPoint(post._geoloc.lat, post._geoloc.lng)) { exception ->
                    if (exception != null) observer.onError(Throwable(exception.message))
                    else observer.onSuccess(ref.id)
                }
            })
    }
}