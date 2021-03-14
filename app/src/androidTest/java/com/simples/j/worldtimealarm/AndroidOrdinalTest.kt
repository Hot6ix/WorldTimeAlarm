package com.simples.j.worldtimealarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidOrdinalTest {

    @Test
    fun testSingleRecurrences() {
        // single ordinal
        val array1 = arrayOf(
                Pair(1, intArrayOf(1)),
        )

        val flatted1 = flatOrdinal(array1)

        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getNext(d, flatted1)

                assertTrue(r == Pair(1, 1))
            }
        }

        // multiple ordinals
        val array2 = arrayOf(
                Pair(1, intArrayOf(1, 2, 3)),
        )

        val flatted2 = flatOrdinal(array2)

        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getNext(d, flatted2)

                assertTrue(r == Pair(1, 1) || r == Pair(1, 2) || r == Pair(1, 3))
            }
        }

        // multiple ordinals
        val array3 = arrayOf(
                Pair(3, intArrayOf(2, 3)),
        )

        val flatted3 = flatOrdinal(array3)

        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getNext(d, flatted3)

                assertTrue(r == Pair(3, 2) || r == Pair(3, 3))
            }
        }
    }

    @Test
    fun testMultiRecurrencesAndMultiOrdinals() {
        val array1 = arrayOf(
                Pair(1, intArrayOf(1,2,3)),
                Pair(2, intArrayOf(1,2)),
                Pair(3, intArrayOf(3,4)),
        )

        val flatted1 = flatOrdinal(array1)

        var round = 1
        for(i in 1..7) {
            for(j in 1..5) {
                println("round=$round")
                val d = Pair(i, j)
                println("wanted=$flatted1")
                println("given=$d")
                val r = getNext(d, flatted1)
                println("selected=$r")

                if(flatted1.contains(d)) {
                    assertTrue(r == d)
                }

                when(d) {
                    Pair(1, 4) -> assertEquals(Pair(3, 4), r)
                    Pair(1, 5) -> assertEquals(Pair(1, 1), r)
                    Pair(2, 4) -> assertEquals(Pair(3, 4), r)
                    Pair(3, 2) -> assertEquals(Pair(1, 3), r)
                    Pair(4, 1) -> assertEquals(Pair(1, 2), r)
                    Pair(7, 1) -> assertEquals(Pair(1, 2), r)
                }
                round++
            }
        }

        val array2 = arrayOf(
                Pair(1, intArrayOf(1,2,3,4,5)),
                Pair(2, intArrayOf(1,2,3,4,5)),
                Pair(3, intArrayOf(1,2,3,4,5)),
                Pair(4, intArrayOf(1,2,3,4,5)),
                Pair(5, intArrayOf(1,2,3,4,5)),
                Pair(6, intArrayOf(1,2,3,4,5)),
                Pair(7, intArrayOf(1,2,3,4,5)),
        )

        val flatted2 = flatOrdinal(array2)

        round = 1
        for(i in 1..7) {
            for(j in 1..5) {
                val d = Pair(i, j)
                val r = getNext(d, flatted2)

                assertTrue(r == d)
                round++
            }
        }
    }

    private fun getNext(given: Pair<Int, Int>, array: List<Pair<Int, Int>>, ignoreSameDay: Boolean = false): Pair<Int, Int> {
        if(!ignoreSameDay) {
            array.find { it == given }?.let {
                // same day of week and ordinal
                return it
            }
        }

        // filter ordinal
        val sameOrAfterWeekOrdinal = array.filter { it.second >= given.second }
        val after = sameOrAfterWeekOrdinal.filterNot { it.first < given.first && it.second == given.second }
        return if(after.isEmpty()) {
            val availableDayOfWeek = sameOrAfterWeekOrdinal.filter { it.second > given.second }
            if(availableDayOfWeek.isEmpty()) array.first()
            else availableDayOfWeek.sortedBy { it.second }.first()
        }
        else {
            after.sortedBy { it.second }.first()
        }
    }

    private fun flatOrdinal(array: Array<Pair<Int, IntArray>>): List<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>()
        array.forEach { pair ->
            val flatted = pair.second.map {
                Pair(pair.first, it)
            }

            list.addAll(flatted)
        }

        list.sortBy {
            it.second
        }

        return list
    }

}