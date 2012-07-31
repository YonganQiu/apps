package com.android.launcher2;

import android.view.View;

import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class ScreenScrollAnimationWave implements ScreenScrollAnimation {
	@Override
	public void screenScroll(float scrollProgress, View v) {
		// TODO Auto-generated method stub
		CellLayout cl = (CellLayout) v;
		int pageWidth = cl.getChildrenLayout().getMeasuredWidth();
		int pageHeight = cl.getChildrenLayout().getMeasuredHeight();
		if (Math.abs(scrollProgress) == 1.0f) {
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		int pageWidth = v.getMeasuredWidth();
		int pageHeight = v.getMeasuredHeight();
		v.setPivotX(0 - pageWidth / 3.0f);
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
		int pageWidth = v.getMeasuredWidth();
		int pageHeight = v.getMeasuredHeight();
		v.setPivotX(pageWidth / 2.0f);
		v.setPivotY(pageHeight / 2.0f);
		v.setFastScaleX(1.0f);
		v.setFastScaleY(1.0f);
		v.fastInvalidate();
	}

}
