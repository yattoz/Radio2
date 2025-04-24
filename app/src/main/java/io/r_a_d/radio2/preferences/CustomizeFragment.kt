package io.r_a_d.radio2.preferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import io.r_a_d.radio2.R
import io.r_a_d.radio2.preferenceStore
import io.r_a_d.radio2.ui.songs.request.Requestor

class CustomizeFragment : PreferenceFragmentCompat() {

    override fun onAttach(context: Context) {
        (activity as AppCompatActivity).supportActionBar?.title = context.getString(R.string.customize)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.customize_preferences, rootKey)

        val userNamePref = preferenceScreen.findPreference<EditTextPreference>("userName")
        userNamePref?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        userNamePref?.setOnPreferenceChangeListener { _, newValue ->
            val name = newValue as String
            Requestor.instance.initFavorites(name) // need to be as parameter cause the callback is called BEFORE PARAMETER SET
            true
        }

        val sortFavorites = preferenceScreen.findPreference<SwitchPreferenceCompat>("sortFavoritesAlphabetically")
        sortFavorites!!.summary = if (preferenceStore.getBoolean("sortFavoritesAlphabetically", false))
            getString(R.string.sortFavoritesAlphabetically)
        else
            getString(R.string.sortFavoritesNot)
        sortFavorites.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean)
                preference.setSummary(R.string.sortFavoritesAlphabetically)
            else
                preference.setSummary(R.string.sortFavoritesNot)
            true
        }


        val snackbarPersistent = preferenceScreen.findPreference<SwitchPreferenceCompat>("snackbarPersistent")
        snackbarPersistent!!.summary = if (preferenceStore.getBoolean("snackbarPersistent", false))
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
            getString(R.string.split_layout)
        else
            getString(R.string.not_split_layout)
        splitLayout.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean)
                preference.setSummary(R.string.split_layout)
            else
                preference.setSummary(R.string.not_split_layout)
            true
        }

        val helpFavorites = preferenceScreen.findPreference<Preference>("helpFavorites")
        helpFavorites?.setOnPreferenceClickListener { _ ->
            val url = getString(R.string.github_url_wiki_irc_for_favorites)
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
            true
        }

        val fetchPeriod = preferenceScreen.findPreference<ListPreference>("fetchPeriod")
        fetchPeriod?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        fetchPeriod?.setOnPreferenceChangeListener { _, newValue ->
            val builder1 = AlertDialog.Builder(context!!)
            if (Integer.parseInt(newValue as String) == 0)
                builder1.setMessage(R.string.fetch_disabled_restart_the_app)
            else
                builder1.setMessage(R.string.restart_the_app)
            builder1.setCancelable(true)

            builder1.setPositiveButton("Close" ) { dialog, _ ->
                dialog.cancel()
            }

            val alert11 = builder1.create()
            alert11.show()
            true
        }

    }
}