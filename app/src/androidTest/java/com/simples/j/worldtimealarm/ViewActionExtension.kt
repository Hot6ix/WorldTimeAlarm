package com.simples.j.worldtimealarm

import android.view.View
import android.widget.TimePicker
import androidx.appcompat.widget.SearchView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher

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