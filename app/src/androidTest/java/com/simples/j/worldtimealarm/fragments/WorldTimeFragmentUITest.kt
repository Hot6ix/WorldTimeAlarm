package com.simples.j.worldtimealarm.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TimeZonePickerActivity
import com.simples.j.worldtimealarm.TimeZoneSearchActivity
import com.simples.j.worldtimealarm.utils.DatabaseManager
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WorldTimeFragmentUITest {
    // TODO:  Should add more code to test several conditions

    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    lateinit var activityScenario: ActivityScenario<MainActivity>
    lateinit var preference: SharedPreferences
    lateinit var context: Context

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

        context = ApplicationProvider.getApplicationContext()
        preference = PreferenceManager.getDefaultSharedPreferences(context)

        activityScenario = activityScenarioRule.scenario
        activityScenario.apply {
            moveToState(Lifecycle.State.CREATED)
            moveToState(Lifecycle.State.STARTED)
            moveToState(Lifecycle.State.RESUMED)
        }

        onView(withId(R.id.view_clock)).perform(click())

        Intents.init()
    }

    @Test
    fun a_checkIfAddWorldClockIsWorking() {
        onView(withId(R.id.new_timezone)).perform(click())

        val timeZoneSelectorOption =  preference.getString(context.resources.getString(R.string.setting_time_zone_selector_key), SettingFragment.SELECTOR_OLD) ?: SettingFragment.SELECTOR_OLD
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M && timeZoneSelectorOption == SettingFragment.SELECTOR_NEW) {
            intended(hasComponent(TimeZonePickerActivity::class.java.name))
        }
        else {
            intended(hasComponent(TimeZoneSearchActivity::class.java.name))
        }
    }

    @After
    fun finish() {
        Intents.release()

        activityScenario.apply {
            moveToState(Lifecycle.State.DESTROYED)
        }
    }
}