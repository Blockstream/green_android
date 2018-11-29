package com.greenaddress.greenbits.ui.components;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class DetectSwipeGestureListener extends GestureDetector.SimpleOnGestureListener {

    private final OnSwipeListener onSwipeListener;
    public interface OnSwipeListener {
        void onSwipe();
    }

    // Minimal x and y axis swipe distance.
    private static int MIN_SWIPE_DISTANCE_X = 200;
    private static int MIN_SWIPE_DISTANCE_Y = 200;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public DetectSwipeGestureListener(final OnSwipeListener swipeListener){
        onSwipeListener = swipeListener;
    }

    /* This method is invoked when a swipe gesture happened. */
    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {

        // Get swipe delta value in x axis.
        final float deltaX = e1.getX() - e2.getX();

        // Get swipe delta value in y axis.
        final float deltaY = e1.getY() - e2.getY();

        // Get absolute value.
        final float deltaXAbs = Math.abs(deltaX);
        final float deltaYAbs = Math.abs(deltaY);

        // Only when swipe distance between minimal and maximal distance value then we treat it as effective swipe
        if ((deltaXAbs >= MIN_SWIPE_DISTANCE_X) && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
            if (deltaX > 0)
                onSwipeLeft();
            else
                onSwipeRight();
        } else if ((deltaYAbs >= MIN_SWIPE_DISTANCE_Y) && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
            if (deltaY > 0)
                onSwipeTop();
            else
                onSwipeBottom();
        }
        return true;
    }

    // Invoked when single tap screen.
    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        return true;
    }

    // Invoked when double tap screen.
    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        return true;
    }

    public boolean onSwipeRight() {
        onSwipeListener.onSwipe();
        return true;
    }

    public boolean onSwipeLeft() {
        onSwipeListener.onSwipe();
        return true;
    }

    public boolean onSwipeTop() {
        return false;
    }

    public boolean onSwipeBottom() {
        return false;
    }
}