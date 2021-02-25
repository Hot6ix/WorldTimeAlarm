package com.simples.j.worldtimealarm

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.simples.j.worldtimealarm.ViewActionExtension.setQueryAndSubmit
import com.simples.j.worldtimealarm.fragments.TimeZoneFragment
import com.simples.j.worldtimealarm.fragments.TimeZonePickerFragment
import org.hamcrest.Matchers.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TimeZonePickerActivityUITest {
    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<TimeZonePickerActivity> = ActivityScenarioRule(TimeZonePickerActivity::class.java)

    @get:Rule
    var mainActivityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    lateinit var activityScenario: ActivityScenario<TimeZonePickerActivity>
    lateinit var mainActivityScenario: ActivityScenario<MainActivity>

    @Before
    fun init() {
        activityScenario = activityScenarioRule.scenario
        mainActivityScenario = mainActivityScenarioRule.scenario

        Intents.init()
    }

    // this test is not executed from AlarmGeneratorActivity
    // activity uses doesn't have any bundle or data
    // test without any bundle
    @Test
    fun a_checkChildren() {
        activityScenario.apply {
            moveToState(Lifecycle.State.CREATED)
            moveToState(Lifecycle.State.STARTED)
            moveToState(Lifecycle.State.RESUMED)
        }

        // timezone country summary
        onView(
                allOf(withId(R.id.time_zone_country_summary), withText(R.string.time_zone_empty))
        ).check(matches(isDisplayed()))
        // timezone region summary
        onView(
                allOf(withId(R.id.time_zone_region_summary), withText(R.string.time_zone_empty))
        ).check(matches(isDisplayed()))
        // change info
        onView(withId(R.id.time_zone_change_info)).check(matches(withText("")))
    }

    // this test is not executed from AlarmGeneratorActivity
    // activity uses doesn't have any bundle or data
    @Test
    fun b_checkFragmentSwitching() {
        var activity: TimeZonePickerActivity? = null
        var timeZoneFragment: TimeZoneFragment? = null
        var timeZonePickerCountryFragment: TimeZonePickerFragment? = null
        var timeZonePickerRegionFragment: TimeZonePickerFragment? = null
        activityScenario.onActivity {
            timeZoneFragment = it.supportFragmentManager.findFragmentByTag(TimeZonePickerActivity.TIME_ZONE_FRAGMENT_TAG) as TimeZoneFragment?
            activity = it
        }

        if(timeZoneFragment == null) throw AssertionError("timezone fragment is null")

        timeZoneFragment?.let {
            // check if timezone fragment is attached and visible
            assertTrue(it.isAdded && it.isVisible)

            onView(withId(R.id.time_zone_country_layout)).perform(click())
            // timezone fragment shouldn't be visible
            assertFalse(it.isAdded && it.isVisible)

            // press back twice to close keyboard first and pop back from timezone picker fragment to timezone fragment
            pressBack()
            pressBack()

            // check timezone fragment is visible again after press back
            assertTrue(it.isAdded && it.isVisible)

            // region layout should be disabled if country has not been selected
            onView(withId(R.id.time_zone_region_layout)).check(matches(not(isEnabled())))
        }

        // click to switch from timezone fragment to timezone picker fragment
        onView(withId(R.id.time_zone_country_layout)).perform(click())

        timeZonePickerCountryFragment = activity?.supportFragmentManager?.findFragmentByTag(TimeZonePickerActivity.TIME_ZONE_PICKER_FRAGMENT_COUNTRY_TAG) as TimeZonePickerFragment?
        // check if timezone picker for country is visible
        assertTrue(timeZonePickerCountryFragment?.isVisible ?: false)
        // app uses appbar search view
        // South Korea has two timezone (Seoul and ROK)
        // app will show two items (Seoul and ROK)
        onView(withId(R.id.search_timezone)).perform(setQueryAndSubmit("South Korea"))
        // wait for result
        Thread.sleep(5000)
        onView(
                allOf(
                        withText("South Korea"), withId(R.id.time_zone_picker_title)
                )
        ).let {
            // check if recyclerview has result South Korea
            it.check(matches(isDisplayed()))
            // click the result
            it.perform(click())

            timeZonePickerRegionFragment = activity?.supportFragmentManager?.findFragmentByTag(TimeZonePickerActivity.TIME_ZONE_PICKER_FRAGMENT_TIME_ZONE_TAG) as TimeZonePickerFragment?
            // check if timezone picker for region is visible
            assertTrue(timeZonePickerRegionFragment?.isVisible ?: false)
        }

        // check if timezone Seoul is displaying
        onView(
                allOf(
                        withText("Seoul"), withId(R.id.time_zone_picker_title)
                )
        ).check(matches(isDisplayed()))
        // check if timezone ROK is displaying
        onView(
                allOf(
                        withText("ROK"), withId(R.id.time_zone_picker_title)
                )
        ).check(matches(isDisplayed()))

        // click Seoul item
        onView(
                allOf(
                        withText("Seoul"), withId(R.id.time_zone_picker_title)
                )
        ).perform(click())

        // check if timezone picker fragment is finished and timezone fragment is visible
        assertTrue(timeZoneFragment?.isVisible ?: false)
        assertFalse(timeZonePickerCountryFragment?.isVisible ?: true)
        assertFalse(timeZonePickerRegionFragment?.isVisible ?: true)

        // check if timezone fragment set country to South Korea
        onView(
                allOf(withId(R.id.time_zone_country_summary), withText("South Korea"))
        ).check(matches(isDisplayed()))
        // check if timezone fragment set region to Seoul
        onView(
                allOf(withId(R.id.time_zone_region_summary), withText(containsString("Seoul")))
        ).check(matches(isDisplayed()))
        // check if change info is not empty
        onView(withId(R.id.time_zone_change_info)).check(matches(not(withText(""))))
    }
}