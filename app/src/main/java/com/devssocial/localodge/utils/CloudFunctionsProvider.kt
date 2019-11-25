package com.devssocial.localodge.utils

import io.reactivex.Single
import org.json.JSONObject

object CloudFunctionsProvider {

    fun chargeNewCustomer(rating: Int, tokenId: String, saveCard: Boolean): Single<JSONObject> {
        return Single.just(JSONObject("{data: []}"))

        // todo continue here
        // https://stripe.com/docs/saving-cards
    }

    fun chargeExistingCustomer(customerId: String, rating: Int): Single<JSONObject> {
        return Single.just(JSONObject("{data: []}"))
    }

    fun getPaymentMethods(customerId: String): Single<JSONObject> {
        return Single.just(JSONObject("{data: []}"))

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