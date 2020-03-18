package com.simples.j.worldtimealarm

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.jakewharton.threetenabp.AndroidThreeTen
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.fragments.AlarmGeneratorFragment
import com.simples.j.worldtimealarm.models.AlarmGeneratorViewModel

class AlarmGeneratorActivity : AppCompatActivity() {

    private lateinit var viewModel: AlarmGeneratorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_generator)

        AndroidThreeTen.init(this)

        viewModel = ViewModelProvider(this)[AlarmGeneratorViewModel::class.java]

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        val bundle = intent.getBundleExtra(BUNDLE_KEY)
        supportActionBar?.title =
                if(bundle == null) {
                    getString(R.string.new_alarm_long)
                }
                else {
                    getString(R.string.modify_alarm)
                }

        if(savedInstanceState == null) {
            if(bundle == null) viewModel.alarmItem = null
            else {
                bundle.getParcelable<AlarmItem>(AlarmReceiver.ITEM).let { alarmItem ->
                    if(alarmItem == null) {
                        Toast.makeText(applicationContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    else {
                        viewModel.alarmItem = alarmItem
                    }
                }
            }

            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, AlarmGeneratorFragment.newInstance(), AlarmGeneratorFragment.TAG)
                    .commitNow()
        }
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
