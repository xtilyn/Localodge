package com.devssocial.localodge.ui.dashboard.ui


import android.Manifest
import android.app.Activity
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.algolia.search.saas.Client
import com.algolia.search.saas.Index
import com.bumptech.glide.Glide
import com.devssocial.localodge.*
import com.devssocial.localodge.R
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.models.User
import com.devssocial.localodge.ui.dashboard.utils.PostsUtil
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.DocumentSnapshot
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.*
import org.imperiumlabs.geofirestore.GeoFirestore
import org.json.JSONObject
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class DashboardFragment :
    Fragment(),
    NavigationView.OnNavigationItemSelectedListener,
    EasyPermissions.PermissionCallbacks {

    companion object {
        private const val TAG = "DashboardFragment"
        const val REQUEST_LOCATION_PERMISSION = 1
        const val REQUEST_CHECK_SETTINGS = 2
        const val NEARBY_RADIUS = 1000000 // 100km
        const val HITS_PER_PAGE = 10
    }

    private val disposables = CompositeDisposable()
    private var userLocation: Location? = null

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboardViewModel = ViewModelProviders.of(activity!!)[DashboardViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
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

        // TODO SETUP RECYCLERVIEW

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onStart() {
        super.onStart()

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_home -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_tools -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }
        drawer_layout?.closeDrawer(GravityCompat.START)
        return true
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
                        loadDashboardData()
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
                loadDashboardData()
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
        val user = dashboardViewModel.getCurrentUser()
        if (dashboardViewModel.getCurrentUser() == null) {
            ActivityLaunchHelper.goToLogin(activity)
            return
        }
        disposables.addAll(
            dashboardViewModel
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
        headerView.findViewById<TextView>(R.id.username_text_view).text = usernameFormat

        if (user.profilePicUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profilePicUrl)
                .into(headerView.findViewById(R.id.user_profile_pic_image_view))
        }
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

    private fun loadDashboardData() {
        Log.d(this::class.java.simpleName, "user location: $userLocation")
        if (userLocation == null) return
        dashboardViewModel.loadDataAroundLocation(
            userLocation!!,
            object : GeoFirestore.SingleGeoQueryDataEventCallback {
                override fun onComplete(
                    documentSnapshots: List<DocumentSnapshot>?,
                    exception: Exception?
                ) {
                    if (exception != null) {
                        (activity as LocalodgeActivity).logAndShowError(
                            TAG,
                            exception,
                            resources.getString(R.string.error_retrieving_data)
                        )
                    } else {
                        val unorderedPosts = documentSnapshots?.map {
                            it.toObject(Post::class.java) ?: return
                        } ?: return
                        lateinit var orderedPosts: ArrayList<Post>
                        synchronized(this) {
                            orderedPosts = PostsUtil.orderPosts(unorderedPosts) as ArrayList<Post>
                        }
                        postsAdapter.updateList(orderedPosts)
                    }
                }
            })
    }

}
