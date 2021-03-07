package com.simples.j.worldtimealarm

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import com.google.android.material.appbar.CollapsingToolbarLayout
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf


object ViewActionExtension {
    fun setQueryAndSubmit(text: String): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(SearchView::class.java)
            }

            override fun getDescription(): String = "put $text on search view"

            override fun perform(uiController: UiController?, view: View?) {
                (view as SearchView).setQuery(text, true)
            }

        }
    }

    fun setTime(h: Int, m: Int): ViewAction {
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

    class NestedScrollViewExtension(scrolltoAction: ViewAction = ViewActions.scrollTo()) : ViewAction by scrolltoAction {
        override fun getConstraints(): Matcher<View> {
            return allOf(withEffectiveVisibility(Visibility.VISIBLE),
                    isDescendantOfA(Matchers.anyOf(isAssignableFrom(NestedScrollView::class.java),
                            isAssignableFrom(ScrollView::class.java),
                            isAssignableFrom(HorizontalScrollView::class.java),
                            isAssignableFrom(ListView::class.java))))
        }
    }
}