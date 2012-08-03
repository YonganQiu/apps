package com.android.launcher2;

import android.view.View;

import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class ScreenScrollAnimationBounce implements ScreenScrollAnimation {

	@Override
	public void screenScroll(float scrollProgress, View v) {
		// TODO Auto-generated method stub
		CellLayout cl = (CellLayout) v;
		int pageWidth = cl.getChildrenLayout().getMeasuredWidth();
		int pageHeight = cl.getChildrenLayout().getMeasuredHeight();
		if (Math.abs(scrollProgress) == 1.0f) {
			v.setFastTranslationY(0);
			v.setFastTranslationX(0);
		} else {
			if (scrollProgress > 0) {
				v.setFastTranslationY(pageHeight * -scrollProgress);

			} else {
				v.setFastTranslationY(pageHeight * scrollProgress);
			}
		}

		v.fastInvalidate();
	}

	@Override
	public void leftScreenOverScroll(float scrollProgress, View v) {
		// TODO Auto-generated method stub

		CellLayout cl = (CellLayout) v;
		int pageWidth = cl.getChildrenLayout().getMeasuredWidth();
		int pageHeight = cl.getChildrenLayout().getMeasuredHeight();

		if (scrollProgress < -0.3) {
			v.setFastTranslationY(pageHeight * -0.3f);
			v.setFastTranslationX(pageWidth * 0.3f);
		} else {
			v.setFastTranslationY(pageHeight * scrollProgress);
			v.setFastTranslationX(pageWidth * -scrollProgress);
		}

		v.fastInvalidate();
	}

	@Override
	public void rightScreenOverScroll(float scrollProgress, View v) {
		// TODO Auto-generated method stub
		CellLayout cl = (CellLayout) v;
		int pageWidth = cl.getChildrenLayout().getMeasuredWidth();
		int pageHeight = cl.getChildrenLayout().getMeasuredHeight();

		if (scrollProgress > 0.3) {
			v.setFastTranslationY(pageHeight * -0.3f);
			v.setFastTranslationX(pageWidth * -0.3f);
		} else {
			v.setFastTranslationY(pageHeight * -scrollProgress);
			v.setFastTranslationX(pageWidth * -scrollProgress);
		}
		v.fastInvalidate();

	}

	@Override
	public void resetAnimationData(View v) {
		v.setFastTranslationX(0);
		v.setFastTranslationY(0);
		v.fastInvalidate();
	}

}
