package com.simples.j.worldtimealarm.fragments


import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.preference.*
import com.google.ads.consent.*
import com.simples.j.worldtimealarm.LicenseActivity
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.utils.MediaCursor

class SettingFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private var mTimeZoneSelectorPref: ListPreference? = null
    private lateinit var mFragmentManager: FragmentManager
    private lateinit var mConsentInformation: ConsentInformation
    private lateinit var mConsentForm: ConsentForm

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        activity?.volumeControlStream = AudioManager.STREAM_ALARM
        mFragmentManager = requireActivity().supportFragmentManager
        mConsentInformation = ConsentInformation.getInstance(context)
        mConsentForm = MediaCursor.getConsentForm(context, ConsentListener(context))

        mTimeZoneSelectorPref = findPreference<ListPreference>(getString(R.string.setting_time_zone_selector_key))?.let {
            it.isEnabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.M
            if(it.value.isNullOrEmpty()) {
                it.value =
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) SELECTOR_NEW
                        else SELECTOR_OLD
            }
            it.onPreferenceChangeListener = this@SettingFragment

            sBindPreferenceSummaryToValueListener.onPreferenceChange(it,
                    PreferenceManager
                            .getDefaultSharedPreferences(this.context)
                            .getString(it.key, ""))
            it
        }

        // enable 24 hour clock
        findPreference<SwitchPreferenceCompat>(getString(R.string.setting_24_hr_clock_key))?.setOnPreferenceClickListener {
            context?.sendBroadcast(Intent(MainActivity.ACTION_UPDATE_ALL))
            true
        }

        // alarm mute
        findPreference<ListPreference>(getString(R.string.setting_alarm_mute_key))?.let {
            if(it.value.isNullOrEmpty()) {
                it.value = "300000"
            }

            bindPreferenceSummaryToValue(it)
        }

        activity?.run {
            val pName = packageManager.getPackageInfo(packageName, 0)?.versionName

            val version = findPreference<Preference>(getString(R.string.setting_version_key))
            version?.summary = pName

            val categoryOther = version?.parent as PreferenceCategory

            if(mConsentInformation.isRequestLocationInEeaOrUnknown) {
                val eeaConsentPref = Preference(context).apply {
                    key = getString(R.string.setting_eea_key)
                    title = getString(R.string.setting_eea_title)
                    onPreferenceClickListener = this@SettingFragment
                }
                categoryOther.addPreference(eeaConsentPref)
            }
        }

        findPreference<Preference>(getString(R.string.setting_converter_goto_key))?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
            true
        }

        findPreference<SwitchPreference>(getString(R.string.setting_converter_remember_last_key))?.onPreferenceChangeListener = this

        findPreference<Preference>(getString(R.string.setting_license_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, LicenseActivity::class.java))
            true
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
            getString(R.string.setting_converter_remember_last_key) -> {
                if(newValue is Boolean && newValue == true) {
                    val intent = Intent(WorldClockFragment.ACTION_LAST_SETTING_CHANGED)
                    context?.sendBroadcast(intent)
                }
            }

        }

        return true
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when(preference?.key) {
            getString(R.string.setting_eea_key) -> {
                SimpleDialogFragment.newInstance(getString(R.string.setting_eea_dialog_title), getString(R.string.setting_eea_dialog_summary), SimpleDialogFragment.CANCELABLE_ALL, getString(R.string.change), getString(R.string.revoke), getString(R.string.detail)).apply {
                    setOnDialogEventListener(object : SimpleDialogFragment.OnDialogEventListener {
                        override fun onPositiveButtonClicked(inter: DialogInterface) {
                            // change
                            mConsentForm.load()
                        }

                        override fun onNegativeButtonClicked(inter: DialogInterface) {
                            // revoke
                            mConsentInformation.consentStatus = ConsentStatus.UNKNOWN
                            activity?.finish()
                        }

                        override fun onNeutralButtonClicked(inter: DialogInterface) {
                            // detail
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.eu_consent_link)))
                            startActivity(intent)
                        }

                    })
                }.show(mFragmentManager, SimpleDialogFragment.TAG)
            }
        }

        return true
    }

    inner class ConsentListener(private val context: Context?): ConsentFormListener() {
        override fun onConsentFormLoaded() {
            super.onConsentFormLoaded()
            Log.d(C.TAG, "Consent form loaded")
            mConsentForm.show()
        }

        override fun onConsentFormOpened() {
            super.onConsentFormOpened()
            Log.d(C.TAG, "Consent form opened")
        }

        override fun onConsentFormClosed(consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?) {
            super.onConsentFormClosed(consentStatus, userPrefersAdFree)
            Log.d(C.TAG, "Consent form closed")

            if(consentStatus == ConsentStatus.NON_PERSONALIZED || consentStatus == ConsentStatus.PERSONALIZED) {
                Toast.makeText(context, getString(R.string.eea_update_message), Toast.LENGTH_SHORT).show()
            }
        }

        override fun onConsentFormError(reason: String?) {
            super.onConsentFormError(reason)
            Log.d(C.TAG, "Consent form error: $reason")
        }
    }

    companion object {

        const val TAG = "SettingFragment"
        const val SELECTOR_NEW = "1"
        const val SELECTOR_OLD = "0"

        @JvmStatic
        fun newInstance() = SettingFragment()

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
