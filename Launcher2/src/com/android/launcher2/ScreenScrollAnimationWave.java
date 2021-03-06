
package com.android.launcher2;

import android.view.View;

import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class ScreenScrollAnimationWave implements ScreenScrollAnimation {
    @Override
    public void screenScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
        int pageWidth = cl.getMeasuredWidth();
        int pageHeight = cl.getMeasuredHeight();
        if (Math.abs(scrollProgress) == 1.0f || Math.abs(scrollProgress) == 0) {
            v.setPivotX(pageWidth / 2.0f);
            v.setPivotY(pageHeight / 2.0f);
            v.setFastScaleX(1.0f);
            v.setFastScaleY(1.0f);
        } else {
            if (scrollProgress > 0) {
                // left screen.
                v.setPivotX(pageWidth);
                v.setPivotY(pageHeight / 2.0f);
                v.setFastScaleX(1 - scrollProgress * 0.5f);
                v.setFastScaleY(1 - scrollProgress * 0.5f);

            } else {
                // right screen.
                v.setPivotX(0);
                v.setPivotY(pageHeight / 2.0f);
                v.setFastScaleX(1 + scrollProgress * 0.5f);
                v.setFastScaleY(1 + scrollProgress * 0.5f);

            }
        }

        v.fastInvalidate();
    }

    @Override
    public void leftScreenOverScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), true);
        cl.setOverscrollTransformsDirty(true);
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        v.setPivotX(pageWidth + pageWidth / 3.0f);
        v.setPivotY(pageHeight / 2.0f);
        if (scrollProgress < -0.4) {
            v.setFastScaleX(0.6f);
            v.setFastScaleY(0.6f);
        } else {
            v.setFastScaleX(1 + scrollProgress);
            v.setFastScaleY(1 + scrollProgress);
        }
        v.fastInvalidate();
    }

    @Override
    public void rightScreenOverScroll(float scrollProgress, View v) {
        CellLayout cl = (CellLayout) v;
        cl.setOverScrollAmount(Math.abs(scrollProgress), false);
        cl.setOverscrollTransformsDirty(true);
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        v.setPivotX(-pageWidth / 3.0f);
        v.setPivotY(pageHeight / 2.0f);

        if (scrollProgress > 0.4) {
            v.setFastScaleX(0.6f);
            v.setFastScaleY(0.6f);
        } else {
            v.setFastScaleX(1 - scrollProgress);
            v.setFastScaleY(1 - scrollProgress);
        }
        v.fastInvalidate();
    }

    @Override
    public void resetAnimationData(View v) {
        CellLayout cl = (CellLayout) v;
        cl.resetOverscrollTransforms();
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        v.setPivotX(pageWidth / 2.0f);
        v.setPivotY(pageHeight / 2.0f);
        v.setFastScaleX(1.0f);
        v.setFastScaleY(1.0f);
        v.fastInvalidate();
    }

}
