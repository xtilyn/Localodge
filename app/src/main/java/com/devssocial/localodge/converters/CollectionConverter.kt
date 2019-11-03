package com.devssocial.localodge.converters

import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import androidx.room.TypeConverter
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter

public class CollectionConverter {

    companion object {
        private const val TAG = "CollectionConverter"
    }

    @TypeConverter
    public fun fromStringSet(strings: Set<String>?): String? {
        if (strings == null) {
            return null
        }

        val result = StringWriter()
        val json = JsonWriter(result)

        try {
            json.beginArray()

            for (s in strings) {
                json.value(s)
            }

            json.endArray()
            json.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception creating JSON", e)
        }

        return result.toString()
    }

    @TypeConverter
    public fun toStringSet(strings: String?): Set<String>? {
        if (strings == null) {
            return null
        }

        val reader = StringReader(strings)
        val json = JsonReader(reader)
        val result = hashSetOf<String>()

        try {
            json.beginArray()

            while (json.hasNext()) {
                result.add(json.nextString())
            }

            json.endArray()
        } catch (e: IOException) {
            Log.e(TAG, "Exception parsing JSON", e)
        }

        return result
    }
}