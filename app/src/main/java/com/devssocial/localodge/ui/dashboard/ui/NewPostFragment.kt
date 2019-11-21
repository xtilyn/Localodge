package com.devssocial.localodge.ui.dashboard.ui


import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.*
import com.devssocial.localodge.models.User
import com.devssocial.localodge.room_models.UserRoom
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.devssocial.localodge.utils.KeyboardUtils
import com.devssocial.localodge.utils.PhotoPicker
import com.esafirm.imagepicker.features.ImagePicker
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_new_post.*
import kotlinx.android.synthetic.main.layout_loading_overlay.*

class NewPostFragment : Fragment() {

    companion object {
        private const val TAG = "NewPostFragment"
    }

    private lateinit var dashboardViewModel: DashboardViewModel
    private val disposables = CompositeDisposable()
    private var currentMediaPath: String? = null

    private val newPostClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.back_button -> {
                activity?.onBackPressed()
            }
            R.id.post_text_view -> {
                onPostButtonClick()
            }
            R.id.take_photo -> {
                PhotoPicker.captureImage(this)
            }
            R.id.post_gallery -> {
                PhotoPicker.pickFromGallery(this, true)
            }
            R.id.user_post_media_content_container -> {
                if (PhotoPicker.isVideoFile(currentMediaPath)) {
                    ActivityLaunchHelper.goToMediaViewer(activity, null, currentMediaPath)
                } else {
                    ActivityLaunchHelper.goToMediaViewer(activity, currentMediaPath, null)
                }
            }
            R.id.delete_media -> {
                currentMediaPath = null
                user_post_image_content?.popHide {
                    user_post_image_content?.setImageResource(0)
                }
                play_video?.instaGone()
                delete_media?.instaGone()
                togglePostButton()
            }
            R.id.promote_post -> {
                // TODO CONTINUE HERE SHOW RATING OPTIONS DIALOG
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboardViewModel = ViewModelProviders.of(activity!!)[DashboardViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup static widgets
        post_description_edit_text.requestFocus()
        KeyboardUtils.showKeyboard(context!!)

        back_button.setOnClickListener(newPostClickListener)
        post_text_view.setOnClickListener(newPostClickListener)
        post_gallery.setOnClickListener(newPostClickListener)
        take_photo.setOnClickListener(newPostClickListener)
        promote_post.setOnClickListener(newPostClickListener)
        delete_media.setOnClickListener(newPostClickListener)

        post_description_edit_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                togglePostButton()
            }

        })

        getUserDataFromRoom { user ->
            if (context == null) return@getUserDataFromRoom
            Glide.with(context!!)
                .load(user.profilePicUrl)
                .into(user_profile_pic)
            username_text_view?.text = user.username
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            ImagePicker.getFirstImageOrNull(data)?.let { image ->
                delete_media?.instaVisible()
                currentMediaPath = image.path
                togglePostButton()

                if (PhotoPicker.isVideoFile(image.path)) play_video?.instaVisible()
                else play_video?.instaGone()

                user_post_image_content?.popShow()
                Glide.with(this)
                    .load(image.path)
                    .into(user_post_image_content)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onPostButtonClick() {
        // TODO CONTINUE HERE empty check
        // todo set location in geofirestore
//        val collectionRef = FirebaseFirestore.getInstance().collection(POSTS)
//        val geoFirestore = GeoFirestore(collectionRef)
//        geoFirestore.setLocation("que8B9fxxjcvbC81h32VRjeBSUW2", GeoPoint(37.7853889, -122.4056973)) { exception ->
//            if (exception != null)
//                Log.d(TAG, "Location saved on server successfully!")
//        }
//        disposables.add(
//
//        )
    }

    private fun togglePostButton() {
        if (context == null) return
        if (post_description_edit_text?.text?.isEmpty() == false || !currentMediaPath.isNullOrBlank()) {
            post_button?.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
        } else {
            post_button?.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.lightGray))
        }
    }

    private fun getUserDataFromRoom(onSuccess: (User) -> Unit) {
        val userId = dashboardViewModel.userRepo.getCurrentUserId() ?: return
        disposables.add(
            dashboardViewModel
                .userRepo
                .userDao
                .getUser(userId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        getUserDataFromFirebase(onSuccess)
                    },
                    onSuccess = {
                        onSuccess(it.mapProperties(User()))
                    }
                )
        )
    }

    private fun getUserDataFromFirebase(onSuccess: (User) -> Unit) {
        val userId = dashboardViewModel.userRepo.getCurrentUserId() ?: return
        disposables.add(
            dashboardViewModel
                .userRepo
                .getUserData(userId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onSuccess = {
                        onSuccess(it)
                    }
                )
        )
    }

    private fun showProgress(show: Boolean) {
        if (show) loading_overlay?.popShow()
        else loading_overlay?.popHide()
    }

    private fun handleError(error: Throwable) {
        showProgress(false)
        Log.e(TAG, error.message, error)
        context?.let {
            Toasty.error(it, getString(R.string.generic_error_message))
        }
    }
}
