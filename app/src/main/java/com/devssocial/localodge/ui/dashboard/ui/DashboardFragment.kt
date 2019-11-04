package com.devssocial.localodge.ui.dashboard.ui


import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.devssocial.localodge.*
import com.devssocial.localodge.R
import com.devssocial.localodge.callbacks.ListItemListener
import com.devssocial.localodge.data_objects.AdapterPayload
import com.devssocial.localodge.extensions.gone
import com.devssocial.localodge.extensions.mapProperties
import com.devssocial.localodge.extensions.onLoadEnded
import com.devssocial.localodge.extensions.visible
import com.devssocial.localodge.models.Feedback
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.models.PostViewItem
import com.devssocial.localodge.models.User
import com.devssocial.localodge.room_models.PostRoom
import com.devssocial.localodge.ui.dashboard.adapter.PostsAdapter
import com.devssocial.localodge.ui.dashboard.utils.PostsProvider
import com.devssocial.localodge.ui.dashboard.utils.PostsUtil
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.devssocial.localodge.utils.DialogHelper
import com.devssocial.localodge.utils.KeyboardUtils
import com.devssocial.localodge.utils.PhotoPicker
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.model.Image
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import es.dmoral.toasty.Toasty
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_dashboard.*
import kotlinx.android.synthetic.main.dialog_choose_photo.view.*
import kotlinx.android.synthetic.main.dialog_choose_photo.view.close_dialog
import kotlinx.android.synthetic.main.dialog_send_feedback.view.*
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.layout_empty_state.*
import kotlinx.android.synthetic.main.nav_header_dashboard.view.*
import org.imperiumlabs.geofirestore.GeoFirestore
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.time.milliseconds

