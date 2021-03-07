package com.simples.j.worldtimealarm

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.TimePicker
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

object ViewMatcherExtension {

    fun withTime(h: Int, m: Int): Matcher<View> {
        return object : BaseMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("with time hour($h) and minute($m)")
            }

            override fun matches(item: Any?): Boolean {
                return (item as TimePicker).let {
                    it.hour == h && it.minute == m
                }
            }

        }
    }

    fun withTime(vararg times: Pair<Int, Int>): Matcher<View> {
        return object : BaseMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("with time list (${times.joinToString()})")
            }

            override fun matches(item: Any?): Boolean {
                return (item as TimePicker).let {
                    times.contains(Pair(it.hour, it.minute))
                }
            }

        }
    }

    fun withTextColor(id: Int): Matcher<View> {
        return object : BaseMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("with text color: $id")
            }

            override fun matches(item: Any?): Boolean {
                return (item as TextView).let {
                    it.currentTextColor == ContextCompat.getColor(it.context, id)
                }
            }
        }
    }

    fun withNeighbor(neighborMatcher: Matcher<View>): Matcher<View> {
        return object : BaseMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("with neighbor: $neighborMatcher")
            }

            override fun matches(item: Any?): Boolean {
                ((item as View).parent as ViewGroup).let {
                    it.forEach { view ->
                        if(neighborMatcher.matches(view)) return true
                    }
                }

                return false
            }
        }
    }

    fun withOneOfText(vararg text: String): Matcher<View> {
        return object : BaseMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("with text: ${text.joinToString()}")
            }

            override fun matches(item: Any?): Boolean {
                return if(item is TextView) {
                    text.contains(item.text.toString())
                } else false
            }
        }
    }

    fun withOneOfText(textList: List<String>): Matcher<View> {
        return object : BaseMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("with text: ${textList.joinToString()}")
            }

            override fun matches(item: Any?): Boolean {
                if(item is TextView) {
                    return textList.contains(item.text.toString())
                }

                return false
            }
        }
    }

     fun exists(): Matcher<View> {
        return object : BaseMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("check if view exists")
            }

            override fun matches(item: Any?): Boolean {
                return item != null && (item as View).isShown
            }
        }
    }
}