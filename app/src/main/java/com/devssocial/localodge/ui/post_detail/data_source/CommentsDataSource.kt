package com.devssocial.localodge.ui.post_detail.data_source

import android.util.Log
import androidx.paging.ItemKeyedDataSource
import com.devssocial.localodge.NO_VALUE
import com.devssocial.localodge.enums.Status
import com.devssocial.localodge.models.CommentViewItem
import com.devssocial.localodge.ui.dashboard.repo.PostsRepository
import com.google.firebase.Timestamp
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class CommentsDataSource(
    private val postId: String,
    private val disposables: CompositeDisposable,
    private val initialLoadResult: BehaviorSubject<Status>,
    private val postsRepo: PostsRepository
) : ItemKeyedDataSource<Timestamp, CommentViewItem>() {

    companion object {
        private const val TAG = "CommentsDataSource"
    }

    override fun loadInitial(
        params: LoadInitialParams<Timestamp>,
        callback: LoadInitialCallback<CommentViewItem>
    ) {
        initialLoadResult.onNext(Status.LOADING)
        disposables.add(
            postsRepo
                .getComments(
                    postId,
                    params.requestedLoadSize.toLong(),
                    null
                )
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it, false)
                    },
                    onSuccess = {
                        if (it.isEmpty()) initialLoadResult.onNext(Status.SUCCESS_NO_DATA)
                        else initialLoadResult.onNext(Status.SUCCESS_WITH_DATA)
                        callback.onResult(it)
                    }
                )
        )
    }

    override fun loadAfter(params: LoadParams<Timestamp>, callback: LoadCallback<CommentViewItem>) {
        disposables.add(
            postsRepo
                .getComments(
                    postId,
                    params.requestedLoadSize.toLong(),
                    params.key
                )
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it, true)
                    },
                    onSuccess = {
                        callback.onResult(it)
                    }
                )
        )
    }

    override fun loadBefore(
        params: LoadParams<Timestamp>,
        callback: LoadCallback<CommentViewItem>
    ) {}

    override fun getKey(item: CommentViewItem): Timestamp = item.timestamp!!

    private fun handleError(error: Throwable, loadAfter: Boolean) {
        if (loadAfter) {
            Log.e(TAG, error.message, error)
            return
        }
        if (error.message == NO_VALUE) {
            initialLoadResult.onNext(Status.SUCCESS_NO_DATA)
        } else {
            initialLoadResult.onNext(Status.ERROR)
            Log.e(TAG, error.message, error)
        }
    }
}