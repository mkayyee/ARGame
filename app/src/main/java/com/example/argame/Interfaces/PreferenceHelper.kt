package com.example.argame.Interfaces

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson

object PreferenceHelper {

    val GSON = "GSON"

    fun defaultPreference(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun customPreference(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
        val editMe = edit()
        operation(editMe)
        editMe.apply()
    }

    inline fun SharedPreferences.Editor.put(pair: Pair<String, Any?>) {
        val key = pair.first
        val value = pair.second
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            else -> error("Only primitive types can be stored in SharedPreferences")
        }
    }

    var SharedPreferences.setGson
        get() = getString(GSON, "")
        set(value) {
            editMe {
                it.put(GSON to value) //it.putString(USER_PASSWORD, value)
            }
        }

    var SharedPreferences.clearValues
        get() = { }
        set(value) {
            editMe {
                it.remove(GSON)
                //it.clear()
            }
        }

}