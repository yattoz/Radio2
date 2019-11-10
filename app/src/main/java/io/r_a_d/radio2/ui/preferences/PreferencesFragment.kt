package io.r_a_d.radio2.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.WorkManager
import io.r_a_d.radio2.R
import io.r_a_d.radio2.preferenceStore
import io.r_a_d.radio2.streamerNotificationService.WorkerStore
import io.r_a_d.radio2.streamerNotificationService.startStreamerMonitor
import io.r_a_d.radio2.streamerNotificationService.stopStreamerMonitor
import io.r_a_d.radio2.ui.songs.request.Requestor

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        preferenceScreen.isIconSpaceReserved = false
        val userNamePref = preferenceScreen.findPreference<EditTextPreference>("userName")
        userNamePref!!.summary = userNamePref.text
        userNamePref.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue as CharSequence
            Requestor.instance.initFavorites(newValue as String) // need to be as parameter cause the callback is called BEFORE PARAMETER SET
            true
        }
        val submitBug = preferenceScreen.findPreference<Preference>("submitBug")
        submitBug!!.setOnPreferenceClickListener {
            val url = getString(R.string.github_url_new_issue)
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
            true
        }
        val streamerPeriod = preferenceScreen.findPreference<Preference>("streamerMonitorPeriodPref")

        val streamerNotification = preferenceScreen.findPreference<Preference>("newStreamerNotification")
        streamerNotification?.setOnPreferenceChangeListener { _, newValue ->
            if ((newValue as Boolean)) {
                startStreamerMonitor(context!!, force = true)
                streamerPeriod?.summary = "Every ${(preferenceStore.getString("streamerMonitorPeriodPref", "") as String)} minutes"
                streamerPeriod?.isEnabled = true
            }
            else {
                stopStreamerMonitor(context!!)
                streamerPeriod?.isEnabled = false
            }
            true
        }

        streamerPeriod?.summary = "Every ${(preferenceStore.getString("streamerMonitorPeriodPref", "") as String)} minutes"
        streamerPeriod?.isEnabled = preferenceStore.getBoolean("newStreamerNotification", true)
        streamerPeriod?.setOnPreferenceChangeListener { preference, newValue ->
            // quite nothing
            streamerPeriod.summary = "Every ${(newValue as String)} minutes"
            WorkerStore.instance.tickerPeriod = (Integer.parseInt(newValue as String)).toLong() * 60
            stopStreamerMonitor(context!!)
            startStreamerMonitor(context!!, force = true)
            true
        }
    }
}