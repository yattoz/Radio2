package io.r_a_d.radio2.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.r_a_d.radio2.R

class SleepFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sleep_preference, rootKey)

    }
}