package com.simples.j.worldtimealarm.support

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.fragments.SettingFragment
import com.simples.j.worldtimealarm.fragments.WorldClockFragment

class FragmentPagerAdapter(fm: FragmentManager): FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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