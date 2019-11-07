package io.r_a_d.radio2

import android.os.Bundle
import io.r_a_d.radio2.ui.preferences.PreferencesFragment

class ParametersActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UI Launch
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_parameters)

        // my_child_toolbar is defined in the layout file
        setSupportActionBar(findViewById(R.id.toolbar))

        // Get a support ActionBar corresponding to this toolbar and enable the Up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        supportFragmentManager
            .beginTransaction()
            .replace(R.id.parameters_host_container, PreferencesFragment())
            .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}