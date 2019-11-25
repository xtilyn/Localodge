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
import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.popHide
import com.devssocial.localodge.extensions.popShow
import com.devssocial.localodge.models.CardDetails
import com.devssocial.localodge.models.CustomerInfo
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.utils.ApiKeysProviders
import com.devssocial.localodge.utils.CloudFunctionsProvider
import com.devssocial.localodge.utils.ScreenUtils
import com.devssocial.localodge.utils.StripeHelper
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_payment.*
import kotlinx.android.synthetic.main.layout_loading_overlay.*
import org.json.JSONObject
import java.lang.ref.WeakReference

class PaymentFragment : Fragment() {

    private enum class State {
        HAS_A_CARD,
        NO_CARD
    }

    companion object {
        private const val TAG = "PaymentFragment"
    }

    private val disposables = CompositeDisposable()
    private lateinit var userRepo: UserRepository
    private lateinit var currentState: State
    private lateinit var customerInfo: CustomerInfo
    private lateinit var cardDetails: CardDetails
    private lateinit var stripe: Stripe

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
                            CloudFunctionsProvider()
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
        if (currentState == State.HAS_A_CARD) {
            showProgress(true)
            // TODO CONTINUE HERE CALL STRIPE CLOUD FUNC
        } else {
            if (card_multiline_widget.card == null) {
                handleError(null, getString(R.string.invalid_card_details))
            } else {
                showProgress(true)
                ApiKeysProviders.getStripeKey {
                    startCheckout(it)
                }
            }
        }
    }

    private fun startCheckout(clientSecret: String) {
        if (activity == null) return
        ScreenUtils.disableTouch(activity!!)
        val params = card_multiline_widget.paymentMethodCreateParams
        if (params != null) {
            // TODO CONTINUE HERE REPLACE LOGIC
            // https://stripe.com/docs/payments/accept-a-payment-charges
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                params, clientSecret, null, true,
                mapOf("setup_future_usage" to "on_session")
            )
            stripe = Stripe(
                activity!!.applicationContext,
                PaymentConfiguration.getInstance(activity!!.applicationContext).publishableKey
            )
            stripe.confirmPayment(this, confirmParams)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (activity != null) return

        // Handle the result of stripe.confirmPayment
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {

                val paymentIntent = result.intent
                val status = paymentIntent.status
                if (status == StripeIntent.Status.Succeeded) {
                    context?.let { c ->
                        Toasty.success(c, getString(R.string.payment_succeeded))
                    }

                    saveToUsersPaymentHistory(result.intent)
                    if (save_card_checkbox.isChecked) {
                        disposables.add(
                            CloudFunctionsProvider().saveCard(
                                result.intent.paymentMethodId!!,
                                customerInfo.customerId
                            )
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribeBy(
                                    onError = { error ->
                                        Log.e(TAG, error.message, error)
                                    },
                                    onComplete = {
                                        Log.d(TAG, "card details successfully saved in stripe.")
                                    }
                                )
                        )
                    }
                } else {
                    if (paymentIntent.lastPaymentError != null) {
                        Log.e(
                            TAG,
                            paymentIntent.lastPaymentError!!.message
                        )
                    }
                    context?.let { c->
                        Toasty.error(c, getString(R.string.payment_failed))
                    }
                }

                ScreenUtils.enableTouch(activity!!)
            }

            override fun onError(e: Exception) {
                ScreenUtils.enableTouch(activity!!)
                handleError(e, getString(R.string.payment_failed))
            }
        })
    }

    private fun saveToUsersPaymentHistory(paymentIntent: PaymentIntent) {
        // TODO CONTINUE HERE
    }

    private fun showProgress(show: Boolean) {
        if (show) loading_overlay?.popShow()
        else loading_overlay?.popHide()
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
