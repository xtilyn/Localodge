package com.devssocial.localodge.utils

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import io.reactivex.Single

object ApiKeysProviders {

    private const val GET_STRIPE_KEY = "getStripeKey"

    fun getStripeKey(): Single<HttpsCallableResult> {
        val task = FirebaseFunctions.getInstance().getHttpsCallable(GET_STRIPE_KEY).call()
        return Single.create { emitter ->
            try {
                val httpsCallableResult = Tasks.await(task)
                emitter.onSuccess(httpsCallableResult)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

}