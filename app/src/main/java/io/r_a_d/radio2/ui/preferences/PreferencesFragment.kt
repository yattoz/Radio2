package io.r_a_d.radio2.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.r_a_d.radio2.preferenceStore
import io.r_a_d.radio2.streamerNotificationService.WorkerStore
import io.r_a_d.radio2.streamerNotificationService.startStreamerMonitor
import io.r_a_d.radio2.streamerNotificationService.stopStreamerMonitor
import io.r_a_d.radio2.ui.songs.request.Requestor
import androidx.appcompat.app.AlertDialog
import io.r_a_d.radio2.R
import android.annotation.SuppressLint
import androidx.preference.*


class PreferencesFragment : PreferenceFragmentCompat() {

    @SuppressLint("ApplySharedPref")
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
        streamerNotification?.setOnPreferenceChangeListener { preference, newValue ->
            if ((newValue as Boolean)) {
                val builder1 = AlertDialog.Builder(context!!)
                builder1.setMessage(R.string.warningStreamerNotif)
                builder1.setCancelable(false)
                builder1.setPositiveButton(
                    "Yes"
                ) { dialog, _ ->
                    startStreamerMonitor(context!!, force = true) // force enabled because the preference value is not yet set when running this callback.
                    streamerPeriod?.summary = "Every ${(preferenceStore.getString("streamerMonitorPeriodPref", "") as String)} minutes"
                    streamerPeriod?.isEnabled = true
                    dialog.cancel()
                }

                builder1.setNegativeButton(
                    "No"
                ) { dialog, _ ->
                    // we force-reset the switch (that's why I use commit() )
                    val preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
                    val editor = preferences.edit()
                    editor.putBoolean("newStreamerNotification", false)
                    editor.commit()
                    stopStreamerMonitor(context!!)
                    (streamerNotification as SwitchPreferenceCompat).isChecked = false
                    dialog.cancel()
                }

                val alert11 = builder1.create()
                alert11.show()
            }
            else {
                stopStreamerMonitor(context!!)
                streamerPeriod?.isEnabled = false
                WorkerStore.instance.isServiceStarted = false
            }
            true
        }

        streamerPeriod?.summary = "Every ${(preferenceStore.getString("streamerMonitorPeriodPref", "") as String)} minutes"
        streamerPeriod?.isEnabled = preferenceStore.getBoolean("newStreamerNotification", true)
        streamerPeriod?.setOnPreferenceChangeListener { _, newValue ->
            // quite nothing
            streamerPeriod.summary = "Every ${(newValue as String)} minutes"
            WorkerStore.instance.tickerPeriod = (Integer.parseInt(newValue)).toLong() * 60
            // this should be sufficient, the next alarm schedule should take the new tickerPeriod.
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



    }


}