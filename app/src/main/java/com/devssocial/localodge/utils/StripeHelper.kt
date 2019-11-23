package com.devssocial.localodge.utils

import android.content.Context
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.Token

object StripeHelper {

    fun getSourceToken(
        context: Context,
        cardToSave: Card,
        onSuccess: (Token?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        ApiKeysProviders.getStripeKey {
            val stripe = Stripe(context, it)
            stripe.createCardToken(cardToSave, null, object: ApiResultCallback<Token> {
                override fun onError(e: Exception) {
                    onError(e)
                }
                override fun onSuccess(result: Token) {
                    onSuccess(result)
                }
            })
        }
    }

}