package com.simples.j.worldtimealarm

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.etc.SnoozeItem
import com.simples.j.worldtimealarm.fragments.ContentSelectorFragment
import com.simples.j.worldtimealarm.ui.models.ContentSelectorViewModel

class ContentSelectorActivity : AppCompatActivity() {

    private lateinit var viewModel: ContentSelectorViewModel

    private var lastValue: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_selector_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lastValue = intent.getSerializableExtra(LAST_SELECTED_KEY)

        supportActionBar?.title = when(intent.action) {
            ACTION_REQUEST_AUDIO -> {
                getString(R.string.select_ringtone)
            }
            ACTION_REQUEST_VIBRATION -> {
                getString(R.string.select_vibration)
            }
            ACTION_REQUEST_SNOOZE -> {
                getString(R.string.select_snooze)
            }
            else -> {
                getString(R.string.app_name)
            }
        }

        viewModel = ViewModelProviders.of(this).get(ContentSelectorViewModel::class.java).also {
            it.action = intent.action
            if(it.lastSelectedValue == null) it.lastSelectedValue = lastValue
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, ContentSelectorFragment.newInstance())
                    .commitNow()
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, getResult())
        finish()
    }

    private fun getResult(): Intent {
        return Intent().apply {
            when(intent.action) {
                ACTION_REQUEST_AUDIO -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as RingtoneItem)
                }
                ACTION_REQUEST_VIBRATION -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as PatternItem)
                }
                ACTION_REQUEST_SNOOZE -> {
                    putExtra(LAST_SELECTED_KEY, viewModel.lastSelectedValue as SnoozeItem)
                }
            }
        }
    }

    companion object {
        const val LAST_SELECTED_KEY = "LAST_SELECTED_KEY"

        const val AUDIO_REQUEST_CODE = 2
        const val VIBRATION_REQUEST_CODE = 3
        const val SNOOZE_REQUEST_CODE = 4
        const val USER_AUDIO_REQUEST_CODE = 5
        const val ACTION_REQUEST_AUDIO = "ACTION_REQUEST_AUDIO"
        const val ACTION_REQUEST_VIBRATION = "ACTION_REQUEST_VIBRATION"
        const val ACTION_REQUEST_SNOOZE = "ACTION_REQUEST_SNOOZE"
    }
}
