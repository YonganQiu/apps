package com.android.launcher2;

import android.view.View;

import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class CubeScreenScrollAnimation implements ScreenScrollAnimation{
	private static float TRANSITION_PIVOT = 0.65f;
	private static float TRANSITION_MAX_ROTATION = 22;
	@Override
	public void screenScroll(float scrollProgress, View v) {
		int pageWidth = v.getMeasuredWidth();
		int pageHeight = v.getMeasuredHeight();
		if (Math.abs(scrollProgress) == 1.0f) {
			v.setPivotX(pageWidth / 2.0f);
			v.setPivotY(pageHeight / 2.0f);
			v.setFastRotationY(0);
		}else{
			if (scrollProgress > 0) {
				//left screen.
				v.setPivotX(pageWidth);
				v.setPivotY(pageHeight / 2.0f);
				v.setFastRotationY(-90 * scrollProgress);
			}else{
				//right screen.
	        	v.setPivotX(0);
	        	v.setPivotY(pageHeight / 2.0f);
	        	v.setFastRotationY(-90 * scrollProgress);
	        }
		}
		
		v.fastInvalidate();
	}

	@Override
	public void leftScreenOverScroll(float scrollProgress, View v) {
		int pageWidth = v.getMeasuredWidth();
		int pageHeight = v.getMeasuredHeight();
		v.setPivotX(TRANSITION_PIVOT * pageWidth);
		v.setPivotY(pageHeight / 2.0f);
		v.setFastRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
		v.fastInvalidate();
	}

	@Override
	public void rightScreenOverScroll(float scrollProgress, View v) {
		int pageWidth = v.getMeasuredWidth();
		int pageHeight = v.getMeasuredHeight();
		v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
		v.setPivotY(pageHeight / 2.0f);
		v.setFastRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
		v.fastInvalidate();
	}

}
