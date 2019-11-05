package com.devssocial.localodge.ui.settings

import android.graphics.Color
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.devssocial.localodge.LocalodgeActivity
import com.devssocial.localodge.R

class SettingsActivity : LocalodgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        window.statusBarColor = Color.WHITE
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}