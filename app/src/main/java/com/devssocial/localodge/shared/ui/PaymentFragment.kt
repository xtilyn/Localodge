package com.devssocial.localodge.shared.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.*
import com.devssocial.localodge.models.*
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.ui.dashboard.ui.DashboardActivity
import com.devssocial.localodge.utils.*
import com.devssocial.localodge.utils.helpers.ActivityLaunchHelper
import com.devssocial.localodge.utils.helpers.DialogHelper
import com.devssocial.localodge.utils.helpers.PromotionRatingHelper
import com.devssocial.localodge.utils.helpers.StripeHelper
import com.devssocial.localodge.utils.providers.CloudFunctionsProvider
import com.devssocial.localodge.view_holders.PostViewHolder
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.google.gson.Gson
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import es.dmoral.toasty.Toasty
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_order_details.view.*
import kotlinx.android.synthetic.main.fragment_payment.*
import kotlinx.android.synthetic.main.layout_loading_overlay.*
import kotlinx.android.synthetic.main.list_item_user_post.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PaymentFragment : Fragment() {

    private enum class State {
        HAS_A_CARD,
        NO_CARD
    }

    companion object {
        private const val TAG = "PaymentFragment"
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 53
    }

    private val args: PaymentFragmentArgs by navArgs()

    private val disposables = CompositeDisposable()
    private lateinit var userRepo: UserRepository
    private lateinit var currentState: State
    private lateinit var customerInfo: CustomerInfo
    private lateinit var cardDetails: CardDetails
    private lateinit var stripe: Stripe
    private lateinit var pendingPost: Post
    private lateinit var paymentsClient: PaymentsClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val postJson = args.postJson ?: throw Exception("Missing arguments")
        pendingPost = Gson().fromJson(postJson, Post::class.java)

        if (activity == null) return
        paymentsClient = Wallet.getPaymentsClient(
            activity!!,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context == null) return
        isReadyToPay()

        back_button.setOnClickListener { activity?.onBackPressed() }

        userRepo = UserRepository(context!!)
        showProgress(true)
        disposables.add(
            userRepo.getCustomerInfo(userRepo.getCurrentUserId() ?: return)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { customerInfo: CustomerInfo?, err: Throwable? ->
                    showProgress(false)
                    if (err != null) {
                        handleError(err)
                    } else if (customerInfo != null && customerInfo.customerId.isNotEmpty()) {
                        this.customerInfo = customerInfo
                        disposables.add(
                            CloudFunctionsProvider
                                .getPaymentMethods(customerInfo.customerId)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe { response: JSONObject?, error: Throwable? ->
                                    if (error != null || response == null) {
                                        handleError(error)
                                    } else {
                                        val data = response.getJSONArray("data")
                                        if (data.length() > 1) {
                                            cardDetails = StripeHelper.LIST_parseResponse(response)
                                            currentState = State.HAS_A_CARD
                                            setupHasACardState()
                                        }
                                    }

                                }
                        )
                    } else {
                        currentState = State.NO_CARD
                        setupNoCardState()
                    }
                    continue_button?.popShow()
                    continue_button?.setOnClickListener(::reviewOrderDetails)
                }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val paymentData = JSONObject(PaymentData.getFromIntent(data!!)!!.toJson())
                        // TODO DISPLAY CARD DETAILS
                        // You can get some data on the user's card, such as the
                        // brand and last 4 digits
//                        val info = paymentData!!.cardInfo
                        // You can also pull the user address from the
                        // PaymentData object.
//                        val address = paymentData.shippingAddress
                        // This is the raw JSON string version of your Stripe token.
                        val rawToken = paymentData
                            .getJSONObject("paymentMethodToken")
                            .getString("token")

                        // Now that you have a Stripe token object,
                        // charge that by using the id
                        val stripeToken = Token.fromString(rawToken)
                        if (stripeToken != null) {
                            // This chargeToken function is a call to your own
                            // server, which should then connect to Stripe's
                            // API to finish the charge.
                            chargeToken(stripeToken.id, false)
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        // Log the status for debugging
                        // Generally there is no need to show an error to
                        // the user as the Google Payment API will do that
                        val status = AutoResolveHelper.getStatusFromIntent(data)
                        Log.e(TAG, "onActivityResult: google pay failed with status: $status")
                    }
                    else -> {
                        // Do nothing.
                    }
                }
            }
            else -> {
                // Handle any other startActivityForResult calls you may have made.
            }
        }
    }

    private fun isReadyToPay() {
        paymentsClient.isReadyToPay(createIsReadyToPayRequest())
            .addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        pay_with_google_pay?.popShow()
                        pay_with_google_pay?.setOnClickListener(::payWithGooglePay)
                    } else {
                        pay_with_google_pay?.instaGone()
                    }
                } catch (exception: ApiException) {
                }
            }
    }

    private fun createIsReadyToPayRequest(): IsReadyToPayRequest {
        return IsReadyToPayRequest.fromJson(
            JSONObject()
                .put(
                    "allowedAuthMethods", JSONArray()
                        .put("PAN_ONLY")
                        .put("CRYPTOGRAM_3DS")
                )
                .put(
                    "allowedCardNetworks",
                    JSONArray()
                        .put("AMEX")
                        .put("DISCOVER")
                        .put("JCB")
                        .put("MASTERCARD")
                        .put("VISA")
                )
                .toString()
        )
    }

    @SuppressLint("DefaultLocale")
    private fun setupHasACardState() {
        card_type?.text = cardDetails.brand.capitalize()
        last_4_digits?.text = cardDetails.last4
        val expFormatted = "${cardDetails.exp_month}/${cardDetails.exp_year}"
        expiry_date?.text = expFormatted
        credit_card_info?.popShow()
    }

    private fun setupNoCardState() {
        new_card?.popShow()
    }

    private fun reviewOrderDetails(view: View) {
        if (context == null) return
        if (card_multiline_widget.card == null) {
            Toasty.error(context!!, getString(R.string.invalid_card_details)).show()
            return
        }

        val dh = DialogHelper(context!!)
        dh.createDialog(R.layout.dialog_order_details)
        dh.dialogView.list_item_user_post_container.user_post_like.instaGone()
        dh.dialogView.list_item_user_post_container.user_post_comment.instaGone()

        disposables.add(
            Single.create<Pair<User, Location>> { e ->
                val location = SharedPrefManager(activity).getLocation()
                if (location == null) {
                    e.onError(Throwable("Missing Location"))
                    return@create
                }

                val userId = userRepo.getCurrentUserId()!!
                lateinit var user: User
                user = try {
                    userRepo.userDao.getUser(userId).blockingGet().mapProperties(User())
                } catch (exception: Exception) {
                    try {
                        userRepo.getUserData(userId).blockingGet()
                    } catch (exception2: Exception) {
                        e.onError(exception2)
                        return@create
                    }
                }

                e.onSuccess(Pair(user, location))
            }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { handleError(it) },
                    onSuccess = {
                        if (context == null) return@subscribeBy
                        val pendingPostViewItem = pendingPost.mapProperties(PostViewItem())
                        pendingPostViewItem.posterProfilePic = it.first.profilePicUrl
                        pendingPostViewItem.posterUsername = it.first.username

                        PostViewHolder.bindItem(
                            pendingPostViewItem,
                            dh.dialogView.list_item_user_post_container,
                            it.second
                        )
                        dh.dialogView.promotion_method.text =
                            PromotionRatingHelper.titles[pendingPost.rating]
                        val formattedTotalPrice =
                            "${PromotionRatingHelper.getPriceInFormattedString(
                                PromotionRatingHelper.prices[pendingPost.rating]!!
                            )} USD"
                        dh.dialogView.total_price.text = formattedTotalPrice

                        // listeners
                        val closeDialog = View.OnClickListener { dh.dialog.dismiss() }
                        dh.dialogView.proceed_order.setOnClickListener {
                            dh.dialog.dismiss()
                            proceedOrder()
                        }
                        dh.dialogView.cancel_order.setOnClickListener(closeDialog)
                        dh.dialogView.close_dialog.setOnClickListener(closeDialog)

                        dh.dialog.show()
                    }
                )
        )
    }

    private fun proceedOrder() {
        if (currentState == State.HAS_A_CARD) {
            showProgress(true)
            disposables.add(
                CloudFunctionsProvider.chargeExistingCustomer(
                    customerId = customerInfo.customerId,
                    rating = pendingPost.rating
                ).subscribeBy(
                    onError = { handleError(it) },
                    onSuccess = ::onPaymentSuccess
                )
            )
        } else {
            val card = card_multiline_widget.card
            if (card == null) {
                handleError(null, getString(R.string.invalid_card_details))
            } else {
                showProgress(true)
                startCheckout(card)
            }
        }
    }

    private fun startCheckout(card: Card) {
        if (activity == null) return
        ScreenUtils.disableTouch(activity!!)
        val params = card_multiline_widget.paymentMethodCreateParams
        if (params != null) {
            ScreenUtils.disableTouch(activity)
            // Create a Stripe token from the card details
            stripe = Stripe(
                activity!!.applicationContext,
                PaymentConfiguration.getInstance(activity!!.applicationContext).publishableKey
            )
            createCardToken(card, UUID.randomUUID().toString(), 5)
        }
    }

    private fun createCardToken(card: Card, idempotentKey: String, retriesLeft: Int) {
        stripe.createCardToken(
            card,
            idempotentKey,
            object : ApiResultCallback<Token> {
                override fun onSuccess(result: Token) {
                    val tokenID = result.id
                    chargeToken(tokenID, save_card_checkbox.isChecked)
                }

                override fun onError(e: java.lang.Exception) {
                    if (retriesLeft > 0) {
                        createCardToken(card, idempotentKey, retriesLeft + 1)
                    } else {
                        ScreenUtils.enableTouch(activity)
                        handleError(e)
                    }
                }
            })
    }

    private fun chargeToken(tokenId: String, saveCard: Boolean) {
        disposables.add(
            CloudFunctionsProvider.chargeNewCustomer(
                pendingPost.rating,
                tokenId,
                saveCard
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = {
                        handleError(it)
                    },
                    onSuccess = ::onPaymentSuccess
                )
        )
    }

    private fun onPaymentSuccess(response: JSONObject) {
        ScreenUtils.enableTouch(activity)
        if (response.isNull("error")) {
            val data = response.getJSONObject("data")
            if (!data.isNull("customerId")) {
                saveCustomerId(data.getString("customerId"))
            }
            context?.let { c ->
                Toasty.success(c, getString(R.string.payment_succeeded))
                DialogHelper(c).showInfoDialog(getString(R.string.pending_post_message)) {
                    findNavController().navigate(R.id.action_paymentFragment_to_dashboardFragment)
                }
            }
        } else {
            handleError(null)
        }
    }

    private fun saveCustomerId(customerId: String) {
        userRepo.saveCustomerId(customerId)
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    private fun showProgress(show: Boolean) {
        if (show) loading_overlay?.popShow()
        else loading_overlay?.gone()
    }

    private fun handleError(
        error: Throwable?,
        errorMessage: String = getString(R.string.generic_error_message)
    ) {
        showProgress(false)
        error?.let { Log.e(TAG, error.message, error) }
        context?.let {
            Toasty.error(it, errorMessage)
        }
    }

    private fun payWithGooglePay(view: View) {
        if (context == null) return
        disposables.add(
            CloudFunctionsProvider
                .getPaymentDataRequest(context!!, pendingPost.rating)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onError = { handleError(it) },
                    onSuccess = { paymentDataRequest: PaymentDataRequest ->
                        if (activity == null) return@subscribeBy
                        AutoResolveHelper.resolveTask(
                            paymentsClient.loadPaymentData(paymentDataRequest),
                            activity!!,
                            LOAD_PAYMENT_DATA_REQUEST_CODE
                        )
                    }
                )
        )
    }

}
