package com.devssocial.localodge.ui.dashboard.view_model

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.devssocial.localodge.R
import com.devssocial.localodge.enums.Status
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.models.*
import com.devssocial.localodge.shared.LocalodgeRepository
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.ui.dashboard.interfaces.NewPostFragmentCallback
import com.devssocial.localodge.ui.dashboard.repo.PostsRepository
import com.devssocial.localodge.ui.dashboard.utils.PostsUtil
import com.devssocial.localodge.utils.Resource
import com.google.firebase.firestore.DocumentSnapshot
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.imperiumlabs.geofirestore.GeoFirestore

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PostsProvider"
        const val INITIAL_RADIUS = 50.0
        const val RADIUS_INCREMENT = 50.0
    }

    private val disposables = CompositeDisposable()

    // repositories
    private val context = application.baseContext
    val postsRepo = PostsRepository(context)
    val userRepo = UserRepository(context)
    val localodgeRepo = LocalodgeRepository()

    // observables
    var onBackPressed = BehaviorSubject.create<Boolean>()
    val data = BehaviorSubject.create<Resource<ArrayList<PostViewItem>>>()

    var isDrawerOpen = false
    lateinit var unreadNotifications: ArrayList<Notification>
    var newPostCallback: NewPostFragmentCallback? = null

    fun isUserLoggedIn(): Boolean = userRepo.getCurrentUser() != null

    // TODO CONTINUE HERE, PAGINATE THIS
    fun loadInitialWithLocation(
        userLocation: Location,
        radius: Double
    ) {
        data.onNext(Resource.loading())
        postsRepo.loadDataAroundLocation(
            userLocation,
            radius,
            object : GeoFirestore.SingleGeoQueryDataEventCallback {
                override fun onComplete(
                    documentSnapshots: List<DocumentSnapshot>?,
                    exception: Exception?
                ) {
                    if (exception != null) {
                        data.onNext(Resource.error(context.getString(R.string.generic_error_message)))
                    } else {

                        val unorderedPosts = documentSnapshots?.map {
                            it.toObject(Post::class.java)!!
                        }

                        if (unorderedPosts.isNullOrEmpty()) {
                            data.onNext(Resource.success(null))
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
                                                userRepo.getUserData(it.posterUserId).onErrorReturnItem(
                                                    User()
                                                ),
                                                postsRepo.getPostStats(it.objectID),
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
                                                        val iterator = orderedPosts.iterator()
                                                        while (iterator.hasNext()) {
                                                            val curr = iterator.next()
                                                            if (curr.posterUsername.isEmpty() ||
                                                                    curr.objectID.isEmpty()) {
                                                                iterator.remove()
                                                            }
                                                        }
                                                        data.onNext(
                                                            Resource.success(
                                                                if (orderedPosts.isNotEmpty())
                                                                    orderedPosts
                                                                else null
                                                            )
                                                        )
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

    override fun onCleared() {
        disposables.clear()
        super.onCleared()
    }
}