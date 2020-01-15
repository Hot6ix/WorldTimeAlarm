package com.simples.j.worldtimealarm.utils

import android.content.Context
import android.media.AudioManager

/**
 * Created by j on 10/03/2018.
 *
 */
class VolumeController(context: Context): Thread() {

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun run() {
        increaseVolume()
    }

    private fun increaseVolume() {
        var currentVolume = 1
        while(currentVolume <= audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)) {
            currentVolume++
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0)
            sleep(500)
        }

    }

}