package com.simples.j.worldtimealarm.suite

import com.simples.j.worldtimealarm.MainActivityUITest
import com.simples.j.worldtimealarm.TimeZonePickerActivityUITest
import com.simples.j.worldtimealarm.fragments.AlarmGeneratorFragmentUITest
import com.simples.j.worldtimealarm.fragments.AlarmListFragmentUITest
import com.simples.j.worldtimealarm.fragments.ContentSelectorFragmentUITest
import com.simples.j.worldtimealarm.fragments.WorldTimeFragmentUITest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
        MainActivityUITest::class,
        TimeZonePickerActivityUITest::class,
        AlarmGeneratorFragmentUITest::class,
        AlarmListFragmentUITest::class,
        ContentSelectorFragmentUITest::class,
        WorldTimeFragmentUITest::class
)
class UITestSuite {
}