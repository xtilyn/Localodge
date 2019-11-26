package com.devssocial.localodge.utils.providers

object ApiKeysProviders {

    fun getStripeKey(onSuccess: (String) -> Unit) {
        CloudFunctionsProvider.getStripeKey(
            onSuccess
        )
    }

}