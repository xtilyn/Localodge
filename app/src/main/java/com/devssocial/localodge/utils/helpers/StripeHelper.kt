package com.devssocial.localodge.utils.helpers

import android.content.Context
import com.devssocial.localodge.models.CardDetails
import com.devssocial.localodge.utils.providers.ApiKeysProviders
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import org.json.JSONObject

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

    fun LIST_parseResponse(jsonObject: JSONObject): CardDetails {
        val data = jsonObject.getJSONArray("data")
        val card = (data.get(0) as JSONObject).getJSONObject("card")
        return CardDetails(
            brand = card.getString("brand"),
            country = card.getString("country"),
            exp_month = card.getInt("exp_month"),
            exp_year = card.getInt("exp_year"),
            last4 = card.getString("last4")
        )
    }

}