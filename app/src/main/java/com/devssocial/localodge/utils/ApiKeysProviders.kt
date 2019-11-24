package com.devssocial.localodge.utils

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import io.reactivex.Single

object ApiKeysProviders : CloudFunctionsProvider(){

    private const val GET_STRIPE_KEY = "getStripeKey"

    override fun getStripeKey(onSuccess: (String) -> Unit) {
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