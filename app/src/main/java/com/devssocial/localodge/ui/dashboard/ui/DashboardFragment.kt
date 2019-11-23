package com.devssocial.localodge.ui.dashboard.ui


import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
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
import com.devssocial.localodge.interfaces.ListItemListener
import com.devssocial.localodge.data_objects.AdapterPayload
import com.devssocial.localodge.enums.Status
import com.devssocial.localodge.extensions.*
import com.devssocial.localodge.interfaces.PostOptionsListener
import com.devssocial.localodge.models.*
import com.devssocial.localodge.room_models.PostRoom
import com.devssocial.localodge.ui.dashboard.adapter.PostsAdapter
import com.devssocial.localodge.ui.dashboard.utils.PostsProvider
import com.devssocial.localodge.ui.dashboard.utils.PostsUtil
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.*
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.model.Image
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import es.dmoral.toasty.Toasty
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.content_dashboard.*
import kotlinx.android.synthetic.main.dialog_choose_photo.view.*
import kotlinx.android.synthetic.main.dialog_choose_photo.view.close_dialog
import kotlinx.android.synthetic.main.dialog_send_feedback.view.*
import kotlinx.android.synthetic.main.dialog_send_feedback.view.send_feedback_progress
import kotlinx.android.synthetic.main.dialog_sign_in_required.view.*
import kotlinx.android.synthetic.main.dialog_warning.view.*
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.nav_header_dashboard_no_user.view.*
import kotlinx.android.synthetic.main.nav_header_dashboard_signed_in.view.*
import kotlinx.android.synthetic.main.nav_header_dashboard_signed_in.view.user_profile_pic_image_view
import kotlinx.android.synthetic.main.layout_empty_state.*
import kotlinx.android.synthetic.main.layout_empty_state.view.*
import kotlinx.android.synthetic.main.nav_header_dashboard_signed_in.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.concurrent.withLock

