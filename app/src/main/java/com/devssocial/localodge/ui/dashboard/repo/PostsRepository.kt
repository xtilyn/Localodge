package com.devssocial.localodge.ui.dashboard.repo

import android.content.Context
import android.location.Location
import android.util.Log
import com.androidhuman.rxfirebase2.database.RxFirebaseDatabase
import com.androidhuman.rxfirebase2.firestore.RxFirebaseFirestore
import com.devssocial.localodge.*
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.*
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.utils.providers.FirebasePathProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.extension.setLocation
import java.io.File
import java.io.FileInputStream


class PostsRepository(context: Context) {

    companion object {
        private const val TAG = "PostsRepository"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDatabase = FirebaseDatabase.getInstance()
    private val geoFirestore = GeoFirestore(firestore.collection(COLLECTION_POSTS))
    private var bucket2 = FirebaseStorage.getInstance(FirebasePathProvider.getSecondBucketPath())

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

    fun loadFeed(startAfter: Timestamp?, limit: Long?): Single<ArrayList<PostViewItem>> {
        val userId = userRepo.getCurrentUserId() ?: return Single.just(arrayListOf())
        var ref = firestore
            .collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_FEED)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)

        if (startAfter != null) ref = ref.startAfter(startAfter)
        if (limit != null) ref = ref.limit(limit)

