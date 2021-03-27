package com.simples.j.worldtimealarm.fragments

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.simples.j.worldtimealarm.*
import com.simples.j.worldtimealarm.ViewMatcherExtension.exists
import com.simples.j.worldtimealarm.ViewMatcherExtension.withNeighbor
import com.simples.j.worldtimealarm.ViewMatcherExtension.withOneOfText
import com.simples.j.worldtimealarm.ViewMatcherExtension.withTime
import com.simples.j.worldtimealarm.utils.MediaCursor
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AlarmGeneratorFragmentUITest {
    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<AlarmGeneratorActivity> = ActivityScenarioRule(AlarmGeneratorActivity::class.java)

    lateinit var activityScenario: ActivityScenario<AlarmGeneratorActivity>
    lateinit var context: Context

    @Before
    fun init() {
//        AccessibilityChecks.enable().setRunChecksFromRootView(true)

        activityScenario = activityScenarioRule.scenario
        activityScenario.apply {
            moveToState(Lifecycle.State.CREATED)
            moveToState(Lifecycle.State.STARTED)
            moveToState(Lifecycle.State.RESUMED)
        }

        activityScenario.onActivity {
            context = it.applicationContext
        }

        Intents.init()
    }

    @After
    fun terminate() {
        Intents.release()
    }

    @Test
    @Throws(Exception::class)
    fun a_checkFragmentElements() {
        // check if support action bar has text "New Alarm" in title
        activityScenario.onActivity {
            assertTrue(it.supportActionBar?.title == context.getString(R.string.new_alarm_long))
        }
        // check if time zone name has something
        onView(withId(R.id.time_zone_name)).check(matches(not(withText(""))))
        // check if date has text "Not set"
        onView(withId(R.id.date)).check(matches(withText(context.getString(R.string.range_not_set))))
        // check date picker is set to time now
        // time may past during testing, so check two different time
        val now = Calendar.getInstance()
        val minuteLater = (now.clone() as Calendar).apply {
            add(Calendar.MINUTE, 1)
        }
        onView(withId(R.id.time_picker)).check(
                matches(
                        withTime(
                                Pair(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)),
                                Pair(minuteLater.get(Calendar.HOUR_OF_DAY), minuteLater.get(Calendar.MINUTE))
                        )
                )
        )
        // check day recurrences are not checked
        onView(withId(R.id.sunday)).check(matches(isNotChecked()))
        onView(withId(R.id.monday)).check(matches(isNotChecked()))
        onView(withId(R.id.tuesday)).check(matches(isNotChecked()))
        onView(withId(R.id.wednesday)).check(matches(isNotChecked()))
        onView(withId(R.id.thursday)).check(matches(isNotChecked()))
        onView(withId(R.id.friday)).check(matches(isNotChecked()))
        onView(withId(R.id.saturday)).check(matches(isNotChecked()))
        // check estimated
        onView(withId(R.id.est_time_zone)).check(matches(not(withText(""))))
        onView(withId(R.id.est_date_time)).check(matches(not(withText(""))))
        // check ringtone is set to first ringtone or not set
        val ringtoneList = MediaCursor.getRingtoneList(context)
        val ringtoneTitleList = ringtoneList.map {
            it.title
        }
        onView(allOf(
                withId(R.id.option_summary),
                withOneOfText(ringtoneTitleList),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[0])
                ))
        )).check(matches(exists()))
        // check vibration is not set
        onView(allOf(
                withId(R.id.option_summary),
                withText(context.getString(R.string.no_vibrator)),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[1])
                ))
        )).check(matches(exists()))
        // check snooze is not set
        onView(allOf(
                withId(R.id.option_summary),
                withText(context.resources.getStringArray(R.array.snooze_array)[0]),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[2])
                ))
        )).check(matches(exists()))
        // check label is empty
        onView(allOf(
                withId(R.id.option_summary),
                withText(""),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[3])
                ))
        )).check(matches(exists()))
        // check action button text
        onView(allOf(
                withText(context.getString(R.string.create)),
                withId(R.id.action)
        )).check(matches(isDisplayed()))
    }

    @Test
    @Throws(Exception::class)
    fun b_testInteraction() {
        // click time zone layout
        onView(withId(R.id.time_zone_view)).perform(click())
        // check TimeZonePickerActivity has launched
        intended(hasComponent(TimeZonePickerActivity::class.java.name))
        // go back
        pressBack()

        // click date layout
        onView(withId(R.id.date_view)).perform(click())
        // check ContentSelectorActivity has launched
        intended(hasComponent(ContentSelectorActivity::class.java.name))
        // go back
        pressBack()

        // click and check day recurrences
        onView(withId(R.id.sunday)).perform(click()).check(matches(isChecked()))
        onView(withId(R.id.monday)).perform(click()).check(matches(isChecked()))
        onView(withId(R.id.tuesday)).perform(click()).check(matches(isChecked()))
        onView(withId(R.id.wednesday)).perform(click()).check(matches(isChecked()))
        onView(withId(R.id.thursday)).perform(click()).check(matches(isChecked()))
        onView(withId(R.id.friday)).perform(click()).check(matches(isChecked()))
        onView(withId(R.id.saturday)).perform(click()).check(matches(isChecked()))
        // click ringtone
        onView(allOf(
                withId(R.id.option_summary),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[0])
                ))
        )).perform(click())
        // check ContentSelectorActivity has launched
        intended(hasComponent(ContentSelectorActivity::class.java.name), times(2))
        // check action bar title is "Ringtone"
        onView(allOf(
                instanceOf(TextView::class.java),
                withParent(withResourceName("action_bar"))
        )).check(matches(withText("Ringtone")))
        // go back
        pressBack()

        // click vibration
        onView(allOf(
                withId(R.id.option_summary),
                withText(context.getString(R.string.no_vibrator)),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[1])
                ))
        )).perform(click())
        // check ContentSelectorActivity has launched
        intended(hasComponent(ContentSelectorActivity::class.java.name), times(3))
        // check action bar title is "Vibration"
        onView(allOf(
                instanceOf(TextView::class.java),
                withParent(withResourceName("action_bar"))
        )).check(matches(withText("Vibration")))
        // go back
        pressBack()

        // click snooze
        onView(allOf(
                withId(R.id.option_summary),
                withText(context.resources.getStringArray(R.array.snooze_array)[0]),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[2])
                ))
        )).perform(ViewActionExtension.NestedScrollViewExtension(), click())
        // check ContentSelectorActivity has launched
        intended(hasComponent(ContentSelectorActivity::class.java.name), times(4))
        // check action bar title is "Snooze"
        onView(allOf(
                instanceOf(TextView::class.java),
                withParent(withResourceName("action_bar"))
        )).check(matches(withText("Snooze")))
        // go back
        pressBack()

        // click label
        onView(allOf(
                withId(R.id.option_summary),
                withText(""),
                withNeighbor(allOf(
                        withId(R.id.option_title),
                        withText(context.resources.getStringArray(R.array.alarm_options)[3])
                ))
        )).perform(ViewActionExtension.NestedScrollViewExtension(), click())
        // check dialog components are displayed
        onView(allOf(
                withId(R.id.label),
                withClassName(`is`(AppCompatEditText::class.java.canonicalName))
        )).check(matches(isDisplayed()))
        // close dialog using button
        onView(allOf(
                withId(android.R.id.button1),
                withText("OK")
        )).perform(click())

        // check action button text
        onView(allOf(
                withText(context.getString(R.string.create)),
                withId(R.id.action)
        )).perform(click())
        // check if AlarmGeneratorActivity has finished
        assertTrue(activityScenario.state == Lifecycle.State.DESTROYED)
    }
}