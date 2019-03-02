package com.simples.j.worldtimealarm.support

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.fragments.SettingFragment
import com.simples.j.worldtimealarm.fragments.WorldClockFragment

class FragmentPagerAdapter(fm: FragmentManager): FragmentStatePagerAdapter(fm) {

    private val alarmListFragment = AlarmListFragment()
    private val worldClockListAdapter = WorldClockFragment()
    private val settingFragment = SettingFragment()

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0  -> alarmListFragment
            1 -> worldClockListAdapter
            2 -> settingFragment
            else -> alarmListFragment
        }
    }

    override fun getCount(): Int = 3

}