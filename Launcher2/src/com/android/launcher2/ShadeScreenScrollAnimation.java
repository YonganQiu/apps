package com.android.launcher2;

import android.view.View;
import android.view.ViewConfiguration;

import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class ShadeScreenScrollAnimation implements ScreenScrollAnimation{
	private static float TRANSITION_PIVOT = 0.65f;
	private static float TRANSITION_MAX_ROTATION = 22;

	@Override
	public void screenScroll(float scrollProgress, View v) {
		// TODO Auto-generated method stub
		CellLayout cl = (CellLayout)v;
		int pageWidth = cl.getChildrenLayout().getMeasuredWidth();
		int pageHeight = cl.getChildrenLayout().getMeasuredHeight();
		float translationX = Math.min(0, scrollProgress) * cl.getMeasuredWidth();
		
		if (Math.abs(scrollProgress) == 1.0f) {
            v.setPivotY(pageHeight / 2.0f);
            v.setPivotX(pageWidth / 2.0f);
            v.setFastScaleX(1.0f);
            v.setFastScaleY(1.0f);
            v.setFastTranslationX(0);
            v.setVisibility(View.VISIBLE);
            v.setFastAlpha(1.0f);
		}else{
			if (scrollProgress > 0) {
				//left screen.
                v.setFastTranslationX(translationX);
                v.setFastScaleX(1.0f);
                v.setFastScaleY(1.0f);
                v.setFastAlpha(1.0f);

			}else{
				//right screen.
                v.setFastTranslationX(translationX);
                v.setFastScaleX(1+scrollProgress * 0.5f);
                v.setFastScaleY(1+scrollProgress * 0.5f);
                float alpha = 1 + scrollProgress;
                if (alpha < ViewConfiguration.ALPHA_THRESHOLD) {
                    v.setVisibility(View.INVISIBLE);
                }
               else {
            	   v.setFastAlpha(alpha);
               }
	        }
		}
		
		v.fastInvalidate();		
	}

	@Override
	public void leftScreenOverScroll(float scrollProgress, View v) {
		// TODO Auto-generated method stub
		
		CellLayout cl = (CellLayout)v;
		int pageWidth = cl.getChildrenLayout().getMeasuredWidth();
		int pageHeight = cl.getChildrenLayout().getMeasuredHeight();
        v.setFastTranslationX(0);
        v.setPivotX(TRANSITION_PIVOT * pageWidth);
        v.setPivotY(pageHeight / 2.0f);
        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
        v.setFastScaleX(1.0f);
        v.setFastScaleY(1.0f);
        v.setFastAlpha(1.0f);
        v.fastInvalidate();	
	}

	@Override
	public void rightScreenOverScroll(float scrollProgress, View v) {
		// TODO Auto-generated method stub

		CellLayout cl = (CellLayout) v;
		int pageWidth = cl.getChildrenLayout().getMeasuredWidth();
		int pageHeight = cl.getChildrenLayout().getMeasuredHeight();
		v.setFastTranslationX(0);
		v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
		v.setPivotY(pageHeight / 2.0f);
		v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
		v.setFastScaleX(1.0f);
		v.setFastScaleY(1.0f);
		v.setFastAlpha(1.0f);
		v.fastInvalidate();	
	}

}
