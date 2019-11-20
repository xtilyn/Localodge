package com.devssocial.localodge.ui.post_detail.data_source

import androidx.paging.DataSource
import com.devssocial.localodge.enums.Status
import com.devssocial.localodge.models.CommentViewItem
import com.devssocial.localodge.ui.dashboard.repo.PostsRepository
import com.google.firebase.Timestamp
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CommentsDataSourceFactory(
    private val postId: String,
    private val disposables: CompositeDisposable,
    private val initialLoadResult: BehaviorSubject<Status>,
    private val postsRepo: PostsRepository
) : DataSource.Factory<Timestamp, CommentViewItem>() {

    private lateinit var current: CommentsDataSource

    override fun create(): DataSource<Timestamp, CommentViewItem> {
        if (!this::current.isInitialized) {
            current = createNew()
        }
        if (current.isInvalid) {
            current = createNew()
        }
        return current
    }

    fun invalidateDataSource() {
        current.invalidate()
    }

    private fun createNew(): CommentsDataSource = CommentsDataSource(
        postId,
        disposables,
        initialLoadResult,
        postsRepo
    )

}