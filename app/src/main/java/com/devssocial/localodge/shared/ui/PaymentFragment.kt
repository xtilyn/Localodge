package com.devssocial.localodge.shared.ui


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.devssocial.localodge.R
import com.devssocial.localodge.extensions.popHide
import com.devssocial.localodge.extensions.popShow
import com.devssocial.localodge.models.CustomerInfo
import com.devssocial.localodge.shared.UserRepository
import com.devssocial.localodge.utils.CloudFunctionsProvider
import com.devssocial.localodge.utils.StripeHelper
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import es.dmoral.toasty.Toasty
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_payment.*
import kotlinx.android.synthetic.main.layout_loading_overlay.*
import java.util.*

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
                    } else if (customerInfo != null && customerInfo.sourceId.isNotEmpty()) {
                        this.customerInfo = customerInfo
                        currentState = State.HAS_A_CARD
                        setupHasACardState(customerInfo)
                    } else {
                        currentState = State.NO_CARD
                        setupNoCardState()
                    }
                    continue_button?.popShow()
                    continue_button?.setOnClickListener(::onContinueClick)
                }
        )
    }

    // TODO CONTINUE HERE - FUNCTIONS_Neuwly

    // TODO CONTINUE HERE IF CUSTOMER HAS A CARD/NOT
    // TODO CONTINUE HERE CONFIRM PAYMENT METHOD

    @SuppressLint("DefaultLocale")
    private fun setupHasACardState(customerInfo: CustomerInfo) {
        card_type?.text = customerInfo.cardType.capitalize()
        last_4_digits?.text = customerInfo.las4Digits
        expiry_date?.text = customerInfo.expDate
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
            this.customerInfo ...
        } else {
            if (card_multiline_widget.card == null) {
                handleError(null, getString(R.string.invalid_card_details))
            } else {
                showProgress(true)
                createStripeToken(card_multiline_widget.card!!) { token: Token? ->
                    if (token != null) {
                        CloudFunctionsProvider.sendIncomingCreditCard(

                        ) { customerInfo: CustomerInfo ->
                            disposables.add(
                                userRepo.saveCustomerInfo(customerInfo)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscibreOn(Schedulers.io())
                                    .subscribeBy (
                                        onError = {
                                            asd
                                        },
                                        onComplete = {

                                        }
                                    )
                            )
                        }
                    } else {
                        handleError(null, resources.getString(R.string.generic_error_message))
                    }
                }
            }
        }
    }

    private fun createStripeToken(
        cardToSave: Card,
        onSuccess: (Token?) -> Unit
    ) {
        if (context == null) return
        StripeHelper.getSourceToken(
            context!!,
            cardToSave,
            { onSuccess(it) },
            { handleError(it, resources.getString(R.string.generic_error_message)) }
        )
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
