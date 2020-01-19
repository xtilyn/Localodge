package com.devssocial.localodge.converters

import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*
import kotlin.collections.HashSet

public class RoomConverter {

    companion object {
        private const val TAG = "CollectionConverter"

        @TypeConverter
        @JvmStatic
        public fun fromTimeStamp(timestamp: Timestamp): Long = timestamp.seconds * 1000L

        @TypeConverter
        @JvmStatic
        public fun toTimeStamp(millis: Long): Timestamp = Timestamp(Date(millis))

        @TypeConverter
        @JvmStatic
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
        @JvmStatic
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

        @TypeConverter
        @JvmStatic
        public fun fromStringMap(strings: HashMap<String, Boolean>?): HashSet<String>? {
            if (strings == null) {
                return null
            }
            return strings.keys.toHashSet()
        }
    }

}