        return RxFirebaseFirestore.data(ref)
            .flatMap {
                val result = it.value().documents.filterNotNull()
                    .map { documentSnapshot: DocumentSnapshot ->
                        documentSnapshot.toObject(Post::class.java)
                    }
                val getUserDocSingles = result.filterNotNull()
                    .map { post ->
                        userRepo.getUserData(post.posterUserId)
                            .onErrorReturnItem(User())
                    }

                val finalResult = arrayListOf<PostViewItem>()
                return@flatMap Single.merge(getUserDocSingles).toList(it.value().size())
                    .subscribeOn(Schedulers.io())
                    .flatMap { users: MutableList<User> ->
                        users.forEachIndexed { index, user ->
                            val postViewItem = result[index]!!.mapProperties(PostViewItem())
                            postViewItem.posterProfilePic = user.profilePicUrl
                            postViewItem.posterUsername = user.username
                            finalResult.add(postViewItem)
                        }
                        Single.just(finalResult)
                    }
            }
            .onErrorResumeNext {
                if (it.message == NO_VALUE) return@onErrorResumeNext Single.just(arrayListOf())
                else Single.error(it)
            }
    }

    fun updateLikes(postId: String, newLikes: HashMap<String, Boolean>): Completable {
        val ref = firestore
            .collection(COLLECTION_POSTS)
            .document(postId)

        return RxFirebaseFirestore.update(
            ref,
            mapOf<String, HashMap<String, Boolean>>(FIELD_LIKES to newLikes)
        )
    }

    fun getPostStats(postId: String): Single<PostStatistics> {
        val dbRef = realtimeDatabase.getReference(FirebasePathProvider.getPostStatisticsPath(postId))
        return RxFirebaseDatabase.data(dbRef.child("statistics")).flatMap {
            if (it.exists()) {
                val postStats: PostStatistics = it.getValue(PostStatistics::class.java)!!
                Single.just(postStats)
            }
            else Single.just(PostStatistics())
        }
    }

    fun getPostDetail(postId: String): Single<PostViewItem> {
        val rootRef = firestore
            .collection(COLLECTION_POSTS)
            .document(postId)
        val getStatsSingle = getPostStats(postId)
        val getRoot = RxFirebaseFirestore.data(rootRef)
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
        return Single.zip(
            getStatsSingle,
            getRoot,
            BiFunction<PostStatistics, PostViewItem, PostViewItem> { stats, postViewItem ->
                postViewItem.commentsCount = stats.commentsCount
                postViewItem
            }
        )
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
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)

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

    fun postComment(postId: String, comment: String, photoUrl: String?): Completable {
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
            .andThen(
                if (photoUrl != null) {
                    Single.create<String> { emitter ->
                        // upload photoUrl to storage
                        val storageRef = bucket2.reference.child(
                            FirebasePathProvider.getCommentsMediaPath(postId, ref.id)
                        )
                        val fileInputStream = FileInputStream(File(photoUrl))
                        val uploadTask = storageRef.putBytes(fileInputStream.readBytes())
                        try {
                            Tasks.await(uploadTask)
                            fileInputStream.close()
                            val downloadUrl = Tasks.await(storageRef.downloadUrl)
                            emitter.onSuccess(downloadUrl.toString())
                        } catch (e: Exception) {
                            emitter.onError(e)
                        }
                    }
                        .subscribeOn(Schedulers.io())
                        .flatMapCompletable { downloadUrl ->
                            return@flatMapCompletable RxFirebaseFirestore.update(
                                ref, mapOf("photoUrl" to downloadUrl)
                            ).subscribeOn(Schedulers.io())
                        }
                } else {
                    Completable.complete()
                }
            )
    }

    fun createPost(post: Post): Single<String> {
        val userId = userRepo.getCurrentUserId() ?: return Single.error(Throwable(NO_VALUE))
        val ref = firestore.collection(COLLECTION_POSTS).document()
        val geoFirestore = GeoFirestore(firestore.collection(COLLECTION_POSTS))

        var uploadTask: UploadTask? = null
        var fileInputStream: FileInputStream? = null
        val storageRef = bucket2.reference.child(FirebasePathProvider.getPostsMediaPath(ref.id))
        if (post.photoUrl != null) {
            fileInputStream = FileInputStream(File(post.photoUrl!!))
            uploadTask = storageRef.putBytes(fileInputStream.readBytes())
        } else if (post.videoUrl != null) {
            fileInputStream = FileInputStream(File(post.videoUrl!!))
            uploadTask = storageRef.putBytes(fileInputStream.readBytes())
        }

        post.apply {
            posterUserId = userId
            objectID = ref.id
        }

        val uploadTaskCompletable: Completable = if (uploadTask != null) {
            Completable.create {
                try {
                    Tasks.await(uploadTask)
                    fileInputStream?.close()
                    it.onComplete()
                } catch (e: Exception) {
                    it.onError(e)
                }
            }.andThen(SingleSource<String> { singleObserver ->
                Log.d(TAG, "UPLOADING TO STORAGE...: ${Thread.currentThread()}")
                try {
                    val downloadUrl = Tasks.await(storageRef.downloadUrl)
                    singleObserver.onSuccess(downloadUrl.toString())
                } catch (e: Exception) {
                    singleObserver.onError(e)
                }
            }).flatMapCompletable { downloadUrl ->
                Log.d(TAG, "GOT DOWNLOAD URL: ${Thread.currentThread()}")
                return@flatMapCompletable RxFirebaseFirestore.update(
                    ref,
                    if (post.photoUrl != null) mapOf("photoUrl" to downloadUrl)
                    else mapOf("videoUrl" to downloadUrl)
                ).subscribeOn(Schedulers.io())
            }
                .subscribeOn(Schedulers.io())
        } else Completable.complete()


        return Completable.mergeArray(
            uploadTaskCompletable,
            RxFirebaseFirestore.set(ref, post).subscribeOn(Schedulers.io())
        )
            .andThen { completableObserver ->
                Log.d(TAG, "SETTING LOCATION: ${Thread.currentThread()}")
                geoFirestore.setLocation(
                    ref.id,
                    GeoPoint(post._geoloc.lat, post._geoloc.lng)
                ) { exception ->
                    if (exception != null) completableObserver.onError(Throwable(exception.message))
                    else completableObserver.onComplete()
                }
            }
            .toSingle {
                Log.d(TAG, "CONVERTING TO SINGLE: ${Thread.currentThread()}")
                return@toSingle ref.id
            }
    }
}