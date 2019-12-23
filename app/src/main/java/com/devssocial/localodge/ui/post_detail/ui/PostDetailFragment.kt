package com.devssocial.localodge.ui.post_detail.ui


import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.devssocial.localodge.R
import com.devssocial.localodge.data_objects.AdapterPayload
import com.devssocial.localodge.enums.ReportType
import com.devssocial.localodge.enums.Status
import com.devssocial.localodge.extensions.*
import com.devssocial.localodge.interfaces.ListItemListener
import com.devssocial.localodge.interfaces.PostOptionsListener
import com.devssocial.localodge.models.Location
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.models.Report
import com.devssocial.localodge.ui.post_detail.adapter.CommentsPagedAdapter
import com.devssocial.localodge.ui.post_detail.data_source.CommentsDataSourceFactory
import com.devssocial.localodge.ui.post_detail.view_model.PostViewModel
import com.devssocial.localodge.utils.KeyboardUtils
import com.devssocial.localodge.utils.SharedPrefManager
import com.devssocial.localodge.utils.helpers.DialogHelper
import com.devssocial.localodge.utils.helpers.PhotoPicker
import com.devssocial.localodge.utils.helpers.PostsHelper
import com.devssocial.localodge.view_holders.PostViewHolder
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.model.Image
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_post_detail.*
import kotlinx.android.synthetic.main.list_item_user_post.*

class PostDetailFragment : Fragment(), PostOptionsListener, ListItemListener {

    companion object {
        private const val TAG = "PostDetailFragment"
    }

    private val disposables = CompositeDisposable()
    private val commentsInitialLoadResult = BehaviorSubject.createDefault(Status.LOADING)

    private lateinit var postViewModel: PostViewModel
    private lateinit var postId: String
    private lateinit var postViewItem: PostViewItem
    private lateinit var commentsAdapter: CommentsPagedAdapter
    private lateinit var factory: CommentsDataSourceFactory
    private var currentCommentPhotoPath: String? = null

    private val args: PostDetailFragmentArgs by navArgs()

