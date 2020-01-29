package com.simples.j.worldtimealarm.utils

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class UnswipeableViewPager : ViewPager {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false

    override fun onTouchEvent(ev: MotionEvent?): Boolean = false

}
