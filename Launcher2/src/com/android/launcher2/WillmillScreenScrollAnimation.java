package com.android.launcher2;

import android.util.Log;
import android.view.View;
import com.android.launcher2.Workspace.ScreenScrollAnimation;


/**
 * added by zhongheng.zheng 2012.7.12 for willmill screen scroll animation
 *
 */

public class WillmillScreenScrollAnimation implements ScreenScrollAnimation {
	private static float TRANSITION_MAX_MULTIPLE = 0.3f;

	@Override
	public void screenScroll(float scrollProgress, View v) {
		int pageWidth = v.getMeasuredWidth();
		int pageHeight = v.getMeasuredHeight();
		int pivotY = 0;
		float angle = (float) (Math.atan(1.0 * pageWidth
				/ (pageHeight - pivotY)) * 180 / Math.PI);
		if (Math.abs(scrollProgress) == 1.0f) {
			v.setPivotX(pageWidth / 2.0f);
			v.setPivotY(pivotY);
			v.setRotation(0);
		} else {
			if (scrollProgress > 0) {
				// left screen.
				v.setPivotX((pageWidth / 2.0f) * (1 + scrollProgress));
				v.setPivotY(pivotY);
				v.setRotation(angle * scrollProgress);
			} else {
				// right screen.
				v.setPivotX((pageWidth / 2.0f) * (1 + scrollProgress));
				v.setPivotY(pivotY);
				v.setRotation(angle * scrollProgress);
			}
		}

	}

	@Override
	public void leftScreenOverScroll(float scrollProgress, View v) {
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

}
