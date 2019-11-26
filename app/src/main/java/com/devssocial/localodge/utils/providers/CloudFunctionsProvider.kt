package com.devssocial.localodge.utils.providers

import android.content.Context
import com.google.android.gms.wallet.PaymentDataRequest
import com.stripe.android.GooglePayConfig
import io.reactivex.Single
import org.json.JSONArray
import org.json.JSONObject

object CloudFunctionsProvider {

    /**
     * Retrieves PaymentDataRequest representing a request method of
     * payment (such as credit card) and other details. This method is
     * used for Google Pay.
     * @param rating post promotion rating
     * @return PaymentDataRequest in JSON representation
     */
    fun getPaymentDataRequest(context: Context, rating: Int): Single<PaymentDataRequest> {
        // todo continue here
        // create PaymentMethod
        val cardPaymentMethod = JSONObject()
            .put("type", "CARD")
            .put(
                "parameters",
                JSONObject()
                    .put("allowedAuthMethods", JSONArray()
                        .put("PAN_ONLY")
                        .put("CRYPTOGRAM_3DS"))
                    .put("allowedCardNetworks",
                        JSONArray()
                            .put("AMEX")
                            .put("DISCOVER")
                            .put("JCB")
                            .put("MASTERCARD")
                            .put("VISA"))

                    // require billing address
                    .put("billingAddressRequired", true)
                    .put(
                        "billingAddressParameters",
                        JSONObject()
                            // require full billing address
                            .put("format", "FULL")

                            // require phone number
                            .put("phoneNumberRequired", true)
                    )
            )
            .put("tokenizationSpecification",
                GooglePayConfig(context).tokenizationSpecification)

        // create PaymentDataRequest
        val paymentDataRequest = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put("allowedPaymentMethods",
                JSONArray().put(cardPaymentMethod))
            .put("transactionInfo", JSONObject()
                .put("totalPrice", "10.00")
                .put("totalPriceStatus", "FINAL")
                .put("currencyCode", "USD")
            )
            .put("merchantInfo", JSONObject()
                .put("merchantName", "Example Merchant"))

            // require email address
            .put("emailRequired", true)
            .toString()

        return Single.just(PaymentDataRequest.fromJson(paymentDataRequest))
        // translate this to javascript and put to cloud functions
        //private fun createPaymentDataRequest(): JSONObject {
        //    // create PaymentMethod
        //    val cardPaymentMethod = JSONObject()
        //        .put("type", "CARD")
        //        .put(
        //            "parameters",
        //            JSONObject()
        //                .put("allowedAuthMethods", JSONArray()
        //                    .put("PAN_ONLY")
        //                    .put("CRYPTOGRAM_3DS"))
        //                .put("allowedCardNetworks",
        //                    JSONArray()
        //                        .put("AMEX")
        //                        .put("DISCOVER")
        //                        .put("JCB")
        //                        .put("MASTERCARD")
        //                        .put("VISA"))
        //
        //                // require billing address
        //                .put("billingAddressRequired", true)
        //                .put(
        //                    "billingAddressParameters",
        //                    JSONObject()
        //                        // require full billing address
        //                        .put("format", "FULL")
        //
        //                        // require phone number
        //                        .put("phoneNumberRequired", true)
        //                )
        //        )
        //        .put("tokenizationSpecification",
        //            GooglePayConfig().tokenizationSpecification)
        //
        //    // create PaymentDataRequest
        //    val paymentDataRequest = JSONObject()
        //        .put("apiVersion", 2)
        //        .put("apiVersionMinor", 0)
        //        .put("allowedPaymentMethods",
        //            JSONArray().put(cardPaymentMethod))
        //        .put("transactionInfo", JSONObject()
        //            .put("totalPrice", "10.00")
        //            .put("totalPriceStatus", "FINAL")
        //            .put("currencyCode", "USD")
        //        )
        //        .put("merchantInfo", JSONObject()
        //            .put("merchantName", "Example Merchant"))
        //
        //        // require email address
        //        .put("emailRequired", true)
        //        .toString()
        //
        //    return PaymentDataRequest.fromJson(paymentDataRequest)
        //}
    }

    fun chargeNewCustomer(rating: Int, tokenId: String, saveCard: Boolean): Single<JSONObject> {
        return Single.just(JSONObject("{data: {}}"))

        // todo continue here
        // https://stripe.com/docs/saving-cards
    }

    fun chargeExistingCustomer(customerId: String, rating: Int): Single<JSONObject> {
        return Single.just(JSONObject("{data: {}}"))

        // todo continue here
    }

    fun getPaymentMethods(customerId: String): Single<JSONObject> {
        return Single.just(JSONObject("{data: {}}"))

        // https://stripe.com/docs/api/payment_methods/list
        // todo continue here call a cloud function that performs the following:
        // var stripe = require('stripe')('sk_test_4eC39HqLyjWDarjtT1zdp7dc');
        //
        //stripe.paymentMethods.list(
        //  {customer: 'cus_GF37ZpUeL7QFG6', type: 'card'},
        //  function(err, paymentMethods) {
        //    // asynchronously called
        //  }
        //);
        // reponse:
        //{
        //  "object": "list",
        //  "url": "/v1/payment_methods",
        //  "has_more": false,
        //  "data": [
        //    {
        //      "id": "pm_1FiZ1Q2eZvKYlo2CckKUpLd1",
        //      "object": "payment_method",
        //      "billing_details": {
        //        "address": {
        //          "city": null,
        //          "country": null,
        //          "line1": null,
        //          "line2": null,
        //          "postal_code": null,
        //          "state": null
        //        },
        //        "email": null,
        //        "name": null,
        //        "phone": null
        //      },
        //      "card": {
        //        "brand": "visa",
        //        "checks": {
        //          "address_line1_check": null,
        //          "address_postal_code_check": null,
        //          "cvc_check": null
        //        },
        //        "country": "US",
        //        "exp_month": 8,
        //        "exp_year": 2020,
        //        "fingerprint": "Xt5EWLLDS7FJjR1c",
        //        "funding": "credit",
        //        "generated_from": null,
        //        "last4": "4242",
        //        "three_d_secure_usage": {
        //          "supported": true
        //        },
        //        "wallet": null
        //      },
        //      "created": 1574655480,
        //      "customer": null,
        //      "livemode": false,
        //      "metadata": {},
        //      "type": "card"
        //    },
        //    {...},
        //    {...}
        //  ]
        //}
    }

    fun getStripeKey(onSuccess: (String) -> Unit) {
        onSuccess("pk_test_TYooMQauvdEDq54NiTphI7jx")
        // TODO CONTINUE HERE CONFIGURE STRIPE IN SERVER
//        val task = FirebaseFunctions.getInstance().getHttpsCallable(GET_STRIPE_KEY).call()
//        return Single.create { emitter ->
//            try {
//                val httpsCallableResult = Tasks.await(task)
//                emitter.onSuccess(httpsCallableResult.data as String)
//            } catch (e: Exception) {
//                emitter.onError(e)
//            }
//        }
    }
}