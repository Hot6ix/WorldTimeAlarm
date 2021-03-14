package com.simples.j.worldtimealarm

import com.simples.j.worldtimealarm.utils.TypeConverters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeConvertersTest {

    private val converters = TypeConverters()

    @Test
    fun testRepeats() {
        val sample1 = intArrayOf(1,2,3,4,5,6)
        val repeatToString1 = converters.repeatToString(sample1)
        val stringToRepeat1 = converters.stringToRepeat(repeatToString1)

        assertEquals(6, stringToRepeat1.size)
        assertTrue(stringToRepeat1.contentEquals(sample1))

        val sample2 = intArrayOf(1,2,3,4)
        val repeatToString2 = converters.repeatToString(sample2)
        val stringToRepeat2 = converters.stringToRepeat(repeatToString2)

        assertEquals(4, stringToRepeat2.size)
        assertTrue(stringToRepeat2.contentEquals(sample2))
    }

    @Test
    fun testVibration() {
        val sample1 = longArrayOf(0)
        val vibrationToString1 = converters.vibrationToString(sample1)
        val stringToVibration1 = converters.stringToVibration(vibrationToString1)

        assertEquals(1, stringToVibration1?.size)
        assertTrue(stringToVibration1.contentEquals(sample1))

        val sample2 = longArrayOf(0, 100, 0, 100)
        val vibrationToString2 = converters.vibrationToString(sample2)
        val stringToVibration2 = converters.stringToVibration(vibrationToString2)

        assertEquals(4, stringToVibration2?.size)
        assertTrue(stringToVibration2.contentEquals(sample2))
    }

    @Test
    fun testIntPairArray() {
        val sample1 = arrayOf(Pair(1, intArrayOf(1,2)))
        val arrayToString1 = converters.intPairArrayToString(sample1)
        val stringToArray1 = converters.stringToIntPairArray(arrayToString1)

        assertEquals(1, stringToArray1?.size)

        val sample2 = arrayOf(Pair(1, intArrayOf(1,2)), Pair(2, intArrayOf(1,2)))
        val arrayToString2 = converters.intPairArrayToString(sample2)
        val stringToArray2 = converters.stringToIntPairArray(arrayToString2)

        assertEquals(2, stringToArray2?.size)
    }
}