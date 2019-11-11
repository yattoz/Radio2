package io.r_a_d.radio2.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.r_a_d.radio2.R
import io.r_a_d.radio2.alarm.RadioAlarm

class AlarmFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.alarm_preferences, rootKey)

        val isWakingUp = findPreference<SwitchPreferenceCompat>("isWakingUp")
        isWakingUp?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean)
                RadioAlarm.instance.setAlarm(context!!)
            else
                RadioAlarm.instance.cancelAlarm(context!!)
            true
        }

    }


}