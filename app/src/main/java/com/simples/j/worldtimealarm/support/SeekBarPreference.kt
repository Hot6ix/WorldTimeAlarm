package com.simples.j.worldtimealarm.support

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.media.AudioManager.STREAM_ALARM
import android.preference.Preference
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import com.simples.j.worldtimealarm.R

/**
 * Created by j on 11/03/2018.
 *
 */
class SeekBarPreference(context: Context, attributeSet: AttributeSet): Preference(context, attributeSet), SeekBar.OnSeekBarChangeListener {

    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private lateinit var seekBar: SeekBar
    private lateinit var image: ImageView

    override fun onCreateView(parent: ViewGroup?): View {
        return super.onCreateView(parent)
    }

    override fun onBindView(view: View?) {
        super.onBindView(view)

        val layout = view?.findViewById<View>(R.id.seekbar_preference_layout)
        if(layout != null) {
            image = layout.findViewById<ImageView>(R.id.volume_level)
            seekBar = layout.findViewById<SeekBar>(R.id.pref_seekbar)
            seekBar.max = audioManager.getStreamMaxVolume(STREAM_ALARM);
            seekBar.progress = audioManager.getStreamVolume(STREAM_ALARM);
            image.setImageResource(if(audioManager.getStreamVolume(STREAM_ALARM) == 0) R.drawable.ic_volume_mute else R.drawable.ic_volume_high)

            val observer = object: ContentObserver(seekBar.handler) {
                override fun onChange(selfChange: Boolean) {
                    seekBar.progress = audioManager.getStreamVolume(STREAM_ALARM);
                }
            }

            seekBar.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View?) {
                    context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
                }

                override fun onViewDetachedFromWindow(view: View?) {
                    context.contentResolver.unregisterContentObserver(observer)
                }
            })
            seekBar.setOnSeekBarChangeListener(this)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, value: Int, bool: Boolean) {
        audioManager.setStreamVolume(STREAM_ALARM, value, 0);
        image.setImageResource(if(value == 0) R.drawable.ic_volume_mute else R.drawable.ic_volume_high)
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {}

    override fun onStopTrackingTouch(p0: SeekBar?) {}

}