package com.devssocial.localodge.ui.dashboard.ui


import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.*
import com.devssocial.localodge.models.User
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.*
import com.esafirm.imagepicker.features.ImagePicker
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_new_post.*
import kotlinx.android.synthetic.main.layout_loading_overlay.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class NewPostFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    companion object {
        private const val TAG = "NewPostFragment"
    }

    private lateinit var dashboardViewModel: DashboardViewModel
    private val userLocationResult by lazy { BehaviorSubject.create<Location>() }
    private val disposables = CompositeDisposable()
    private var currentMediaPath: String? = null
    private var chosenPostRating: Int = 0

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
                // TODO CONTINUE HERE SHOW RATING OPTIONS DIALOG and change chosenPostRating (if changed)
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

        requireActivity().onBackPressedDispatcher.addCallback {
            if (hasData()) {
                context?.let {
                    DialogHelper(it)
                        .showConfirmActionDialog(
                            getString(R.string.discard_changes),
                            getString(R.string.are_you_sure_you_want_to_discard_changes),
                            getString(R.string.discard),
                            { dialog ->
                                dialog.dismiss()
                                activity?.onBackPressed()
                            },
                            getString(R.string.cancel),
                            { dialog -> dialog.dismiss() },
                            false
                        )
                }
            }
        }

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

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        showLocationNotFoundDialog()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        getLocation()
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

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (activity == null) return
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) checkLocationSettings()
                    else {
                        userLocationResult.onNext(location)
                    }
                }
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationSettings() {
        LocationRequest.create()?.apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }?.let { locationRequest ->
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

            val client: SettingsClient = LocationServices.getSettingsClient(activity!!)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        exception.startResolutionForResult(
                            activity!!,
                            DashboardFragment.REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
        }
    }

    @AfterPermissionGranted(DashboardFragment.REQUEST_LOCATION_PERMISSION)
    fun requestLocationPermission() {
        val perms = Manifest.permission.ACCESS_FINE_LOCATION
        if (EasyPermissions.hasPermissions(context!!, perms)) {
            getLocation()
        } else {
            EasyPermissions.requestPermissions(
                this,
                resources.getString(R.string.location_needed_to_make_a_post_with_question),
                DashboardFragment.REQUEST_LOCATION_PERMISSION,
                perms
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun onPostButtonClick() {
        if (hasData()) {
            val desc = post_description_edit_text?.text?.toString() ?: ""
            var photoUrl: String? = null
            var videoUrl: String? = null
            if (!currentMediaPath.isNullOrEmpty()) {
                if (PhotoPicker.isVideoFile(currentMediaPath)) videoUrl = currentMediaPath
                else photoUrl = currentMediaPath
            }

            val location = SharedPrefManager(activity).getLocation()
            if (location == null) {
                getLocation()
                disposables.add(
                    userLocationResult
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { userLoc: Location? ->
                            if (userLoc != null) createPost(
                                lat = userLoc.latitude,
                                lng = userLoc.longitude,
                                desc = desc,
                                photoUrl = photoUrl,
                                videoUrl = videoUrl
                            )
                        }
                )
            } else {
                createPost(
                    lat = location.lat,
                    lng = location.lng,
                    desc = desc,
                    photoUrl = photoUrl,
                    videoUrl = videoUrl
                )
            }
        }
    }

    private fun createPost(
        lat: Double,
        lng: Double,
        desc: String,
        photoUrl: String?,
        videoUrl: String?
    ) {
        showProgress(true)
        disposables.add(
            dashboardViewModel
                .postsRepo
                .createPost(
                    lat = lat,
                    lng = lng,
                    desc = desc,
                    photoUrl = photoUrl,
                    videoUrl = videoUrl,
                    rating = chosenPostRating
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onSuccess = { postId ->
                        showProgress(false)
                        if (postId.isEmpty()) return@subscribeBy
                        ActivityLaunchHelper.goToPostDetail(
                            activity,
                            postId,
                            false
                        )
                    }
                )
        )
    }

    private fun togglePostButton() {
        if (context == null) return
        if (hasData()) {
            post_button?.setCardBackgroundColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.colorPrimary
                )
            )
        } else {
            post_button?.setCardBackgroundColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.lightGray
                )
            )
        }
    }

    private fun hasData(): Boolean =
        post_description_edit_text?.text?.isEmpty() == false || !currentMediaPath.isNullOrBlank()

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

    private fun showLocationNotFoundDialog() {
        val dialog = AlertDialog.Builder(context!!).create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setTitle(resources.getString(R.string.location_needed_to_make_a_post))
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Retry") { dialogInterface, _ ->
            dialogInterface.dismiss()
            getLocation()
        }
        dialog.show()
    }
}
