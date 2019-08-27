package com.devssocial.localodge.ui.dashboard.ui


import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.devssocial.localodge.NO_VALUE
import com.devssocial.localodge.R
import com.devssocial.localodge.models.User
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.*

class DashboardFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {

    private val disposables = CompositeDisposable()
    private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashboardViewModel = ViewModelProviders.of(activity!!)[DashboardViewModel::class.java]
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

    private fun retrieveCurrentUserData() {
        val user = dashboardViewModel.getCurrentUser()
        if (dashboardViewModel.getCurrentUser() == null) {
            ActivityLaunchHelper.goToLogin(activity)
            return
        }
        disposables.add(
            dashboardViewModel
                .getUserData(user!!.uid)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { error ->
                        handleError(error)
                    },
                    onSuccess = { user ->
                        setupUserWidgets(user)
                    }
                )
        )
    }

    private fun setupUserWidgets(user: User) {
        val usernameFormat = "@${user.username}"
        nav_view.getHeaderView(0).findViewById<TextView>(R.id.username_text_view).text = usernameFormat

        if (user.profilePicUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profilePicUrl)
                .into(nav_view.getHeaderView(0).findViewById(R.id.user_profile_pic_image_view))
        }
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

}
