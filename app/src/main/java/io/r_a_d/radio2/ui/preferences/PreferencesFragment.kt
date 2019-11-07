package io.r_a_d.radio2.ui.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.r_a_d.radio2.R

class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}