class DashboardFragment :
    Fragment(),
    NavigationView.OnNavigationItemSelectedListener,
    EasyPermissions.PermissionCallbacks, ListItemListener, PostOptionsListener {

    companion object {
        private const val TAG = "DashboardFragment"
        const val REQUEST_LOCATION_PERMISSION = 1
        const val REQUEST_CHECK_SETTINGS = 2
        private const val RC_CAMERA = 3000
        private const val HITS_PER_PAGE = 15
    }

    private val disposables = CompositeDisposable()
    private var userLocation: Location? = null
    private var expandSearchCount: Int =
        0 // number of times geo search was expanded due to lack of data (limit = 2)

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var postsAdapter: PostsAdapter
    private lateinit var postsProvider: PostsProvider
    private var blockedPosts: HashSet<String>? = null
    private val blockedPostsResult = BehaviorSubject.create<Status>()

    private lateinit var retrievedPosts: HashMap<Int, ArrayList<PostViewItem>>
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboardViewModel = ViewModelProviders.of(activity!!)[DashboardViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        postsProvider =
            PostsProvider(disposables, dashboardViewModel.postsRepo, dashboardViewModel.userRepo)
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
            if (!dashboardViewModel.isUserLoggedIn()) {
                activity?.let { a ->
                    DialogHelper(a).showSignInRequiredDialog(
                        a,
                        resources.getString(R.string.create_post_needs_credentials)
                    )
                }
            } else {
                (it as? FloatingActionButton)?.hide()
                findNavController().navigate(R.id.action_dashboardFragment_to_newPostFragment)
            }
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

        // swipe refresh
        swipe_refresh_dashboard.setColorSchemeColors(
            ContextCompat.getColor(
                context!!,
                R.color.colorPrimary
            )
        )
        swipe_refresh_dashboard.setOnRefreshListener(::onRefresh)
        swipe_refresh_dashboard.isRefreshing = true

        nav_view.setNavigationItemSelectedListener(this)

        // empty state
        layout_empty_state.share_app_button.setOnClickListener(::startShareAppIntent)

        retrieveCurrentUserData { user: User? ->
            if (user == null) {
                setupNoUserState()
            } else {
                setupUserWidgets(user)
            }
        }
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

        getBlockedUsers()
        getBlockedPosts()
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
                startShareAppIntent(null)
            }
            R.id.nav_send_feedback -> {
                context?.let {

                    if (!dashboardViewModel.isUserLoggedIn()) {
                        activity?.let { a ->
                            DialogHelper(a).showSignInRequiredDialog(
                                a,
                                resources.getString(R.string.feedback_needs_credentials)
                            )
                        }
                        return@let
                    }

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
                            helper.dialogView.review_edittext.requestFocus()
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
            R.id.nav_contact_us -> {
                // TODO OPEN WEB PAGE TO LEAD TO LANDING PAGE 'Contact Us'
            }
            R.id.nav_sign_out -> {
                if (context == null) return true
                DialogHelper(context!!).showConfirmActionDialog(
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
                if (!dashboardViewModel.isUserLoggedIn()) {
                    activity?.let { a ->
                        DialogHelper(a).showSignInRequiredDialog(
                            a,
                            resources.getString(R.string.sign_in_required_to_perform_actions)
                        )
                    }
                    return
                }
                PostsHelper(this@DashboardFragment).showMoreOptionsPopup(
                    context,
                    view,
                    current,
                    position
                )
            }
            R.id.user_post_comment -> {
                ActivityLaunchHelper.goToPostDetail(
                    activity,
                    current.objectID,
                    view.id == R.id.user_post_comment
                )
            }
            R.id.user_post_media_content_container -> {
                ActivityLaunchHelper.goToMediaViewer(
                    activity,
                    current.photoUrl,
                    current.videoUrl
                )
            }
            R.id.user_post_like -> {
                if (!dashboardViewModel.isUserLoggedIn()) {
                    activity?.let { a ->
                        DialogHelper(a).showSignInRequiredDialog(
                            a,
                            resources.getString(R.string.sign_in_required_to_like_posts)
                        )
                    }
                    return
                }
                val shouldUpdateInitialDataInRoom =
                    retrievedPosts[0]?.any { it.objectID == current.objectID } == true
                if (current.likes.contains(dashboardViewModel.userRepo.getCurrentUser()?.uid)) {
                    unlikePost(current, shouldUpdateInitialDataInRoom) {
                        postsAdapter.notifyItemChanged(
                            position,
                            AdapterPayload.LIKED_OR_UNLIKED_POST
                        )
                    }
                } else {
                    likePost(current, shouldUpdateInitialDataInRoom) {
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
                        activity?.let {
                            SharedPrefManager(it).saveLocation(
                                userLocation!!.latitude.toFloat(),
                                userLocation!!.longitude.toFloat()
                            )
                        }
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

    private fun retrieveCurrentUserData(onUserRetrieved: (User?) -> Unit) {
        val user = dashboardViewModel.userRepo.getCurrentUser()
        if (user == null) {
            onUserRetrieved(null)
            return
        }

        val getUserDataAndBlacklist = Single.zip(
            dashboardViewModel
                .userRepo
                .getUserData(user.uid),
            dashboardViewModel
                .userRepo
                .getMeta(user.uid),
            dashboardViewModel
                .localodgeRepo
                .getBlacklist()
                .onErrorResumeNext {
                    if (it.message == NO_VALUE)
                        return@onErrorResumeNext Single.just(hashSetOf())
                    else
                        return@onErrorResumeNext Single.error(it)
                },
            Function3<User, Meta?, HashSet<String>, Triple<User, Meta?, HashSet<String>>>
            { localodgeUser, meta, blacklist ->
                return@Function3 Triple(localodgeUser, meta, blacklist)
            }
        )

        disposables.addAll(
            getUserDataAndBlacklist
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { error ->
                        handleError(error)
                    },
                    onSuccess = { triple ->
                        val localodgeUser = triple.first
                        val meta = triple.second ?: Meta()
                        val blacklist = triple.third

                        if (meta.suspendedTillDate > 0) {
                            showWarningDialog(
                                resources.getString(R.string.account_suspended),
                                resources.getString(R.string.account_suspended_message)
                            )
                            return@subscribeBy
                        }

                        if (blacklist.contains(user.uid)) {
                            showWarningDialog(
                                resources.getString(R.string.account_banned),
                                resources.getString(R.string.account_banned_message)
                            )
                            return@subscribeBy
                        }

                        onUserRetrieved(localodgeUser)
                    }
                ),
            dashboardViewModel
                .userRepo
                .getNotifications()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onSuccess = { unreadNotifications ->
                        dashboardViewModel.unreadNotifications = unreadNotifications
                        if (unreadNotifications.size > 0)
                            unread_notifs_indicator?.instaVisible()
                    }
                )
        )
    }

    private fun setupUserWidgets(user: User) {
        if (context == null) return
        val headerView = nav_view.inflateHeaderView(R.layout.nav_header_dashboard_signed_in)

        nav_view.menu.getItem(0).subMenu.getItem(3).isVisible = true

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

    private fun setupNoUserState() {
        if (context == null) return
        nav_view.menu.getItem(0).subMenu.getItem(3).isVisible = false

        val headerView = nav_view.inflateHeaderView(R.layout.nav_header_dashboard_no_user)
        headerView.sign_in_button.setOnClickListener {
            ActivityLaunchHelper.goToLogin(activity)
        }
    }

    private fun captureImage() {
        PhotoPicker.captureImage(this)
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
        Log.e(TAG, error.message, error)
        showError(resources.getString(R.string.generic_error_message))
        showProgress(false)
    }

    private fun showError(message: String) {
        Toasty.error(
            context!!,
            message,
            Toast.LENGTH_SHORT, true
        ).show()
    }

    private fun loadInitialDashboardData() {
        // setup recycler view
        postsAdapter = PostsAdapter(
            arrayListOf(),
            this@DashboardFragment,
            Location(
                lat = userLocation!!.latitude,
                lng = userLocation!!.longitude
            )
        )
        dashboard_recyclerview.adapter = postsAdapter
        dashboard_recyclerview.layoutManager = LinearLayoutManager(context)
        dashboard_recyclerview.onScrolledToBottom(::loadMoreDashboardData)

        if (dashboardViewModel.isUserLoggedIn()) {
            if (blockedPosts == null || dashboardViewModel.blockedUsers == null) {
                disposables.addAll(
                    Observable.zip(
                        blockedPostsResult,
                        dashboardViewModel.blockedUsersResult,
                        BiFunction<Status, Status, Pair<Boolean, Boolean>> { s1, s2 ->
                            Pair(s1 == Status.SUCCESS_WITH_DATA, s2 == Status.SUCCESS_WITH_DATA)
                        }
                    )
                        .subscribe { results ->
                            if (results.first && results.second) {
                                loadDashboardDataFromRoom()
                            }
                        }
                )
            }
        } else {
            blockedPosts = hashSetOf()
            dashboardViewModel.blockedUsers = hashSetOf()
            loadDashboardDataFromRoom()
        }
    }

    private fun loadDashboardDataFromRoom() {
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
                                if (roomPost.timestamp != null) {
                                    timestamp = Timestamp(Date(roomPost.timestamp!!))
                                }
                                if (roomPost.lat != null && roomPost.lng != null) {
                                    _geoloc = Location(
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
        var blockedUsers = hashSetOf<String>()
        var blockedPosts = hashSetOf<String>()
        if (dashboardViewModel.isUserLoggedIn()) {
            blockedUsers = dashboardViewModel.blockedUsers!!
            blockedPosts = this.blockedPosts!!
        }
        postsProvider.loadInitial(
            userLocation = userLocation!!,
            radius = radius,
            blockedUsers = blockedUsers,
            blockedPosts = blockedPosts,
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
                    swipe_refresh_dashboard?.isRefreshing = false
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
                            timestamp = postFirebase.timestamp?.seconds?.times(1000)
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

    private fun loadMoreDashboardData() {
        currentPage++
        postsAdapter.appendToList(retrievedPosts[currentPage] ?: arrayListOf())
    }

    private fun onRefresh() {
        if (!this::retrievedPosts.isInitialized) return
        toggleEmptyState(false)
        postsAdapter.clear()
        expandSearchCount = 0
        currentPage = 0
        loadInitialDataFromFirebase(PostsProvider.INITIAL_RADIUS)
    }

    private fun unlikePost(post: PostViewItem, updateInRoom: Boolean, onComplete: () -> Unit) {
        val currUserId = dashboardViewModel.userRepo.getCurrentUser()?.uid ?: return
        post.likes.remove(currUserId)
        updateLikes(post.objectID, post.likes, updateInRoom, onComplete)
    }

    private fun likePost(post: PostViewItem, updateInRoom: Boolean, onComplete: () -> Unit) {
        val currUserId = dashboardViewModel.userRepo.getCurrentUser()?.uid ?: return
        post.likes.add(currUserId)
        updateLikes(post.objectID, post.likes, updateInRoom, onComplete)
    }

    private fun updateLikes(
        postId: String,
        newLikes: HashSet<String>,
        updateInRoom: Boolean,
        onComplete: () -> Unit
    ) {
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

        if (updateInRoom) {
            dashboardViewModel.postsRepo.postsDao.updateLikes(postId, newLikes)
        }
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

    private fun startShareAppIntent(view: View?) {
        activity?.let {
            ShareCompat.IntentBuilder.from(it)
                .setType("text/plain")
                .setChooserTitle("Chooser title")
                .setText("http://play.google.com/store/apps/details?id=" + it.packageName)
                .startChooser()
        }
    }

    private fun showWarningDialog(title: String, message: String) {
        context?.let {
            val dh = DialogHelper(it)
            dh.createDialog(R.layout.dialog_warning)
            dh.setCancelable(false)

            dh.dialogView.warning_title?.text = title
            dh.dialogView.warning_message?.text = message
            dh.dialogView.dialog_login_btn?.setOnClickListener {
                (activity as? LocalodgeActivity)?.logOut()
            }

            dh.dialog.show()
        }
    }

    private fun getBlockedUsers() {
        if (!dashboardViewModel.isUserLoggedIn()) return
        disposables.add(
            dashboardViewModel
                .userRepo
                .getBlocking()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onSuccess = {
                        dashboardViewModel.blockedUsers = it
                        dashboardViewModel.blockedUsersResult.onNext(Status.SUCCESS_WITH_DATA)
                    }
                )
        )
    }

    private fun getBlockedPosts() {
        if (!dashboardViewModel.isUserLoggedIn()) return
        disposables.add(
            dashboardViewModel
                .userRepo
                .getBlockedPosts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onSuccess = {
                        blockedPosts = it
                        blockedPostsResult.onNext(Status.SUCCESS_WITH_DATA)
                    }
                )
        )
    }

    override fun onReportUser(userIdToReport: String, reason: String, desc: String) {
        val userId = dashboardViewModel.userRepo.getCurrentUserId() ?: return
        val report = Report(
            reportedByUserId = userId,
            reason = reason,
            description = desc
        )
        showProgress(true)
        disposables.add(
            dashboardViewModel
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
        val userId = dashboardViewModel.userRepo.getCurrentUserId() ?: return
        val report = Report(
            reportedByUserId = userId,
            reason = reason,
            description = desc
        )
        showProgress(true)
        disposables.add(
            dashboardViewModel
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
            dashboardViewModel
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
        blockedPosts!!.add(postViewItem.objectID)
        val userId = dashboardViewModel.userRepo.getCurrentUserId() ?: return
        disposables.addAll(
            dashboardViewModel
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
                        postsAdapter.data.remove(postViewItem)
                        postsAdapter.notifyItemRemoved(position!!)
                    }
                )
        )

        dashboardViewModel
            .userRepo
            .userDao
            .updateBlockedPosts(userId, blockedPosts!!)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}
