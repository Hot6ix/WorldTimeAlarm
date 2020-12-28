package com.simples.j.worldtimealarm


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {
        val floatingActionButton = onView(
                allOf(withId(R.id.new_alarm),
                        childAtPosition(
                                allOf(withId(R.id.fragment_list),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                1),
                        isDisplayed()))
        floatingActionButton.perform(click())

        val extendedFloatingActionButton = onView(
                allOf(withId(R.id.action), withText("Create"),
                        childAtPosition(
                                allOf(withId(R.id.fragment_container),
                                        childAtPosition(
                                                withId(R.id.container),
                                                0)),
                                1),
                        isDisplayed()))
        extendedFloatingActionButton.perform(click())

        val switch_ = onView(
                allOf(withId(R.id.on_off),
                        withParent(allOf(withId(R.id.local_time_layout),
                                withParent(withId(R.id.list_item_layout)))),
                        isDisplayed()))
        switch_.check(matches(isDisplayed()))
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
}
