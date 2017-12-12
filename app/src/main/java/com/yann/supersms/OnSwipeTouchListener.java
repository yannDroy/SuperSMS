package com.yann.supersms;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/** Classe gérant les mouvements de swipe
 * Created by yann on 28/08/16.
 */
public class OnSwipeTouchListener implements OnTouchListener {
    private final GestureDetector gestureDetector;

    public OnSwipeTouchListener (Context ctx){
        gestureDetector = new GestureDetector(ctx, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD_X = 100;
        private static final int SWIPE_THRESHOLD_Y = 500;
        private static final int SWIPE_VELOCITY_THRESHOLD = 0;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            singleTap();

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            doubleTap();

            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            longTap();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffY) <= SWIPE_THRESHOLD_Y) {
                    if (Math.abs(diffX) >= SWIPE_THRESHOLD_X && Math.abs(velocityX) >= SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0)
                            onSwipeRight();
                        else
                            onSwipeLeft();
                    }
                    result = true;
                }
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    public void onSwipeRight() {
    }

    public void onSwipeLeft() {
    }

    public void singleTap() {
    }

    public void doubleTap() {
    }

    public void longTap() {
    }
}