    private val postItemsListener: View.OnClickListener by lazy {
        View.OnClickListener {
            when (it.id) {
                R.id.user_post_more_options -> {
                    if (!isLoggedIn()) {
                        activity?.let { a ->
                            DialogHelper(a)
                                .showSignInRequiredDialog(
                                a,
                                resources.getString(R.string.sign_in_required_to_perform_actions)
                            )
                        }
                        return@OnClickListener
                    }
                    PostsHelper(this@PostDetailFragment)
                        .showMoreOptionsPopup(
                        context,
                        user_post_more_options,
                        postViewItem,
                        null
                    )
                }
                R.id.user_post_media_content_container -> {
                    context?.let { c ->
                        DialogHelper(c).showMediaDialog(
                            photoUrl = postViewItem.photoUrl,
                            videoUrl = postViewItem.videoUrl
                        )
                    }
                }
                R.id.user_post_comment -> {
                    if (!isLoggedIn()) {
                        activity?.let { a ->
                            DialogHelper(a)
                                .showSignInRequiredDialog(
                                a,
                                resources.getString(R.string.sign_in_required_to_comment)
                            )
                        }
                        return@OnClickListener
                    }
                    comment_et?.requestFocus()
                    context?.let { c -> KeyboardUtils.showKeyboard(c) }
                }
                R.id.user_post_like -> {
                    if (!isLoggedIn()) {
                        activity?.let { a ->
                            DialogHelper(a)
                                .showSignInRequiredDialog(
                                a,
                                resources.getString(R.string.sign_in_required_to_like_posts)
                            )
                        }
                        return@OnClickListener
                    }
                    toggleLike()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isComment = args.requestComment
        if (isComment && postViewModel.userRepo.getCurrentUser() != null) {
            comment_et.requestFocus()
            context?.let { KeyboardUtils.showKeyboard(it) }
        }

        showProgress(true)
        getUserLocation { userLocation: Location? ->
            getPostDetail { postViewItem: PostViewItem ->
                this.postViewItem = postViewItem

                if (userLocation == null || context == null) return@getPostDetail
                PostViewHolder.bindItem(
                    postViewItem,
                    post_detail_container,
                    userLocation
                )

                // listeners
                user_post_more_options?.setOnClickListener(postItemsListener)
                user_post_username?.setOnClickListener(postItemsListener)
                if (postViewItem.photoUrl != null || postViewItem.videoUrl != null) {
                    user_post_media_content_container.setOnClickListener(postItemsListener)
                }
                user_post_comment?.setOnClickListener(postItemsListener)
                user_post_like?.setOnClickListener(postItemsListener)

                post_comment?.setOnClickListener(::onPostComment)
                comment_et?.doOnTextChanged { text, _, _, _ ->
                    if (context == null) return@doOnTextChanged
                    comment_et?.error = null
                    post_comment?.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context!!,
                            if (text.toString().isNotEmpty()) R.color.colorPrimary
                            else R.color.lightGray
                        )
                    )
                }

                add_photo_comment?.setOnClickListener {
                    PhotoPicker.pickFromGallery(this)
                }

                delete_media?.setOnClickListener {
                    toggleCommentAttachment(false)
                }
            }

            setupRecyclerView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postId = args.contentId
        activity?.let {
            postViewModel = ViewModelProviders.of(it)[PostViewModel::class.java]
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            val images = ImagePicker.getImages(data) as ArrayList<Image>
            currentCommentPhotoPath = images[0].path
            toggleCommentAttachment(true)
            context?.let {
                Glide.with(it)
                    .load(currentCommentPhotoPath)
                    .into(comment_image_attachment)
            }
            comment_et?.error = null
            post_comment?.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context!!,
                    R.color.colorPrimary
                )
            )
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPostDetail(onSuccess: (PostViewItem) -> Unit) {
        disposables.add(
            postViewModel
                .postsRepo
                .getPostDetail(postId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onSuccess = { postViewItem ->
                        onSuccess(postViewItem)
                    }
                )
        )
    }

    private fun getUserLocation(onSuccess: (Location?) -> Unit) {
        activity?.let {
            val loc = SharedPrefManager(it).getLocation()
            onSuccess(loc)
        }
    }

    private fun handleError(error: Throwable) {
        Log.e(TAG, error.message, error)
        context?.let {
            Toasty.error(it, getString(R.string.generic_error_message)).show()
            showProgress(false)
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) loading_overlay?.popShow()
        else loading_overlay?.gone()
    }

    private fun toggleLike() {
        val userId = postViewModel.userRepo.getCurrentUserId() ?: return
        if (postViewItem.likes.contains(userId)) {
            postViewItem.likes.remove(userId)
        } else {
            postViewItem.likes[userId] = true
        }
        updateLikes(postViewItem.likes) {
            if (context == null) return@updateLikes
            user_post_like.setCompoundDrawables(
                if (postViewItem.likes.contains(userId)) {
                    ContextCompat.getDrawable(context!!, R.drawable.ic_favorite_filled)
                } else {
                    ContextCompat.getDrawable(context!!, R.drawable.ic_favorite_border)
                },
                null, null, null
            )
        }
    }

    private fun updateLikes(newLikes: HashMap<String, Boolean>, onComplete: () -> Unit) {
        disposables.add(
            postViewModel
                .postsRepo
                .updateLikes(postViewItem.objectID, newLikes)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onComplete = {
                        onComplete()
                    }
                )
        )
    }

    private fun onPostComment(view: View) {
        if (!isLoggedIn()) {
            activity?.let { a ->
                DialogHelper(a).showSignInRequiredDialog(
                    a,
                    resources.getString(R.string.sign_in_required_to_comment)
                )
            }
            return
        }

        val comment = comment_et?.text.toString()
        if (comment.isBlank() && currentCommentPhotoPath == null) {
            comment_et?.error = resources.getString(R.string.comment_required)
            return
        }
        showPostCommentProgress(true)
        disposables.add(
            postViewModel
                .postsRepo
                .postComment(postViewItem.objectID, comment, currentCommentPhotoPath)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                        showPostCommentProgress(false)
                    },
                    onComplete = {
                        showPostCommentProgress(false)
                        context?.let {
                            factory.invalidateDataSource()
                            setupRecyclerView()
                            comment_et?.setText("")
                            toggleCommentAttachment(false)
                        }
                    }
                )
        )
    }

    private fun toggleCommentAttachment(show: Boolean) {
        if (show) {
            post_comment_photo_container?.instaVisible()
        } else {
            comment_image_attachment?.setImageDrawable(null)
            post_comment_photo_container?.instaGone()
            currentCommentPhotoPath = null
            post_comment?.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context!!,
                    R.color.lightGray
                )
            )
        }
    }

    private fun showPostCommentProgress(show: Boolean) {
        post_comment?.isEnabled = !show
        if (show) {
            post_comment?.instaInvisible()
            post_comment_progress?.popShow()
        } else {
            post_comment_progress?.popHide()
            post_comment?.popShow()
        }
    }

    private fun setupRecyclerView() {
        val userId = postViewModel.userRepo.getCurrentUserId() ?: ""
        commentsAdapter = CommentsPagedAdapter(this@PostDetailFragment, userId)
        comments_recycler_view?.adapter = commentsAdapter
        comments_recycler_view?.layoutManager = LinearLayoutManager(context)

        factory = CommentsDataSourceFactory(
            postId,
            disposables,
            commentsInitialLoadResult,
            postViewModel.postsRepo
        )
        val config = PagedList.Config.Builder()
            .setPageSize(10)
            .setEnablePlaceholders(true)
            .setInitialLoadSizeHint(10)
            .build()

        disposables.addAll(
            RxPagedListBuilder(factory, config).buildObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe {
                    commentsAdapter.submitList(it)
                },
            commentsInitialLoadResult
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    showProgress(it == Status.LOADING)
                    if (it == Status.ERROR) showError()
                }
        )
    }

    private fun showError() {
        context?.let {
            Toasty.error(it, getString(R.string.generic_error_message)).show()
        }
    }

    override fun onItemClick(view: View, position: Int) {
        val current = commentsAdapter.currentList?.get(position) ?: return
        when (view.id) {
            R.id.comment_toggle_text -> {
                commentsAdapter.notifyItemChanged(position, AdapterPayload.EXPAND_OR_COLLAPSE)
            }
            R.id.comment_photo_container -> {
                context?.let {
                    DialogHelper(it).showMediaDialog(
                        photoUrl = current.photoUrl,
                        videoUrl = null
                    )
                }
            }
        }
    }

    override fun onItemLongPress(view: View, position: Int): Boolean {
        val current = commentsAdapter.currentList?.get(position) ?: return false
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_layout_report, null)
        val popup = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        popupView.findViewById<Button>(R.id.report_button).setOnClickListener {
            context?.let {
                DialogHelper(it).showReportDialog(
                    it, ReportType.USER
                ) { reason, desc ->

                    showProgress(true)
                    val userId = postViewModel.userRepo.getCurrentUserId() ?: return@showReportDialog
                    disposables.add(
                        postViewModel
                            .localodgeRepo
                            .sendUserReport(
                                current.postedBy,
                                Report(
                                    reportedByUserId = userId,
                                    reason = reason,
                                    description = desc
                                )
                            )
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribeBy(
                                onError = { error ->
                                    handleError(error)
                                },
                                onComplete = {
                                    showProgress(false)
                                    context?.let { c ->
                                        Toasty.success(c, getString(R.string.report_sent))
                                    }
                                }
                            )
                    )
                }
            }
            popup.dismiss()
        }

        popup.showAsDropDown(view, view.width, 0)
        return true
    }

    override fun onReportUser(userIdToReport: String, reason: String, desc: String) {
        val userId = postViewModel.userRepo.getCurrentUserId() ?: return
        val report = Report(
            reportedByUserId = userId,
            reason = reason,
            description = desc
        )
        showProgress(true)
        disposables.add(
            postViewModel
                .localodgeRepo
                .sendUserReport(userIdToReport, report)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onComplete = {
                        showProgress(false)
                        context?.let { c ->
                            Toasty.success(
                                c,
                                resources.getString(R.string.report_sent)
                            ).show()
                        }
                    }
                )
        )
    }

    override fun onReportPost(postId: String, reason: String, desc: String) {
        val userId = postViewModel.userRepo.getCurrentUserId() ?: return
        val report = Report(
            reportedByUserId = userId,
            reason = reason,
            description = desc
        )
        showProgress(true)
        disposables.add(
            postViewModel
                .localodgeRepo
                .sendPostReport(postId, report)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onComplete = {
                        showProgress(false)
                        context?.let { c ->
                            Toasty.success(
                                c,
                                resources.getString(R.string.report_sent)
                            ).show()
                        }
                    }
                )
        )
    }

    override fun onBlockUser(userId: String) {
        showProgress(true)
        disposables.add(
            postViewModel
                .userRepo
                .blockUser(userId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onComplete = {
                        showProgress(false)
                        context?.let {
                            Toasty.success(
                                it,
                                getString(R.string.user_blocked)
                            )
                        }
                    }
                )
        )
    }

    override fun onBlockPost(postViewItem: PostViewItem, position: Int?) {
        showProgress(true)
        disposables.addAll(
            postViewModel
                .userRepo
                .blockPost(postViewItem.objectID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onComplete = {
                        showProgress(false)
                        context?.let { c ->
                            Toasty.success(c, resources.getString(R.string.post_blocked)).show()
                        }
                    }
                )
        )
    }

    private fun isLoggedIn(): Boolean = postViewModel.userRepo.getCurrentUser() != null
}
