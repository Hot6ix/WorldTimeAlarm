package com.simples.j.worldtimealarm.fragments


import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import android.widget.CompoundButton
import com.simples.j.worldtimealarm.LicenseActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity.Companion.TIME_ZONE_REQUEST_CODE
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.fragments.WorldClockFragment.Companion.TIME_ZONE_CHANGED_KEY
import com.simples.j.worldtimealarm.utils.MediaCursor
import java.util.*

class SettingFragment : PreferenceFragmentCompat(), CompoundButton.OnCheckedChangeListener, Preference.OnPreferenceChangeListener {

    private lateinit var converterTimezone: com.simples.j.worldtimealarm.support.SwitchPreference
    private var mTimeZoneSelectorPref: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        activity?.volumeControlStream = AudioManager.STREAM_ALARM

        mTimeZoneSelectorPref = with(findPreference(getString(R.string.setting_time_zone_selector_key)) as ListPreference) {
            isEnabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.M
            if(this.value.isNullOrEmpty()) {
                value =
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) SELECTOR_NEW
                        else SELECTOR_OLD
            }
            onPreferenceChangeListener = this@SettingFragment

            sBindPreferenceSummaryToValueListener.onPreferenceChange(this,
                    PreferenceManager
                            .getDefaultSharedPreferences(this.context)
                            .getString(this.key, ""))
            this
        }

        with(findPreference(getString(R.string.setting_alarm_mute_key)) as ListPreference) {
            if(this.value.isNullOrEmpty()) {
                value = "300000"
            }

            bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.setting_alarm_mute_key)))
        }

        val pName = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName

        val version = findPreference(resources.getString(R.string.setting_version_key))
        version.summary = pName

        converterTimezone = findPreference(resources.getString(R.string.setting_converter_timezone_key)) as com.simples.j.worldtimealarm.support.SwitchPreference
        converterTimezone.setSwitchListener(this)

        val converterTimezoneId = PreferenceManager.getDefaultSharedPreferences(context).getString(resources.getString(R.string.setting_converter_timezone_id_key), "")
        converterTimezone.summary =
                if(converterTimezoneId.isNullOrEmpty()) getNameForTimeZone(TimeZone.getDefault().id)
                else getNameForTimeZone(converterTimezoneId)

        converterTimezone.setOnPreferenceClickListener {
            if(converterTimezone.isChecked) {
                var timezone = PreferenceManager.getDefaultSharedPreferences(context).getString(resources.getString(R.string.setting_converter_timezone_id_key), "")?.replace(" ", "_")
                if(timezone.isNullOrEmpty()) timezone = TimeZone.getDefault().id

                when {
                    Build.VERSION.SDK_INT > Build.VERSION_CODES.M && mTimeZoneSelectorPref?.value == SettingFragment.SELECTOR_NEW -> {
                        val i = Intent(context, TimeZonePickerActivity::class.java).apply {
                            putExtra(TimeZonePickerActivity.ACTION, TimeZonePickerActivity.ACTION_CHANGE)
                            putExtra(TimeZonePickerActivity.TIME_ZONE_ID, timezone)
                            putExtra(TimeZonePickerActivity.TYPE, TimeZonePickerActivity.TYPE_WORLD_CLOCK)
                        }
                        startActivityForResult(i, TIME_ZONE_REQUEST_CODE)
                    }
                    else -> {
                        startActivityForResult(Intent(activity, TimeZoneSearchActivity::class.java), TIME_ZONE_REQUEST_CODE)
                    }
                }
            }
            true
        }

        converterTimezone.onPreferenceChangeListener = this

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
                    context?.sendBroadcast(intent)
                }
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        converterTimezone.isChecked = isChecked
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(resources.getString(R.string.setting_converter_timezone_key), isChecked).apply()
        when(isChecked) {
            true -> {
                var timezone = PreferenceManager.getDefaultSharedPreferences(context).getString(resources.getString(R.string.setting_converter_timezone_id_key), "")?.replace(" ", "_")
                if(timezone.isNullOrEmpty()) timezone = TimeZone.getDefault().id

                val intent = Intent(WorldClockFragment.ACTION_TIME_ZONE_CHANGED)
                intent.putExtra(TIME_ZONE_CHANGED_KEY, timezone)
                context?.sendBroadcast(intent)
            }
            false -> {
//                val intent = Intent(WorldClockFragment.ACTION_TIME_ZONE_CHANGED)
//                intent.putExtra(TIME_ZONE_CHANGED_KEY, TimeZone.getDefault().id)
//                context?.sendBroadcast(intent)
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when(preference.key) {
            getString(R.string.setting_time_zone_selector_key) -> {
                val intent = Intent(WorldClockFragment.ACTION_TIME_ZONE_SELECTOR_CHANGED)
                context?.sendBroadcast(intent)

                val index = (preference as ListPreference).findIndexOfValue(newValue.toString())

                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)
            }
            getString(R.string.setting_converter_timezone_key) -> {
                val isEnabled = preference.isEnabled
                if(!isEnabled) converterTimezone.summary = TimeZone.getDefault().id
            }
        }

        return true
    }

    private fun getNameForTimeZone(timeZoneId: String?): String {
        return if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            MediaCursor.getBestNameForTimeZone(android.icu.util.TimeZone.getTimeZone(timeZoneId))
        }
        else timeZoneId ?: getString(R.string.time_zone_unknown)
    }

    private fun updateDefaultMuteAlarmValue() {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val alarmMutePref = findPreference(getString(R.string.setting_alarm_mute_key)) as ListPreference
        Log.d(C.TAG, alarmMutePref.value)
        if(alarmMutePref.value != "0") {
            pref.edit().putBoolean(INTERNAL_MUTE_ALARM_BOOL, true).apply()
        }

        if(!pref.getBoolean(INTERNAL_MUTE_ALARM_BOOL, false)) {
            alarmMutePref.value = "300000"
            bindPreferenceSummaryToValue(alarmMutePref)
        }
    }

    companion object {

        const val SELECTOR_NEW = "1"
        const val SELECTOR_OLD = "0"

        const val INTERNAL_MUTE_ALARM_BOOL = "INTERNAL_MUTE_ALARM_BOOL"

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
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
