package io.r_a_d.radio2.preferences

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.r_a_d.radio2.R
import io.r_a_d.radio2.preferenceStore
import io.r_a_d.radio2.ui.songs.request.Requestor

class CustomizeFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.customize_preferences, rootKey)

        val userNamePref = preferenceScreen.findPreference<EditTextPreference>("userName")
        userNamePref!!.summary = userNamePref.text
        userNamePref.setOnPreferenceChangeListener { preference, newValue ->
            val name = newValue as String
            preference.summary = name
            Requestor.instance.initFavorites(name) // need to be as parameter cause the callback is called BEFORE PARAMETER SET
            true
        }

        val snackbarPersistent = preferenceScreen.findPreference<SwitchPreferenceCompat>("snackbarPersistent")
        snackbarPersistent!!.summary = if (preferenceStore.getBoolean("snackbarPersistent", true))
            getString(R.string.snackbarPersistent)
        else
            getString(R.string.snackbarNonPersistent)
        snackbarPersistent.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean)
                preference.setSummary(R.string.snackbarPersistent)
            else
                preference.setSummary(R.string.snackbarNonPersistent)
            true
        }

        val splitLayout = preferenceScreen.findPreference<SwitchPreferenceCompat>("splitLayout")
        splitLayout!!.summary = if (preferenceStore.getBoolean("splitLayout", true))
            getString(R.string.splitLayout)
        else
            getString(R.string.notSplitLayout)
        splitLayout.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean)
                preference.setSummary(R.string.splitLayout)
            else
                preference.setSummary(R.string.notSplitLayout)
            true
        }

    }


}