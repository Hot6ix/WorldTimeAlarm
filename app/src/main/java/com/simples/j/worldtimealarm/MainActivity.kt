package com.simples.j.worldtimealarm

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.simples.j.worldtimealarm.etc.C
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.support.FragmentPagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var fragmentPagerAdapter: FragmentStatePagerAdapter

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launch(coroutineContext) {
            withContext(Dispatchers.IO) {
                MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
            }
            adViewMain.loadAd(AdRequest.Builder().build())
        }

        val tab01 = tab.newTab()
                .setIcon(R.drawable.ic_action_alarm_white)
        val tab02 = tab.newTab()
                .setIcon(R.drawable.ic_action_time_white)
        val tab03 = tab.newTab()
                .setIcon(R.drawable.ic_action_setting_white)

        tab.addTab(tab01)
        tab.addTab(tab02)
        tab.addTab(tab03)

        tab.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                fragment_pager.currentItem = tab!!.position
            }

        })
        tab.setSelectedTabIndicatorColor(ContextCompat.getColor(applicationContext, R.color.blueGrayDark))
        tab.minimumHeight = (8 * resources.displayMetrics.density).toInt()

        when(resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> tab.layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (60 * resources.displayMetrics.density).toInt())
            Configuration.ORIENTATION_LANDSCAPE -> tab.layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (50 * resources.displayMetrics.density).toInt())
        }

        fragmentPagerAdapter = FragmentPagerAdapter(supportFragmentManager)

        sendHighlightRequest(intent)

        fragment_pager.apply {
            adapter = fragmentPagerAdapter
            offscreenPageLimit = 3
        }

        if(savedInstanceState != null) {
            tab.getTabAt(savedInstanceState.getInt(TAB_STATE, 0))?.select()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        sendHighlightRequest(intent)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(TAB_STATE, tab.selectedTabPosition)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        adViewMain.destroy()
    }

    private fun sendHighlightRequest(intent: Intent?) {
        with(intent?.getIntExtra(AlarmListFragment.HIGHLIGHT_KEY, 0)) {
            if(this != null && this > 0) {
                val alarmListFragment = fragmentPagerAdapter.getItem(0) as AlarmListFragment
                val bundle = Bundle().apply {
                    putInt(AlarmListFragment.HIGHLIGHT_KEY, this@with)
                }
                alarmListFragment.arguments = bundle
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_SINGLE = "com.simples.j.world_time_alarm.ACTION_UPDATE_SINGLE"
        const val ACTION_UPDATE_ALL = "com.simples.j.world_time_alarm.ACTION_UPDATE_ALL"
        const val TAB_STATE = "TAB_STATE"
    }
}
