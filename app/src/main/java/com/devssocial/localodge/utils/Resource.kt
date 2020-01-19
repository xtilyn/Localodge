package com.devssocial.localodge.utils

import android.content.Intent
import androidx.annotation.Keep
import com.devssocial.localodge.enums.Status

@Keep
data class Resource<out T>(val status: Status, val data: T?, val error: String?, val intent: Intent?) {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, null, null)
        }

        fun <T> error(error: String): Resource<T> {
            return Resource(Status.ERROR, null, error, null)
        }

        fun <T> loading(): Resource<T> {
            return Resource(Status.LOADING, null, null, null)
        }
    }
}