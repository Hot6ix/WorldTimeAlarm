package com.simples.j.worldtimealarm.support

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.fragments.SettingFragment
import com.simples.j.worldtimealarm.fragments.WorldClockFragment

class FragmentPagerAdapter(private val fm: FragmentManager): FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0  -> AlarmListFragment()
            1 -> WorldClockFragment()
            2 -> SettingFragment()
            else -> AlarmListFragment()
        }
    }

    override fun getCount(): Int = 3

}