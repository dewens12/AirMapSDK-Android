package com.airmap.airmapsdk.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * Prevent ViewPager from consuming any touch events when isPagingEnabled is false. This prevents
 * buggy behavior when dragging annotations in the FreehandMap.
 */
public class SwipeDisableableViewPager extends ViewPager {

    private boolean isPagingEnabled = false;

    public SwipeDisableableViewPager(Context context) {
        super(context);
    }

    public SwipeDisableableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @return True if the event was handled, false otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return isPagingEnabled && super.onTouchEvent(event);
    }

    /**
     * @return True if the event was handled, false otherwise
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return isPagingEnabled && super.onInterceptTouchEvent(event);
    }

    /**
     * This method should be called to enable or disable swiping between pages in the viewpager
     *
     * @param enable Whether to enable or disable swiping
     */
    public void setPagingEnabled(boolean enable) {
        this.isPagingEnabled = enable;
    }
}
