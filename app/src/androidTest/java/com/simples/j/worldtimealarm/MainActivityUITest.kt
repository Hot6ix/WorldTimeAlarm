package com.simples.j.worldtimealarm

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.simples.j.worldtimealarm.fragments.AlarmListFragment
import com.simples.j.worldtimealarm.fragments.SettingFragment
import com.simples.j.worldtimealarm.fragments.WorldClockFragment
import com.simples.j.worldtimealarm.utils.DatabaseManager
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MainActivityUITest {
    // TODO:  Should add more code to test several conditions

    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    lateinit var activityScenario: ActivityScenario<MainActivity>

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Delete all data before start test...")
            ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DatabaseManager.DB_NAME)
        }
    }

    @Before
    fun init() {
//        AccessibilityChecks.enable().setRunChecksFromRootView(true)

        activityScenario = activityScenarioRule.scenario
        activityScenario.apply {
            moveToState(Lifecycle.State.CREATED)
            moveToState(Lifecycle.State.STARTED)
            moveToState(Lifecycle.State.RESUMED)
        }

        Intents.init()
    }

    @Test
    fun check_tabs() {
        var fragmentManager: FragmentManager? = null

        activityScenario.onActivity {
            fragmentManager = it.supportFragmentManager

        }

        val alarmListFragment = fragmentManager?.findFragmentByTag(AlarmListFragment.TAG) as AlarmListFragment
        val worldClockFragment = fragmentManager?.findFragmentByTag(WorldClockFragment.TAG) as WorldClockFragment
        val settingFragment = fragmentManager?.findFragmentByTag(SettingFragment.TAG) as SettingFragment

        assertTrue("Check if fragments are added", alarmListFragment.isAdded && worldClockFragment.isAdded && settingFragment.isAdded)

        assertTrue("Check AlarmListFragment's visibility", alarmListFragment.isVisible)
        assertTrue("Check WorldClockFragment's visibility", !worldClockFragment.isVisible)
        assertTrue("Check SettingFragment's visibility", !settingFragment.isVisible)

        onView(withId(R.id.view_clock)).perform(click())
        assertTrue("Check AlarmListFragment's visibility", !alarmListFragment.isVisible)
        assertTrue("Check WorldClockFragment's visibility", worldClockFragment.isVisible)
        assertTrue("Check SettingFragment's visibility", !settingFragment.isVisible)

        onView(withId(R.id.view_setting)).perform(click())
        assertTrue("Check AlarmListFragment's visibility", !alarmListFragment.isVisible)
        assertTrue("Check WorldClockFragment's visibility", !worldClockFragment.isVisible)
        assertTrue("Check SettingFragment's visibility", settingFragment.isVisible)
    }

    @After
    fun finish() {
        Intents.release()

        activityScenario.apply {
            moveToState(Lifecycle.State.DESTROYED)
        }
    }
}