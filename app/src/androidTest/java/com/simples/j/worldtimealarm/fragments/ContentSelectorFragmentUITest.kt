package com.simples.j.worldtimealarm.fragments

import android.content.Context
import android.content.Intent
import android.widget.CalendarView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.simples.j.worldtimealarm.ContentSelectorActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.ViewMatcherExtension.withOneOfText
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@MediumTest
@RunWith(AndroidJUnit4::class)
class ContentSelectorFragmentUITest {

    private lateinit var activityScenario: ActivityScenario<ContentSelectorActivity>
    private lateinit var uiDevice: UiDevice
    private lateinit var context: Context

    @Before
    fun init() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()

        Intents.init()
    }

    @After
    fun terminate() {
        Intents.release()
    }

    @Test
    fun checkActivityLaunched() {
        val intent = Intent(context, ContentSelectorActivity::class.java)
        activityScenario = ActivityScenario.launch(intent)

        intending(hasComponent(ContentSelectorActivity::class.java.name))
    }

    @Test
    fun testWithDateBundle() {
        // intent with data need for date
        val intent = Intent(context, ContentSelectorActivity::class.java).apply {
            action = ContentSelectorActivity.ACTION_REQUEST_DATE
            putExtra(ContentSelectorActivity.START_DATE_KEY, "")
            putExtra(ContentSelectorActivity.END_DATE_KEY, "")
            putExtra(ContentSelectorActivity.TIME_ZONE_KEY, Calendar.getInstance().timeZone.id)
        }
        activityScenario = ActivityScenario.launch(intent)

        // check calendar is displaying
        onView(allOf(
                withId(R.id.calendar),
                withClassName(`is`(CalendarView::class.java.canonicalName))
        )).check(matches(isDisplayed()))
        // check tab views are displaying
        onView(allOf(
                withId(R.id.title),
                withText(context.getString(R.string.range_start))
        )).check(matches(isDisplayed()))
        onView(allOf(
                withId(R.id.title),
                withText(context.getString(R.string.range_end))
        )).check(matches(isDisplayed()))
        // check action button
        onView(allOf(
                withId(R.id.action),
                withText(context.getString(R.string.apply))
        )).check(matches(isDisplayed()))
    }

    @Test
    fun testWithRingtoneBundle() {
        // intent with data need for ringtone
        val intent = Intent(context, ContentSelectorActivity::class.java).apply {
            action = ContentSelectorActivity.ACTION_REQUEST_AUDIO
            putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, "")
        }
        activityScenario = ActivityScenario.launch(intent)
        // wait for coroutine
        uiDevice.wait(Until.findObject(By.text(context.getString(R.string.my_ringtone))), 10000)
        // check title "my ringtone"
        onView(withText(context.getString(R.string.my_ringtone)))
                .check(matches(isDisplayed()))
        // check title system ringtone"
        onView(withText(context.getString(R.string.system_ringtone)))
                .check(matches(isDisplayed()))
        // check recycler view item "add new"
        onView(allOf(
                withClassName(`is`(ConstraintLayout::class.java.canonicalName)),
                withChild(withText(context.getString(R.string.add_new))),
        )).check(matches(isDisplayed()))
    }

    @Test
    fun testWithVibrationBundle() {
        // intent with data need for vibration
        val intent = Intent(context, ContentSelectorActivity::class.java).apply {
            action = ContentSelectorActivity.ACTION_REQUEST_VIBRATION
            putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, 0)
        }
        activityScenario = ActivityScenario.launch(intent)
        // wait for coroutine
        uiDevice.wait(Until.findObject(By.text(context.getString(R.string.no_vibrator))), 10000)
        // check title "no vibration"
        onView(withText(context.getString(R.string.no_vibrator)))
                .check(matches(isDisplayed()))
    }

    @Test
    fun testWithSnoozeBundle() {
        // intent with data need for snooze
        val intent = Intent(context, ContentSelectorActivity::class.java).apply {
            action = ContentSelectorActivity.ACTION_REQUEST_SNOOZE
            putExtra(ContentSelectorActivity.LAST_SELECTED_KEY, "")
        }
        activityScenario = ActivityScenario.launch(intent)
        // wait for coroutine
        uiDevice.wait(Until.findObject(By.text(context.getString(R.string.no_vibrator))), 10000)
        // check title "no snooze"
        onView(withOneOfText("Disable", "사용 안 함"))
                .check(matches(isDisplayed()))
    }

}