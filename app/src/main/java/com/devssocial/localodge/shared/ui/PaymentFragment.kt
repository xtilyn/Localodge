package com.devssocial.localodge.shared.ui


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.gone
import com.devssocial.localodge.extensions.popHide
import com.devssocial.localodge.extensions.popShow
import com.devssocial.localodge.models.CardDetails
import com.devssocial.localodge.models.CustomerInfo
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.utils.ActivityLaunchHelper
import com.devssocial.localodge.utils.CloudFunctionsProvider
import com.devssocial.localodge.utils.ScreenUtils
import com.devssocial.localodge.utils.StripeHelper
import com.google.gson.Gson
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_payment.*
import kotlinx.android.synthetic.main.layout_loading_overlay.*
import org.json.JSONObject
import java.util.*

class PaymentFragment : Fragment() {

    private enum class State {
        HAS_A_CARD,
        NO_CARD
    }

    companion object {
        private const val TAG = "PaymentFragment"
    }

    private val args: PaymentFragmentArgs by navArgs()

    private val disposables = CompositeDisposable()
    private lateinit var userRepo: UserRepository
    private lateinit var currentState: State
    private lateinit var customerInfo: CustomerInfo
    private lateinit var cardDetails: CardDetails
    private lateinit var stripe: Stripe
    private lateinit var pendingPost: Post

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val postJson = args.postJson ?: throw Exception("Missing arguments")
        pendingPost = Gson().fromJson(postJson, Post::class.java)
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
                    continue_button?.setOnClickListener(::onContinueClick)
                }
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

    private fun onContinueClick(view: View) {
        if (context == null) return

        // TODO CONTINUE HERE LET CUSTOMER REVIEW DETAILS FIRST BEFORE PROCEED

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
            stripe.createCardToken(
                card,
                UUID.randomUUID().toString(),
                object : ApiResultCallback<Token> {
                    override fun onSuccess(result: Token) {
                        val tokenID = result.id
                        disposables.add(
                            CloudFunctionsProvider.chargeNewCustomer(
                                pendingPost.rating,
                                tokenID,
                                save_card_checkbox.isChecked
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

                    override fun onError(e: java.lang.Exception) {
                        ScreenUtils.enableTouch(activity)
                        handleError(e)
                    }
                })

        }
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
            }
            ActivityLaunchHelper.goToPostDetail(
                activity, pendingPost.objectID, false
            )
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

}
