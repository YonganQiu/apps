
package com.android.launcher2;

import android.view.View;

import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class ScreenScrollAnimationBase implements ScreenScrollAnimation {
    private static float TRANSITION_PIVOT = 0.75f;
    private static float TRANSITION_MAX_ROTATION = 24f;

    @Override
    public void screenScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
    }

    @Override
    public void leftScreenOverScroll(float scrollProgress, View v) {
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), true);
        float rotation = -TRANSITION_MAX_ROTATION * scrollProgress;
        cl.setPivotX(TRANSITION_PIVOT * pageWidth);
        cl.setPivotY(pageHeight * 0.5f);
        cl.setRotationY(rotation);
        cl.setOverscrollTransformsDirty(true);
    }

    @Override
    public void rightScreenOverScroll(float scrollProgress, View v) {
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), false);
        float rotation = -TRANSITION_MAX_ROTATION * scrollProgress;
        cl.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
        cl.setPivotY(pageHeight * 0.5f);
        cl.setRotationY(rotation);
        cl.setOverscrollTransformsDirty(true);
    }

    @Override
    public void resetAnimationData(View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
    }

}
