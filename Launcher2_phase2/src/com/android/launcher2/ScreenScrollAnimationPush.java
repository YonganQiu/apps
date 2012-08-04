
package com.android.launcher2;

import android.view.View;

/**
 * added by zhongheng.zheng 2012.7.16 for Push screen scroll animation
 * 
 */

import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class ScreenScrollAnimationPush implements ScreenScrollAnimation {
    private static float TRANSITION_MAX_MULTIPLE = 0.6f;

    @Override
    public void screenScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        if (Math.abs(scrollProgress) == 1.0f) {
            v.setPivotX(pageWidth / 2.0f);
            v.setPivotY(pageHeight / 2.0f);
            v.setScaleX(1);
        } else {
            if (scrollProgress > 0) {
                // left screen.
                v.setPivotX(pageWidth);
                v.setScaleX(1 - scrollProgress);
            } else {
                // right screen.
                v.setPivotX(0);
                v.setScaleX(1 + scrollProgress);
            }
        }

    }

    @Override
    public void leftScreenOverScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), true);
        cl.setOverscrollTransformsDirty(true);
        int pageWidth = v.getMeasuredWidth();
        v.setPivotX(pageWidth);
        v.setScaleX(1 + scrollProgress * TRANSITION_MAX_MULTIPLE);
    }

    @Override
    public void rightScreenOverScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), false);
        cl.setOverscrollTransformsDirty(true);
        v.setPivotX(0);
        v.setScaleX(1 - scrollProgress * TRANSITION_MAX_MULTIPLE);
    }

    @Override
    public void resetAnimationData(View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        v.setPivotX((pageWidth / 2.0f));
        v.setPivotY(pageHeight / 2.0f);
        v.setScaleX(1);
    }
}
