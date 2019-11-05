package com.devssocial.localodge.ui.dashboard.ui


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.devssocial.localodge.R
import com.devssocial.localodge.ui.dashboard.view_model.DashboardViewModel
import com.devssocial.localodge.utils.KeyboardUtils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_new_post.*

class NewPostFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private val disposables = CompositeDisposable()

    private val newPostClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.back_button -> {
                activity?.onBackPressed()
            }
            R.id.post_button -> {
                onPostButtonClick()
            }
            R.id.take_photo -> {

            }
            R.id.post_gallery -> {

            }
            R.id.promote_post -> {

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
        post_button.setOnClickListener(newPostClickListener)
        post_gallery.setOnClickListener(newPostClickListener)
        take_photo.setOnClickListener(newPostClickListener)
        promote_post.setOnClickListener(newPostClickListener)

        post_description_edit_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    post_button.setCardBackgroundColor(
                        ContextCompat.getColor(context!!, R.color.colorPrimary)
                    )
                } else {
                    post_button.setCardBackgroundColor(
                        ContextCompat.getColor(context!!, R.color.lightGray)
                    )
                }
            }

        })
    }

    override fun onStart() {
        super.onStart()
        // TODO load user profile pic and username in their corresponding widgets
        // grab from ROOM. If failed/empty, grab from firebase
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    private fun onPostButtonClick() {
        // TODO CONTINUE HERE
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
}
