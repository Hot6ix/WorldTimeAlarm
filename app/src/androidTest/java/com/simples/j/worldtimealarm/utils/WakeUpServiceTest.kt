package com.simples.j.worldtimealarm.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.simples.j.worldtimealarm.AlarmReceiver
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.TestUtils
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class WakeUpServiceTest {

    @get:Rule
    var serviceTestRule: ServiceTestRule = ServiceTestRule()

    private lateinit var keyguardManager: KeyguardManager
    private lateinit var uiDevice: UiDevice
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        Intents.init()
    }

    @After
    fun terminate() {
        Intents.release()
    }

    @Test
    fun testInitiationOfService() {
        // set up WakeUpService intent without extra data
        val serviceIntent = Intent(context, WakeUpService::class.java).apply {
            val bundle = Bundle().apply {
                putParcelable(AlarmReceiver.ITEM, TestUtils.createAlarm())
            }
            putExtra(AlarmReceiver.OPTIONS, bundle)
            putExtra(AlarmReceiver.EXPIRED, false)
        }
        serviceTestRule.startService(serviceIntent)

        // WakeUpService closes too fast, wait 1s for WakeUpActivity to start
        Thread.sleep(5000)

        // check notification
        uiDevice.openNotification()
        uiDevice.wait(Until.hasObject(By.text(context.resources.getString(R.string.alarm))), 1000)
        val title = uiDevice.findObject(By.text(context.resources.getString(R.string.alarm)))
        assertTrue(title != null)

        val dismissBtn = uiDevice.findObject(By.text(context.resources.getString(R.string.dismiss)))
        dismissBtn.click()
    }

}