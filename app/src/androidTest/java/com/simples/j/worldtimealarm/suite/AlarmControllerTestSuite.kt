package com.simples.j.worldtimealarm.suite

import com.simples.j.worldtimealarm.utils.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
        AlarmControllerSimpleTest::class,
        AlarmControllerSingleRepeatingAndroidTest::class,
        AlarmControllerMultipleRepeatingAndroidTest::class,
        AlarmControllerSingleOrdinalTest::class,
        AlarmControllerMultipleOrdinalTest::class
)
class AlarmControllerTestSuite {

}