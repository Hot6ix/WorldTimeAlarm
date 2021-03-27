 package com.simples.j.worldtimealarm.fragments

import android.content.Context
import android.content.pm.ActivityInfo
import android.text.format.DateFormat
import android.view.View
import android.widget.TimePicker
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions.setTime
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.simples.j.worldtimealarm.AlarmGeneratorActivity
import com.simples.j.worldtimealarm.ViewMatcherExtension.withTextColor
import com.simples.j.worldtimealarm.ViewMatcherExtension.withTime
import com.simples.j.worldtimealarm.MainActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.support.AlarmListAdapter
import com.simples.j.worldtimealarm.utils.DatabaseManager
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.android.synthetic.main.fragment_alarm_list.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.*

@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AlarmListFragmentUITest {
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
    fun a_checkIfNewAlarmButtonIsWorking() {
        onView(withId(R.id.new_alarm)).perform(click())
        intended(hasComponent(AlarmGeneratorActivity::class.java.name))

        onView(allOf(withId(R.id.action), withText(R.string.create))).check(matches(isDisplayed()))
    }

    @Test
    fun b_checkIfNewAlarmIsCreated() {
        createNewAlarm(10, 0)
    }

    @Test
    fun c_checkIfClickingAlarmIsWorking() {
        val itemView = getAlarmItemViewByTime(10, 0)

        itemView.perform(click())
        intended(hasComponent(AlarmGeneratorActivity::class.java.name))

        onView(allOf(withId(R.id.action), withText(R.string.apply))).check(matches(isDisplayed()))
        onView(
                allOf(withId(R.id.time_picker), withTime(10, 0))
        ).check(matches(isDisplayed()))
    }

    @Test
    fun d_checkIfAlarmIsDisabled() {
        val localTime = getLocalTimeText(10, 0)

        onView(
                allOf(withId(R.id.on_off), withParent(
                        allOf(withId(R.id.local_time_layout), withChild(
                                allOf(withId(R.id.local_time), withText(localTime))
                        ))
                ))
        ).perform(click())

        // TextView: local time
        onView(
                allOf(withId(R.id.local_time), withText(localTime))
        ).check(matches(withTextColor(R.color.textColorDisabled)))

        // TextView: repeat
        onView(
                allOf(withId(R.id.repeat), withParent(
                        allOf(withId(R.id.local_time_layout), withChild(
                                allOf(withId(R.id.local_time), withText(localTime))
                        ))
                ))
        ).check(matches(withTextColor(R.color.textColorDisabled)))
    }

    @Test
    fun e_checkIfAlarmIsDeleted() {
        val itemView = getAlarmItemViewByTime(10, 0)

        // Check if new alarm has been delete by swiping
        itemView.perform(GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER_RIGHT, GeneralLocation.CENTER_LEFT, Press.FINGER))
        itemView.check(matches(not(isDisplayed())))
    }

    @Test
    fun f_checkSwapping() {
        val first = createNewAlarm(9, 30)
        val second = createNewAlarm(21, 30)

        activityScenario.onActivity {
            (it.supportFragmentManager.findFragmentByTag(AlarmListFragment.TAG) as AlarmListFragment).let { fragment ->
                val p = getItemPosition(fragment.alarmList, getLocalTimeText(9, 30))

                assertEquals(0, p)
            }
        }
    }

    @Test
    fun g_orientation_change_test() {
        activityScenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        Thread.sleep(10000)
    }

    private fun getLocalTimeText(h: Int, m: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }

        return DateFormat.format(MediaCursor.getLocalizedTimeFormat(), calendar).toString()
    }

    private fun getAlarmItemViewByTime(h: Int, m: Int): ViewInteraction {

        return onView(
                allOf(withId(R.id.local_time), withText(getLocalTimeText(h, m)),
                        withParent(allOf(withId(R.id.local_time_layout),
                                withParent(withId(R.id.list_item_layout))))
                )
        )
    }

    private fun createNewAlarm(h: Int = -1, m: Int = -1): ViewInteraction {
        // Check if AlarmGeneratorActivity has launched
        onView(withId(R.id.new_alarm)).perform(click())
        onView(allOf(withId(R.id.action), withText(R.string.create))).check(matches(isDisplayed()))

        // Create new alarm
        val calendar = Calendar.getInstance().apply {
            if(h >= 0 && m >= 0) {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
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

    @After
    fun finish() {
        Intents.release()

        activityScenario.apply {
            moveToState(Lifecycle.State.DESTROYED)
        }
    }

    private fun getItemPosition(recyclerView: RecyclerView, text: String): Int {
        recyclerView.adapter?.let {
            for(i in 0 until it.itemCount) {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as AlarmListAdapter.ViewHolder

                if(viewHolder.localTime.text == text) {
                    return i
                }
            }
        }

        return -1
    }

    private fun moveItemTo(source: Int, destination: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun getDescription(): String = "Long press"

            override fun perform(uiController: UiController?, view: View?) {
                val recyclerView = view as RecyclerView

                val sourceItem = recyclerView.findViewHolderForAdapterPosition(source)
                val destinationItem = recyclerView.findViewHolderForAdapterPosition(destination)

                recyclerView.scrollToPosition(source)
                uiController?.loopMainThreadUntilIdle()

                val downMovement = MotionEvents.sendDown(uiController, GeneralLocation.VISIBLE_CENTER.calculateCoordinates(sourceItem?.itemView), Press.FINGER.describePrecision())
                uiController?.loopMainThreadForAtLeast(2000)

                val sourceViewCenter = GeneralLocation.VISIBLE_CENTER.calculateCoordinates(sourceItem?.itemView)

                val destinationViewCenter =
                        if(source < destination) GeneralLocation.BOTTOM_CENTER.calculateCoordinates(destinationItem?.itemView)
                        else GeneralLocation.TOP_CENTER.calculateCoordinates(destinationItem?.itemView)

                val steps = interpolate(sourceViewCenter, destinationViewCenter)

                for(element in steps) {
                    if (!MotionEvents.sendMovement(uiController, downMovement.down, element)) {
                        MotionEvents.sendCancel(uiController, downMovement.down)
                    }
                }

                MotionEvents.sendUp(uiController, downMovement.down)
                downMovement.down.recycle()
            }

        }

    }

    private val SWIPE_EVENT_COUNT = 10

    private fun interpolate(start: FloatArray, end: FloatArray): Array<FloatArray> {
        val res = Array(SWIPE_EVENT_COUNT) { FloatArray(2) }

        for (i in 1..SWIPE_EVENT_COUNT) {
            res[i - 1][0] = start[0] + (end[0] - start[0]) * i / SWIPE_EVENT_COUNT
            res[i - 1][1] = start[1] + (end[1] - start[1]) * i / SWIPE_EVENT_COUNT
        }

        return res
    }
}