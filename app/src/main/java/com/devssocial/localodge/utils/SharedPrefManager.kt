package com.devssocial.localodge.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.devssocial.localodge.LOCALODGE_SHARED_PREF
import com.devssocial.localodge.models.Location

class SharedPrefManager(private val activity: Activity?) {

    companion object {
        const val LAT = "lat"
        const val LNG = "lng"
    }

    private val pref: SharedPreferences? by lazy {
        activity?.getSharedPreferences(
            LOCALODGE_SHARED_PREF,
            Context.MODE_PRIVATE
        )
    }

    fun saveLocation(lat: Float, lng: Float) {
        if (pref == null) return
        with(pref!!.edit()) {
            putFloat(LAT, lat)
            putFloat(LNG, lng)
            apply()
        }
    }

    fun getLocation(): Location? {
        val lat = pref?.getFloat(LAT, 0f) ?: return null
        val lng = pref?.getFloat(LNG, 0f) ?: return null
        return Location(
            lat = lat.toDouble(),
            lng = lng.toDouble()
        )
    }

}