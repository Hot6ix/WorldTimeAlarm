package com.simples.j.worldtimealarm

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.fragments.AlarmGeneratorFragment
import com.simples.j.worldtimealarm.models.AlarmGeneratorViewModel

class AlarmGeneratorActivity : AppCompatActivity() {

    private lateinit var viewModel: AlarmGeneratorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_generator)

        viewModel = ViewModelProvider(this)[AlarmGeneratorViewModel::class.java]

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        val bundle = intent.getBundleExtra(BUNDLE_KEY)
        if(bundle == null) {
            // New Alarm
            supportActionBar?.title = getString(R.string.new_alarm_long)
            viewModel.alarmItem.value = null
        }
        else {
            // Modify Alarm
            supportActionBar?.title = getString(R.string.modify_alarm)

            bundle.getParcelable<AlarmItem>(AlarmReceiver.ITEM).let { alarmItem ->
                if(alarmItem == null) {
                    Toast.makeText(applicationContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                    finish()
                }
                else {
                    viewModel.alarmItem.value = alarmItem
                }
            }
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, AlarmGeneratorFragment.newInstance(), AlarmGeneratorFragment.TAG)
                .commitNow()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                true
            }
            else -> false
        }
    }

    companion object {
        const val TAG_FRAGMENT_LABEL = "TAG_FRAGMENT_LABEL"
        const val TAG_FRAGMENT_COLOR_TAG = "TAG_FRAGMENT_COLOR_TAG"
        const val BUNDLE_KEY = "BUNDLE_KEY"
    }
}
