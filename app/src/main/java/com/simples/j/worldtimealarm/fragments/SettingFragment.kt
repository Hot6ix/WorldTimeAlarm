package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.preference.SwitchPreference
import android.provider.Settings
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import android.widget.CompoundButton
import com.simples.j.worldtimealarm.LicenseActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.fragments.WorldClockFragment.Companion.TIME_ZONE_CHANGED_KEY
import java.util.*

/**
 * A simple [Fragment] subclass.
 *
 */
class SettingFragment : PreferenceFragmentCompat(), CompoundButton.OnCheckedChangeListener {

    private lateinit var converterTimezone: com.simples.j.worldtimealarm.support.SwitchPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        activity!!.volumeControlStream = AudioManager.STREAM_ALARM

        val pName = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName

        val version = findPreference(resources.getString(R.string.setting_version_key))
        version.summary = pName

        bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.setting_alarm_mute_key)))

        converterTimezone = findPreference(resources.getString(R.string.setting_converter_timezone_key)) as com.simples.j.worldtimealarm.support.SwitchPreference
        converterTimezone.setSwitchListener(this)

        val converterTimezoneId = PreferenceManager.getDefaultSharedPreferences(context).getString(resources.getString(R.string.setting_converter_timezone_id_key), "")
        converterTimezone.summary =
                if(converterTimezoneId.isEmpty()) TimeZone.getDefault().id.replace("_", " ")
                else converterTimezoneId.replace("_", " ")

        converterTimezone.setOnPreferenceClickListener {
            if(converterTimezone.isChecked)
                startActivityForResult(Intent(activity, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
            true
        }
        converterTimezone.setOnPreferenceChangeListener { preference, newValue ->
            val isEnabled = preference.isEnabled
            if(!isEnabled) converterTimezone.summary = TimeZone.getDefault().id
            true
        }

        findPreference(resources.getString(R.string.setting_converter_goto_key)).setOnPreferenceClickListener {
            val intent = Intent(Settings.ACTION_DATE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        }

        findPreference(resources.getString(R.string.setting_license_key)).setOnPreferenceClickListener {
            startActivity(Intent(context, LicenseActivity::class.java))
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == TIME_ZONE_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                if(data != null && data.hasExtra(TimeZoneSearchActivity.TIME_ZONE_ID)) {
                    val timeZone = data.getStringExtra(TimeZoneSearchActivity.TIME_ZONE_ID)
                    val formattedTimeZone = timeZone.replace(" ", "_")
                    converterTimezone.summary = timeZone
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(resources.getString(R.string.setting_converter_timezone_id_key), formattedTimeZone).apply()
                    val intent = Intent(WorldClockFragment.ACTION_TIME_ZONE_CHANGED)
                    intent.putExtra(TIME_ZONE_CHANGED_KEY, formattedTimeZone)
                    context!!.sendBroadcast(intent)
                }
            }
        }
    }
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        converterTimezone.isChecked = isChecked
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(resources.getString(R.string.setting_converter_timezone_key), isChecked).apply()
        when(isChecked) {
            true -> {
                var timezone = PreferenceManager.getDefaultSharedPreferences(context).getString(resources.getString(R.string.setting_converter_timezone_id_key), "").replace(" ", "_")
                if(timezone.isEmpty()) timezone = TimeZone.getDefault().id

                val intent = Intent(WorldClockFragment.ACTION_TIME_ZONE_CHANGED)
                intent.putExtra(TIME_ZONE_CHANGED_KEY, timezone)
                context!!.sendBroadcast(intent)
            }
            false -> {
//                val intent = Intent(WorldClockFragment.ACTION_TIME_ZONE_CHANGED)
//                intent.putExtra(TIME_ZONE_CHANGED_KEY, TimeZone.getDefault().id)
//                context!!.sendBroadcast(intent)
            }
        }
    }

    companion object {

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            listPreference.entries[index]
                        else
                            null)

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }

}
