package pynith.apps.nextel.fragment

import android.os.Bundle
import androidx.preference.*
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.views.us.SupportActivity

class SettingsFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Bind summaries
        bindPreferenceSummaryToValue(findPreference("settings_display_options_size")!!)
        bindPreferenceSummaryToValue(findPreference("theme")!!)

        val supportPref = findPreference<Preference>("open_support")

        supportPref?.setOnPreferenceClickListener {
            val context = requireContext()
            startActivity(SupportActivity.createIntent(context, SessionService(context).getToken()))
            true
        }

    }

    private fun bindPreferenceSummaryToValue(preference: Preference) {
        preference.onPreferenceChangeListener = this

        val prefs = PreferenceManager.getDefaultSharedPreferences(preference.context)
        val value = prefs.getString(preference.key, "")

        onPreferenceChange(preference, value)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val stringValue = newValue.toString()

        when (preference) {
            is ListPreference -> {
                val index = preference.findIndexOfValue(stringValue)
                preference.summary = if (index >= 0)
                    preference.entries[index]
                else null
            }

            is SwitchPreferenceCompat -> {
                preference.summary = if (newValue as Boolean)
                    "Enabled"
                else "Disabled"
            }

            else -> {
                preference.summary = stringValue
            }
        }

        return true
    }
}