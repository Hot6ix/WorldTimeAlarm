package com.simples.j.worldtimealarm

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

@SmallTest
class MiscTest {

    private val fixedSizeStack = Stack<Int>()

    @Test
    fun testFixedSizeStack() {
        for(i in 0 until 5) {
            stack(i)
        }
        assertEquals(5, fixedSizeStack.size)
        assertEquals(0, fixedSizeStack.firstElement())
        assertEquals(4, fixedSizeStack.lastElement())

        stack(5)
        assertEquals(5, fixedSizeStack.size)
        assertEquals(1, fixedSizeStack.firstElement())
        assertEquals(5, fixedSizeStack.lastElement())

        for(i in 10 until 20) {
            stack(i)
        }

        assertEquals(5, fixedSizeStack.size)
    }

    private fun stack(n: Int) {
        while(fixedSizeStack.size >= 5) {
            fixedSizeStack.removeFirstOrNull()
        }

        fixedSizeStack.push(n)
    }

    companion object {
        const val MAX_STACK_SIZE = 5
    }
}