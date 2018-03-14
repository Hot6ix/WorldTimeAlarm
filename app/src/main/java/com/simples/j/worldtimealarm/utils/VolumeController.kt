package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.media.AudioManager

/**
 * Created by j on 10/03/2018.
 *
 */
class VolumeController(context: Context, private var expectVolume: Int): Thread() {

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun run() {
        increaseVolume(expectVolume)
    }

    private fun increaseVolume(max: Int) {
        var currentVolume = 1
        while(currentVolume <= max) {
            currentVolume++
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0)
            sleep(500)
        }

    }

}