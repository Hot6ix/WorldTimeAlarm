package com.simples.j.worldtimealarm.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.etc.C
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

    fun getSimpleNotification(context: Context, title: String, content: String, contentIntent: PendingIntent? = null): Notification {
        val builder = NotificationCompat.Builder(context, C.DEFAULT_NOTIFICATION_CHANNEL)

        builder
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_action_alarm_white)
            .setContentTitle(title)
            .setContentText(content)
            .setGroup(C.GROUP_DEFAULT)
            .setContentIntent(contentIntent)

        return builder.build()
    }
}