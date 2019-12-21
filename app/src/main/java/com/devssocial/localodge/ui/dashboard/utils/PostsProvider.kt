package com.devssocial.localodge.ui.dashboard.utils

import android.location.Location
import android.util.Log
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.models.PostStatistics
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.models.User
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.ui.dashboard.repo.PostsRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.imperiumlabs.geofirestore.GeoFirestore

class PostsProvider(
    private val disposables: CompositeDisposable,
    private val repo: PostsRepository,
    private val userRepo: UserRepository
) {

    companion object {
        private const val TAG = "PostsProvider"
        const val INITIAL_RADIUS = 50.0
        const val RADIUS_INCREMENT = 50.0
    }

    fun loadInitial(
        startAfter: Timestamp?,
        limit: Long?,
        blockedUsers: HashSet<String>,
        blockedPosts: HashSet<String>
    ): Single<ArrayList<PostViewItem>> {
        return repo.loadFeed(startAfter = startAfter, limit = limit)
            .flatMap {
                val iterator = it.iterator()
                while (iterator.hasNext()) {
                    val current = iterator.next()
                    if (blockedUsers.contains(current.posterUserId) ||
                        blockedPosts.contains(current.objectID)) {
                        iterator.remove()
                    }
                }
                return@flatMap Single.just(it)
            }
    }

    fun loadInitialWithLocation(
        userLocation: Location,
        radius: Double,
        blockedUsers: HashSet<String>,
        blockedPosts: HashSet<String>,
        onError: (Exception) -> Unit,
        onSuccess: (ArrayList<PostViewItem>) -> Unit
    ) {
        repo.loadDataAroundLocation(
            userLocation,
            radius,
            object : GeoFirestore.SingleGeoQueryDataEventCallback {
                override fun onComplete(
                    documentSnapshots: List<DocumentSnapshot>?,
                    exception: Exception?
                ) {
                    if (exception != null) {
                        onError(exception)
                    } else {

                        val unorderedPosts = documentSnapshots?.map {
                            it.toObject(Post::class.java)!!
                        }

                        if (unorderedPosts.isNullOrEmpty()) {
                            onSuccess(arrayListOf())
                            return
                        }

                        disposables.add(
                            Single.create<ArrayList<PostViewItem>> { e ->
                                try {
                                    val orderedPosts =
                                        PostsUtil.orderPosts(unorderedPosts).map { post: Post ->
                                            post.mapProperties(PostViewItem())
                                        } as ArrayList<PostViewItem>
                                    e.onSuccess(orderedPosts)
                                } catch (exception: Exception) {
                                    e.onError(exception)
                                }
                            }
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.computation())
                                .subscribeBy(
                                    onError = {
                                        Log.e(TAG, it.message, it)
                                    },
                                    onSuccess = { orderedPosts ->
                                        val combinedSingles = orderedPosts.map {
                                            Single.zip(
                                                userRepo.getUserData(it.posterUserId).onErrorReturnItem(User()),
                                                repo.getPostStats(it.objectID),
                                                BiFunction<User, PostStatistics, Pair<User, PostStatistics>> {
                                                    user, stats ->
                                                    Pair(user, stats)
                                                }
                                            )

                                        }

                                        disposables.add(
                                            Single.merge(combinedSingles)
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribeOn(Schedulers.io())
                                                .subscribeBy(
                                                    onError = {
                                                        Log.e(TAG, it.message, it)
                                                    },
                                                    onNext = { result: Pair<User, PostStatistics> ->
                                                        val user = result.first
                                                        val stats = result.second
                                                        orderedPosts.forEach { postViewItem: PostViewItem ->
                                                            if (postViewItem.posterUserId == user.userId) {
                                                                postViewItem.posterUsername = user.username
                                                                postViewItem.posterProfilePic = user.profilePicUrl
                                                            }
                                                            postViewItem.commentsCount = stats.commentsCount
                                                        }
                                                    },
                                                    onComplete = {
                                                        // remove empty users
                                                        // remove blocked users
                                                        // remove blocked posts
                                                        val iterator = orderedPosts.iterator()
                                                        while (iterator.hasNext()) {
                                                            val curr = iterator.next()
                                                            if (curr.posterUsername.isEmpty() ||
                                                                blockedUsers.contains(curr.posterUserId) ||
                                                                blockedPosts.contains(curr.objectID)) {
                                                                iterator.remove()
                                                            }
                                                        }
                                                        onSuccess(orderedPosts)
                                                    }
                                                )
                                        )
                                    }
                                )
                        )

                    }
                }
            })
    }

}