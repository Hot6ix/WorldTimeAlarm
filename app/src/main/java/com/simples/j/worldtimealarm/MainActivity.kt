package com.simples.j.worldtimealarm

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.simples.j.worldtimealarm.etc.AlarmItem
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.fragments.WorldClockFragment
import com.simples.j.worldtimealarm.support.FragmentPagerAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(){

    private lateinit var fragmentPagerAdapter: FragmentStatePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testWakeUpActivity()

        Handler(Handler.Callback {
            MobileAds.initialize(applicationContext, resources.getString(R.string.ad_app_id))
            adViewMain.loadAd(AdRequest.Builder().build())
            true
        }).sendEmptyMessageDelayed(0, 1000)

//        val tabView01 = layoutInflater.inflate(R.layout.tab_item, null)
//        tabView01.findViewById<ImageView>(R.id.tab_icon).setBackgroundResource(R.drawable.ic_action_alarm_white)
//        val tabView02 = layoutInflater.inflate(R.layout.tab_item, null)
//        tabView02.findViewById<ImageView>(R.id.tab_icon).setBackgroundResource(R.drawable.ic_action_world_white)
//        val tabView03 = layoutInflater.inflate(R.layout.tab_item, null)
//        tabView02.findViewById<ImageView>(R.id.tab_icon).setBackgroundResource(R.drawable.ic_action_setting_white)

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
        tab.setSelectedTabIndicatorHeight((8 * resources.displayMetrics.density).toInt())

        when(resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> tab.layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (60 * resources.displayMetrics.density).toInt())
            Configuration.ORIENTATION_LANDSCAPE -> tab.layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (50 * resources.displayMetrics.density).toInt())
        }


        fragmentPagerAdapter = FragmentPagerAdapter(supportFragmentManager)
        fragment_pager.adapter = fragmentPagerAdapter

        if(savedInstanceState != null) {
            tab.getTabAt(savedInstanceState.getInt(TAB_STATE, 0))?.select()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(TAB_STATE, tab.selectedTabPosition)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        adViewMain.destroy()
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> {
//                startActivity(Intent(this, SettingsActivity::class.java))
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    fun testWakeUpActivity() {
        // Sample alarm item to start wakeup activity
        val item = AlarmItem(999, "Asia/Taipei", "1532695080742", intArrayOf(0,0,0,0,0,0,0,0), null, null, 6000, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 0, 999999, -7508381, 999)

        val wakeUpIntent = Intent(this, WakeUpActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(AlarmReceiver.ITEM, item)
        wakeUpIntent.putExtra(AlarmReceiver.OPTIONS, bundle)
        wakeUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        wakeUpIntent.action = intent.action
        wakeUpIntent.putExtra(AlarmReceiver.OPTIONS, bundle)
        startActivity(wakeUpIntent)
    }

    companion object {
        const val ACTION_UPDATE_SINGLE = "com.simples.j.worldtimealarm.ACTION_UPDATE_SINGLE"
        const val ACTION_UPDATE_ALL = "com.simples.j.worldtimealarm.ACTION_UPDATE_ALL"
        const val TAB_STATE = "TAB_STATE"
    }
}