class DashboardFragment :
    Fragment(),
    NavigationView.OnNavigationItemSelectedListener,
    EasyPermissions.PermissionCallbacks, ListItemListener {

    companion object {
        private const val TAG = "DashboardFragment"
        const val REQUEST_LOCATION_PERMISSION = 1
        const val REQUEST_CHECK_SETTINGS = 2
        private const val RC_CAMERA = 3000
        private const val HITS_PER_PAGE = 15
    }

    private val disposables = CompositeDisposable()
    private var userLocation: Location? = null
    private var expandSearchCount: Int = 0 // number of times geo search was expanded due to lack of data (limit = 2)

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var postsProvider: PostsProvider
    private lateinit var retrievedPosts: HashMap<Int, ArrayList<PostViewItem>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboardViewModel = ViewModelProviders.of(activity!!)[DashboardViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        postsProvider = PostsProvider(disposables, dashboardViewModel.postsRepo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup widgets
        fab.setOnClickListener {
            (it as? FloatingActionButton)?.hide()
            findNavController().navigate(R.id.action_dashboardFragment_to_newPostFragment)
        }

        // setup static widgets
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            activity!!,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerClosed(drawerView: View) {
                dashboardViewModel.isDrawerOpen = false
            }

            override fun onDrawerOpened(drawerView: View) {
                dashboardViewModel.isDrawerOpen = true
            }

        }

        )
        toggle.syncState()

        // recycler view
        postsAdapter = PostsAdapter(arrayListOf(), this@DashboardFragment)
        dashboard_recyclerview?.adapter = postsAdapter
        dashboard_recyclerview?.layoutManager = LinearLayoutManager(context)
        // TODO CONTINUE HERE onScrolledToBottom: loadMore()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onStart() {
        super.onStart()

        if (context != null && view != null)
            KeyboardUtils.hideKeyboard(context!!, view!!)

        // observe activity's onBackPressed event
        disposables.add(
            dashboardViewModel.onBackPressed
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (drawer_layout?.isDrawerOpen(GravityCompat.START) == true) {
                        drawer_layout?.closeDrawer(GravityCompat.START)
                    }
                }
        )

        retrieveCurrentUserData()
        getLocation()
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
            val images = ImagePicker.getImages(data) as ArrayList<Image>

            showProfilePicProgress(true)
            var photoUrl = ""
            disposables.add(
                dashboardViewModel.userRepo
                    .updateProfilePicInStorage(images[0].path)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doOnNext {
                        photoUrl = if (it.second.isNotEmpty()) it.second else ""
                    }.flatMapCompletable {
                        if (photoUrl.isNotEmpty()) {
                            dashboardViewModel
                                .userRepo
                                .updateProfilePicInFirestore(photoUrl)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .andThen(Completable.defer {
                                    dashboardViewModel.userRepo.userDao
                                        .updateProfilePic(
                                            dashboardViewModel.userRepo.getCurrentUserId()
                                                ?: return@defer Completable.complete(),
                                            photoUrl
                                        )
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeOn(Schedulers.io())
                                })
                        } else {
                            Completable.complete()
                        }
                    }.doOnComplete {
                        val headerView = nav_view.getHeaderView(0)
                        Glide.with(this)
                            .load(images[0].path)
                            .onLoadEnded { showProfilePicProgress(false) }
                            .into(headerView.user_profile_pic_image_view)
                    }
                    .doOnError {
                        Log.e(TAG, it.message, it)
                    }
                    .subscribe()
            )

            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RC_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage()
            }
        }

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_share -> {
                activity?.let {
                    ShareCompat.IntentBuilder.from(it)
                        .setType("text/plain")
                        .setChooserTitle("Chooser title")
                        .setText("http://play.google.com/store/apps/details?id=" + it.packageName)
                        .startChooser()
                }
            }
            R.id.nav_send_feedback -> {
                context?.let {
                    val helper = DialogHelper(it)
                    helper.createDialog(
                        R.layout.dialog_send_feedback,
                        R.style.DefaultDialogAnimation
                    )
                    helper.dialogView.send_feedback_button.setOnClickListener {
                        val comment = helper.dialogView.review_edittext.text
                        if (comment.isEmpty()) {
                            helper.dialogView.review_edittext.error =
                                resources.getString(R.string.feedback_required)
                            return@setOnClickListener
                        }
                        val ratingPercent = helper.dialogView.rating_bar.rating / 5 * 100

                        helper.setCancelable(false)
                        context?.let { c ->
                            KeyboardUtils.hideKeyboard(
                                c,
                                helper.dialogView.review_edittext
                            )
                        }
                        helper.dialogView.send_feedback_button.gone()
                        helper.dialogView.send_feedback_progress.visible()
                        sendFeedback(ratingPercent, comment.toString()) {
                            context?.let { c ->
                                Toasty.success(
                                    c,
                                    resources.getString(R.string.feedback_send)
                                ).show()
                            }
                            helper.dialog.dismiss()
                        }
                    }
                    helper.dialogView.close_dialog.setOnClickListener {
                        helper.dialog.dismiss()
                    }

                    helper.dialog.show()
                }
            }
            R.id.nav_sign_out -> {
                if (context == null) return true
                DialogHelper.showConfirmActionDialog(
                    context = context!!,
                    message = resources.getString(R.string.sign_out_confirmation),
                    positiveButtonText = resources.getString(R.string.sign_out),
                    positiveButtonCallback = {
                        drawer_layout.closeDrawer(nav_view, true)
                        it.dismiss()
                        showProgress(true)
                        (activity as LocalodgeActivity).logOut()
                    },
                    negativeButtonText = resources.getString(R.string.cancel),
                    negativeButtonCallback = {
                        it.dismiss()
                    }
                )
            }
        }
        drawer_layout?.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onItemClick(view: View, position: Int) {
        val current = postsAdapter.data[position]
        when (view.id) {
            R.id.user_post_more_options -> {
                val popupView = LayoutInflater.from(context)
                    .inflate(R.layout.popup_user_post_more_options, null)
                val popup = PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                )
                popup.showAsDropDown(view)
            }
            R.id.user_post_media_content_container, R.id.user_post_comment -> {
                ActivityLaunchHelper.goToPostDetail(activity, current.objectID)
            }
            R.id.user_post_like -> {
                if (current.likes.contains(dashboardViewModel.userRepo.getCurrentUser()?.uid)) {
                    unlikePost(current) {
                        postsAdapter.notifyItemChanged(
                            position,
                            AdapterPayload.LIKED_OR_UNLIKED_POST
                        )
                    }
                } else {
                    likePost(current) {
                        postsAdapter.notifyItemChanged(
                            position,
                            AdapterPayload.LIKED_OR_UNLIKED_POST
                        )
                    }
                }
            }
        }
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) checkLocationSettings()
                    else {
                        this@DashboardFragment.userLocation = location
                        loadInitialDashboardData()
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

            task.addOnSuccessListener {
                loadInitialDashboardData()
            }

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        exception.startResolutionForResult(
                            activity!!,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
        }
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    fun requestLocationPermission() {
        val perms = Manifest.permission.ACCESS_FINE_LOCATION
        if (EasyPermissions.hasPermissions(context!!, perms)) {
            getLocation()
        } else {
            EasyPermissions.requestPermissions(
                this,
                resources.getString(R.string.request_location),
                REQUEST_LOCATION_PERMISSION,
                perms
            )
        }
    }

    private fun retrieveCurrentUserData() {
        val user = dashboardViewModel.userRepo.getCurrentUser()
        if (dashboardViewModel.userRepo.getCurrentUser() == null) {
            ActivityLaunchHelper.goToLogin(activity)
            return
        }
        disposables.addAll(
            dashboardViewModel
                .userRepo
                .getUserData(user!!.uid)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { error ->
                        handleError(error)
                    },
                    onSuccess = { localodgeUser ->
                        setupUserWidgets(localodgeUser)
                    }
                )
        )
    }

    private fun setupUserWidgets(user: User) {
        if (context == null) return
        val headerView = nav_view.getHeaderView(0)
        val usernameFormat = "@${user.username}"
        headerView.username_text_view.text = usernameFormat

        if (user.profilePicUrl.isNotEmpty()) {
            showProfilePicProgress(true)
            Glide.with(this)
                .load(user.profilePicUrl)
                .onLoadEnded { showProfilePicProgress(false) }
                .into(headerView.user_profile_pic_image_view)
        }

        headerView.user_profile_pic_image_view.setOnClickListener {
            if (context == null) return@setOnClickListener
            val dh = DialogHelper(context!!)
            dh.createDialog(R.layout.dialog_choose_photo, R.style.DefaultDialogAnimation)
            dh.dialogView.close_dialog?.setOnClickListener {
                dh.dialog.dismiss()
            }
            dh.dialogView.take_photo?.setOnClickListener {
                captureImage()
                dh.dialog.dismiss()
            }
            dh.dialogView.choose_photo?.setOnClickListener {
                PhotoPicker.pickFromGallery(this)
                dh.dialog.dismiss()
            }
            dh.dialog.show()
        }
    }

    private fun captureImage() {
        ImagePicker.cameraOnly().start(this)
    }

    private fun showLocationNotFoundDialog() {
        val dialog = AlertDialog.Builder(context!!).create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setTitle(resources.getString(R.string.location_denied))
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Retry") { dialogInterface, _ ->
            dialogInterface.dismiss()
            getLocation()
        }
        dialog.show()
    }

    private fun sendFeedback(
        ratingPercent: Float,
        feedbackComment: String,
        onComplete: () -> Unit
    ) {
        val userId = dashboardViewModel.userRepo.getCurrentUserId() ?: return
        disposables.add(
            dashboardViewModel.localodgeRepo
                .sendFeedback(
                    userId,
                    Feedback(
                        userId = userId,
                        ratingPercent = ratingPercent,
                        comment = feedbackComment
                    )
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        Log.e(TAG, it.message, it)
                        showError(resources.getString(R.string.send_feedback_failed))
                    },
                    onComplete = {
                        onComplete()
                    }
                )
        )
    }

    private fun handleError(error: Throwable) {
        if (error.message == NO_VALUE) {
            ActivityLaunchHelper.goToLogin(activity!!)
            return
        }
        showError(resources.getString(R.string.generic_error_message))
    }

    private fun showError(message: String) {
        Toasty.error(
            context!!,
            message,
            Toast.LENGTH_SHORT, true
        ).show()
    }

    private fun loadInitialDashboardData() {
        disposables.add(
            dashboardViewModel.postsRepo.postsDao
                .getPosts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        Log.e(TAG, it.message, it)
                    },
                    onSuccess = {
                        val posts = it.map { roomPost ->
                            roomPost.mapProperties(PostViewItem()).apply {
                                if (roomPost.createdDate != null) {
                                    createdDate = Timestamp(Date(roomPost.createdDate!!))
                                }
                                if (roomPost.lat != null && roomPost.lng != null) {
                                    _geoloc = com.devssocial.localodge.models.Location(
                                        lat = roomPost.lat!!,
                                        lng = roomPost.lng!!
                                    )
                                }
                            }
                        } as ArrayList<PostViewItem>
                        postsAdapter.updateList(posts)
                        if (userLocation == null) {
                            showError(resources.getString(R.string.could_not_get_location))
                            return@subscribeBy
                        }
                        loadInitialDataFromFirebase(PostsProvider.INITIAL_RADIUS)
                    }
                )
        )
    }

    private fun loadInitialDataFromFirebase(radius: Double) {
        Log.d(this::class.java.simpleName, "searching for posts with radius: $radius")
        postsProvider.loadInitial(
            userLocation = userLocation!!,
            radius = radius,
            onError = { exception ->
                (activity as LocalodgeActivity).logAndShowError(
                    TAG,
                    exception,
                    resources.getString(R.string.error_retrieving_data)
                )
            },
            onSuccess = { posts: ArrayList<PostViewItem> ->
                if ((posts.isEmpty() || posts.size < 3) && expandSearchCount < 3) {
                    expandSearchCount++
                    loadInitialDataFromFirebase(radius + 10.0)
                } else {
                    // TODO CONTINUE HERE
//                    swipeRefreshDashboard.isRefreshing = false
                    toggleEmptyState(posts.isEmpty())

                    retrievedPosts = PostsUtil.constructMapBasedOnHitsPerPage(HITS_PER_PAGE, posts)
                    val initialPosts = if (posts.size > HITS_PER_PAGE) {
                        retrievedPosts[0]
                    } else {
                        posts
                    } ?: arrayListOf()
                    postsAdapter.updateList(initialPosts)

                    // save initial data to room
                    val postsRoom = initialPosts.map { postFirebase ->
                        postFirebase.mapProperties(PostRoom()).apply {
                            lat = postFirebase._geoloc.lat
                            lng = postFirebase._geoloc.lng
                            createdDate = postFirebase.createdDate?.seconds?.times(1000)
                        }
                    }
                    disposables.add(
                        dashboardViewModel.postsRepo.postsDao
                            .deleteAll()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe {
                                disposables.add(
                                    dashboardViewModel.postsRepo.postsDao
                                        .insertAll(postsRoom)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeOn(Schedulers.io())
                                        .subscribe()
                                )
                            }
                    )
                }
            }
        )
    }

    private fun onRefresh() {
        if (!this::retrievedPosts.isInitialized) return
        // TODO CONTINUE HERE SETUP SWIPE REFRESH
        toggleEmptyState(false)
        showProgress(true)
        postsAdapter.clear()
        expandSearchCount = 0
        loadInitialDataFromFirebase(PostsProvider.INITIAL_RADIUS)
    }

    private fun unlikePost(post: PostViewItem, onComplete: () -> Unit) {
        val currUserId = dashboardViewModel.userRepo.getCurrentUser()?.uid ?: return
        post.likes.remove(currUserId)
        updateLikes(post.objectID, post.likes, onComplete)
    }

    private fun likePost(post: PostViewItem, onComplete: () -> Unit) {
        val currUserId = dashboardViewModel.userRepo.getCurrentUser()?.uid ?: return
        post.likes.add(currUserId)
        updateLikes(post.objectID, post.likes, onComplete)
    }

    private fun updateLikes(postId: String, newLikes: HashSet<String>, onComplete: () -> Unit) {
        disposables.add(
            dashboardViewModel.postsRepo
                .updateLikes(postId, newLikes)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        (activity as LocalodgeActivity).logAndShowError(
                            TAG,
                            it,
                            resources.getString(R.string.generic_error_message)
                        )
                    },
                    onComplete = {
                        onComplete()
                    }
                )
        )
    }

    private fun showProfilePicProgress(show: Boolean) {
        val headerView = nav_view.getHeaderView(0)
        if (show) {
            headerView.upload_progress.visible()
        } else {
            headerView.upload_progress.gone()
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            loading_overlay?.visible()
        } else {
            loading_overlay?.gone()
        }
    }

    private fun toggleEmptyState(show: Boolean) {
        if (show) {
            layout_empty_state?.visible()
        } else {
            layout_empty_state?.gone()
        }
    }
}
