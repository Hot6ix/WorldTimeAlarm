package com.simples.j.worldtimealarm.fragments

import android.content.res.Configuration
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.simples.j.worldtimealarm.AlarmGeneratorActivity
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.utils.MediaCursor
import junit.framework.Assert.assertEquals
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.TypeSafeMatcher
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AlarmListFragmentUITest {
    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        activityScenario = activityScenarioRule.scenario
        activityScenario.apply {
            moveToState(Lifecycle.State.CREATED)
            moveToState(Lifecycle.State.STARTED)
            moveToState(Lifecycle.State.RESUMED)
        }

        Intents.init()
    }

    @Test
    fun a_checkIfNewAlarmButtonIsWorking() {
        onView(withId(R.id.new_alarm)).perform(click())
        intended(hasComponent(AlarmGeneratorActivity::class.java.name))
    }

    @Test
    fun b_checkIfNewAlarmHasCreated() {
        createNewAlarm(10, 0)
    }

    @Test
    fun c_checkIfAlarmHasDeleted() {
        val itemView = createNewAlarm(17, 30)

        // Check if new alarm has been delete by swiping
        itemView.perform(GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT, GeneralLocation.CENTER_LEFT, Press.FINGER))
        itemView.check(doesNotExist())
    }

    private fun createNewAlarm(h: Int = -1, m: Int = -1): ViewInteraction {
        // Check if AlarmGeneratorActivity has launched
        a_checkIfNewAlarmButtonIsWorking()

        // Create new alarm
        val calendar = Calendar.getInstance().apply {
            if(h >= 0 && m >= 0) {
                set(Calendar.HOUR, h)
                set(Calendar.MINUTE, m)
                if(h in 0..11) set(Calendar.AM_PM, Calendar.AM)
                else set(Calendar.AM_PM, Calendar.PM)
            }
        }

        // Set provided hour and minute
        if(h > 0 && m >= 0) {
            onView(withClassName(equalTo(TimePicker::class.java.name))).perform(setTime(h, m))
        }

        onView(allOf(withId(R.id.action), withText(R.string.create))).perform(click())

        val localTime = DateFormat.format(MediaCursor.getLocalizedTimeFormat(), calendar).toString()

        // Check if new alarm has been created
        val itemView = onView(
                allOf(withId(R.id.local_time), withText(localTime),
                        withParent(allOf(withId(R.id.local_time_layout),
                                withParent(withId(R.id.list_item_layout))))
                )
        )
        itemView.check(matches(isDisplayed()))

        return itemView
    }

//    @Test
    fun rotateFragment() {
        activityScenarioRule.scenario.onActivity {
            it.requestedOrientation = Configuration.ORIENTATION_LANDSCAPE

            // Orientation change needs time
            Thread.sleep(100)

            assertEquals(Configuration.ORIENTATION_LANDSCAPE, it.resources.configuration.orientation)
        }
    }

    @After
    fun finish() {
        Intents.release()

        activityScenario.apply {
            moveToState(Lifecycle.State.DESTROYED)
        }
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }

    private fun setTime(h: Int, m: Int): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(TimePicker::class.java)

            override fun getDescription(): String = "Set provided hour and minute into TimePicker"

            override fun perform(uiController: UiController?, view: View?) {
                (view as TimePicker).apply {
                    hour = h
                    minute = m
                }
            }

        }
    }
}