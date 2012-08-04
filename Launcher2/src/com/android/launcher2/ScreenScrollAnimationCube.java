
package com.android.launcher2;

import android.view.View;

public class ScreenScrollAnimationCube extends ScreenScrollAnimationBase {

    @Override
    public void screenScroll(float scrollProgress, View v) {
        super.screenScroll(scrollProgress, v);
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        if (Math.abs(scrollProgress) == 1.0f || Math.abs(scrollProgress) == 0) {
            v.setPivotX(pageWidth / 2.0f);
            v.setPivotY(pageHeight / 2.0f);
            v.setFastRotationY(0);
        } else {
            if (scrollProgress > 0) {
                // left screen.
                v.setPivotX(pageWidth);
                v.setPivotY(pageHeight / 2.0f);
                v.setFastRotationY(-90 * scrollProgress);
            } else {
                // right screen.
                v.setPivotX(0);
                v.setPivotY(pageHeight / 2.0f);
                v.setFastRotationY(-90 * scrollProgress);
            }
        }

        v.fastInvalidate();
    }

    @Override
    public void resetAnimationData(View v) {
        super.resetAnimationData(v);
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        v.setPivotX(pageWidth / 2.0f);
        v.setPivotY(pageHeight / 2.0f);
        v.setFastRotationY(0);
        v.fastInvalidate();
    }

}
