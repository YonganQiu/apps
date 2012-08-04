
package com.android.launcher2;

import android.view.View;
import android.view.ViewConfiguration;

public class ScreenScrollAnimationCascade extends ScreenScrollAnimationBase {

    @Override
    public void screenScroll(float scrollProgress, View v) {
        super.screenScroll(scrollProgress, v);

        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        float translationX = 0;
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float alpha = 1.0f;

        if (scrollProgress < 0 && scrollProgress > -1.0f) {
            // right screen.
            translationX = scrollProgress * pageWidth;
            scaleX = 1 + scrollProgress * 0.5f;
            scaleY = 1 + scrollProgress * 0.5f;
            alpha = 1 + scrollProgress;
        }

        v.setPivotX(pageWidth / 2.0f);
        v.setPivotY(pageHeight / 2.0f);
        v.setFastTranslationX(translationX);
        v.setFastScaleX(scaleX);
        v.setFastScaleY(scaleY);
        v.setFastAlpha(alpha);
        if (alpha < ViewConfiguration.ALPHA_THRESHOLD) {
            v.setVisibility(View.INVISIBLE);
        } else if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
        v.fastInvalidate();
    }

    @Override
    public void leftScreenOverScroll(float scrollProgress, View v) {
        v.setFastTranslationX(0);
        v.setFastScaleX(1.0f);
        v.setFastScaleY(1.0f);
        v.setFastAlpha(1.0f);
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
        super.leftScreenOverScroll(scrollProgress, v);
    }

    @Override
    public void rightScreenOverScroll(float scrollProgress, View v) {
        v.setFastTranslationX(0);
        v.setFastScaleX(1.0f);
        v.setFastScaleY(1.0f);
        v.setFastAlpha(1.0f);
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
        super.rightScreenOverScroll(scrollProgress, v);
    }

    @Override
    public void resetAnimationData(View v) {
        super.resetAnimationData(v);
        int pageWidth = v.getMeasuredWidth();
        int pageHeight = v.getMeasuredHeight();
        v.setPivotX(pageWidth / 2.0f);
        v.setPivotY(pageHeight / 2.0f);
        v.setFastTranslationX(0);
        v.setFastScaleX(1.0f);
        v.setFastScaleY(1.0f);
        v.setFastAlpha(1.0f);
        if (v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
        v.fastInvalidate();
    }

}
