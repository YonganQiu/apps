
package com.android.launcher2;

import android.view.View;

import com.android.launcher2.Workspace.ScreenScrollAnimation;

/**
 * added by zhongheng.zheng 2012.7.12 for Windmill screen scroll animation
 */

public class ScreenScrollAnimationWindmill implements ScreenScrollAnimation {
    private static float TRANSITION_MAX_MULTIPLE = 0.3f;

    @Override
    public void screenScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        int pivotY = 0;
        float angle = (float) (Math.atan(1.0 * pageWidth
                / (pageHeight - pivotY)) * 180 / Math.PI);
        if (Math.abs(scrollProgress) == 1.0f || Math.abs(scrollProgress) == 0) {
            v.setPivotX(pageWidth / 2.0f);
            v.setPivotY(pageHeight / 2.0f);
            v.setRotation(0);
        } else {
            v.setPivotX((pageWidth / 2.0f) * (1 + scrollProgress));
            v.setPivotY(pivotY);
            v.setRotation(angle * scrollProgress);
        }

    }

    @Override
    public void leftScreenOverScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), true);
        cl.setOverscrollTransformsDirty(true);
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        int pivotY = 0;
        float angle = (float) (Math.atan(1.0 * pageWidth
                / (pageHeight - pivotY)) * 180 / Math.PI);
        v.setPivotX((pageWidth / 2.0f));
        v.setPivotY(pivotY);
        v.setRotation(angle * scrollProgress * TRANSITION_MAX_MULTIPLE);
        v.setTranslationX(-scrollProgress * pageWidth * TRANSITION_MAX_MULTIPLE);

    }

    @Override
    public void rightScreenOverScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), false);
        cl.setOverscrollTransformsDirty(true);
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        int pivotY = 0;
        float angle = (float) (Math.atan(1.0 * pageWidth
                / (pageHeight - pivotY)) * 180 / Math.PI);
        v.setPivotX((pageWidth / 2.0f));
        v.setPivotY(pivotY);
        v.setRotation(angle * scrollProgress * TRANSITION_MAX_MULTIPLE);
        v.setTranslationX(-scrollProgress * pageWidth * TRANSITION_MAX_MULTIPLE);
    }

    @Override
    public void resetAnimationData(View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        v.setPivotX((pageWidth / 2.0f));
        v.setPivotY(pageHeight / 2.0f);
        v.setRotation(0);
        v.setTranslationX(0);
    }

}
