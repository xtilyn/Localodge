package com.devssocial.localodge.ui.post_detail.ui


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.devssocial.localodge.R
import com.devssocial.localodge.interfaces.PostOptionsListener
import com.devssocial.localodge.models.Location
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.models.Report
import com.devssocial.localodge.ui.post_detail.view_model.PostViewModel
import com.devssocial.localodge.utils.*
import com.devssocial.localodge.utils.ActivityLaunchHelper.Companion.CONTENT_ID
import com.devssocial.localodge.utils.ActivityLaunchHelper.Companion.REQUEST_COMMENT
import com.devssocial.localodge.view_holders.PostViewHolder
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_post_detail.*
import kotlinx.android.synthetic.main.list_item_user_post.*

class PostDetailFragment : Fragment(), PostOptionsListener {

    companion object {
        private const val TAG = "PostDetailFragment"
    }

    private val disposables = CompositeDisposable()

    private lateinit var postViewModel: PostViewModel
    private lateinit var postId: String
    private lateinit var postViewItem: PostViewItem

    private val postItemsListener: View.OnClickListener by lazy {
        View.OnClickListener {
            when (it.id) {
                R.id.user_post_more_options -> {
                    PostsHelper(this@PostDetailFragment).showMoreOptionsPopup(
                        context,
                        user_post_more_options,
                        postViewItem,
                        null
                    )
                }
                R.id.user_post_username, R.id.user_post_profile_pic -> {
                    ActivityLaunchHelper.gotoUserProfile(postViewItem.posterUserId)
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

        val isComment = activity?.intent?.extras?.getBoolean(REQUEST_COMMENT, false)
        if (isComment == true && postViewModel.userRepo.getCurrentUser() != null) {
            comment_et.requestFocus()
            context?.let { KeyboardUtils.showKeyboard(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        postId = activity?.intent?.extras?.getString(CONTENT_ID)
            ?: throw Exception("Missing intent extras")

        activity?.let {
            postViewModel = ViewModelProviders.of(it)[PostViewModel::class.java]
        }
    }

    override fun onStart() {
        super.onStart()

        showProgress(true)
        getUserLocation { userLocation: Location? ->
            getPostDetail { postViewItem: PostViewItem ->
                this.postViewItem = postViewItem
                showProgress(false)
                if (userLocation == null) return@getPostDetail
                PostViewHolder.bindItem(
                    postViewItem,
                    post_detail_container,
                    userLocation
                )

                // listeners
                user_post_more_options?.setOnClickListener(postItemsListener)
                user_post_username?.setOnClickListener(postItemsListener)
                // TODO CONTINUE HERE SETUP POST ITEMS LISTENER
            }
        }
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
        swipe_refresh_post_detail?.isRefreshing = show
    }

    override fun onReportUser(userIdToReport: String, reason: String, desc: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onReportPost(postId: String, reason: String, desc: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBlockUser(userId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBlockPost(postViewItem: PostViewItem, position: Int?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
