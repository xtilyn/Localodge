package com.devssocial.localodge.ui.dashboard.utils

import android.content.Context
import android.location.Location
import android.util.Log
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.models.User
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.ui.dashboard.repo.PostsRepository
import com.google.firebase.firestore.DocumentSnapshot
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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
        const val INITIAL_RADIUS = 20.0
    }

    fun loadInitial(
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
                                        val singles = orderedPosts.map {
                                            userRepo.getUserData(it.posterUserId)
                                                .onErrorReturnItem(User())
                                        }

                                        var counter = 0
                                        disposables.add(
                                            Single.merge(singles)
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribeOn(Schedulers.io())
                                                .subscribeBy(
                                                    onError = {
                                                        Log.e(TAG, it.message, it)
                                                    },
                                                    onNext = {
                                                        orderedPosts[0].posterUsername = it.username
                                                        orderedPosts[0].posterProfilePic =
                                                            it.profilePicUrl
                                                        counter++
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