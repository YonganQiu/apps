
package com.android.launcher2;

import android.view.View;

public class ScreenScrollAnimationBounce extends ScreenScrollAnimationBase {

    @Override
    public void screenScroll(float scrollProgress, View v) {
        super.screenScroll(scrollProgress, v);
        CellLayout cl = (CellLayout) v;
        int pageHeight = cl.getChildrenLayout().getMeasuredHeight();
        if (Math.abs(scrollProgress) == 1.0f || Math.abs(scrollProgress) == 0) {
            v.setFastTranslationX(0);
            v.setFastTranslationY(0);
        } else {
            v.setFastTranslationX(0);
            v.setFastTranslationY(-pageHeight * Math.abs(scrollProgress));
        }
        v.fastInvalidate();
    }

    @Override
    public void leftScreenOverScroll(float scrollProgress, View v) {
        super.leftScreenOverScroll(scrollProgress, v);
        v.setFastTranslationX(0);
        v.setFastTranslationY(0);
        v.fastInvalidate();
    }

    @Override
    public void rightScreenOverScroll(float scrollProgress, View v) {
        super.rightScreenOverScroll(scrollProgress, v);
    }

    @Override
    public void resetAnimationData(View v) {
        super.resetAnimationData(v);
        v.setFastTranslationX(0);
        v.setFastTranslationY(0);
        v.fastInvalidate();
    }

}
