package com.devssocial.localodge.extensions

import android.util.Log
import kotlin.reflect.KMutableProperty

/**
 * Map properties of object to the specified object instance
 * @param resultingObj object instance
 */
fun <T : Any> Any.mapProperties(resultingObj: T): T {
    for (prop in this::class.declaredMemberProperties) {
        val name = prop.name
        val value = prop.getter.call(this)

        for (resultProp in resultingObj::class.declaredMemberProperties) {
            if (resultProp.name == name) {
                try {
                    (resultProp as? KMutableProperty<*>)?.setter?.call(resultingObj, value)
                } catch (e: Exception) {
                    Log.d("ObjectExt", "mapProperties() failed to map $name")
                    e.printStackTrace()
                }
            }
        }
    }
    return resultingObj
}

fun Any.setPropValue(propName: String, value: Any) {
    for (prop in this::class.declaredMemberProperties) {
        if (prop.name == propName) {
            (prop as? KMutableProperty<*>)?.setter?.call(this, value)
        }
    }
}

fun Any.getPropValue(propName: String): Any? {
    for (prop in this::class.declaredMemberProperties) {
        if (prop.name == propName) return prop.getter.call(this)
    }
    return null
}

fun Any.toMap(): Map<String, Any> {
    val result = hashMapOf<String, Any>()
    for (prop in this::class.declaredMemberProperties) {
        try {
            val value = prop.getter.call(this)
            result[prop.name] = value!!
        } catch (e: Exception) {
            Log.d("ObjectExt", "failed to call property's getter")
            e.printStackTrace()
        }
    }
    return result
}
