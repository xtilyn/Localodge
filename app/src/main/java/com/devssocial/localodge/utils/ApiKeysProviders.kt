package com.devssocial.localodge.utils

object ApiKeysProviders {

    fun getStripeKey(onSuccess: (String) -> Unit) {
        CloudFunctionsProvider.getStripeKey(onSuccess)
    }

}