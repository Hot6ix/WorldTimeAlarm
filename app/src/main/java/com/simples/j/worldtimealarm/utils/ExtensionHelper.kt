package com.simples.j.worldtimealarm.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.delay

object ExtensionHelper {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    suspend fun <T> retryIO(times: Int = 3, delay: Long = 500, block: () -> T): T {
        repeat(times) {
            try {
                return block()
            } catch (e: Exception) {
                e.printStackTrace()

                crashlytics.recordException(e.fillInStackTrace())
            }
            delay(delay)
        }

        throw Exception("Retry Failed")
    }
}