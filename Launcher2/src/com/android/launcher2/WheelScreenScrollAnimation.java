package com.android.launcher2;

import java.util.ArrayList;

import android.view.View;

import com.android.launcher2.CellLayout.LayoutParams;
import com.android.launcher2.Workspace.ScreenScrollAnimation;

public class WheelScreenScrollAnimation implements ScreenScrollAnimation {
	@Override
	public void screenScroll(float scrollProgress, View v) {
		CellLayout cell = (CellLayout) v;
		CellLayoutChildren childrenLayout = cell.getChildrenLayout();
		if (childrenLayout.getChildCount() <= 0) {
			return;
		}

		int pageWidth = childrenLayout.getMeasuredWidth();
		int pageHeight = childrenLayout.getMeasuredHeight();
		double radius = pageWidth * 0.4;
		double radians = -(2 * Math.PI / (childrenLayout.getChildCount()));
		double degree = 360 / (childrenLayout.getChildCount());
		float circleCenterX = pageWidth / 2;
		float circleCenterY = pageHeight / 2;
		float absScrollProgress = Math.abs(scrollProgress);

		// sort child list.
		ArrayList<View> childList = new ArrayList<View>();
		for (int y = 0; y < cell.getCountY(); y++) {
			for (int x = 0; x < cell.getCountX(); x++) {
				View child = childrenLayout.getChildAt(x, y);
				if (child != null && !childList.contains(child)) {
					childList.add(child);
				}
			}
		}

		for (int i = 0; i < childList.size(); i++) {
			View child = childList.get(i);
			CellLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();

			float toX = (float) (circleCenterX + radius * Math.cos(radians * i)
					- child.getWidth() / 2 - child.getLeft());
			float toY = (float) (circleCenterY + radius * Math.sin(radians * i)
					- child.getHeight() / 2 - child.getTop());
			float toRotation = (float) -(90 + degree * i);

			float toScaleX = 1;
			float toScaleY = 1;
			if (lp.cellHSpan > 1 || lp.cellVSpan > 1) {
				toScaleX = (float) cell.getCellWidth() / child.getWidth();
				toScaleY = (float) cell.getCellHeight() / child.getHeight();
			}

			if (absScrollProgress == 1.0f) {
				toX = toY = 0;
				toRotation = 0;
				toScaleX = 1;
				toScaleY = 1;
			} else if (absScrollProgress <= 0.5f) {
				toX = toX * absScrollProgress * 2;
				toY = toY * absScrollProgress * 2;
				toRotation = toRotation * absScrollProgress * 2;
				if (lp.cellHSpan > 1 || lp.cellVSpan > 1) {
					toScaleX = 1 + (toScaleX - 1) * absScrollProgress * 2;
					toScaleY = 1 + (toScaleY - 1) * absScrollProgress * 2;
				}
			}

			child.setPivotX(child.getWidth() / 2);
			child.setPivotY(child.getHeight() / 2);
			child.setScaleX(toScaleX);
			child.setScaleY(toScaleY);
			child.setTranslationX(toX);
			child.setTranslationY(toY);
			child.setRotation(toRotation);
		}

		childrenLayout.setPivotX(childrenLayout.getWidth() / 2);
		childrenLayout.setPivotY(childrenLayout.getHeight() / 2);
		if (absScrollProgress > 0.5f && absScrollProgress < 1.0f) {
			if (scrollProgress < 0) {
				childrenLayout.setRotation(90 * (absScrollProgress - 0.5f) * 2);
			} else {
				childrenLayout
						.setRotation(-90 * (absScrollProgress - 0.5f) * 2);
			}
		} else {
			childrenLayout.setRotation(0);
		}
	}

	@Override
	public void leftScreenOverScroll(float scrollProgress, View v) {
		CellLayout cell = (CellLayout) v;
		CellLayoutChildren childrenLayout = cell.getChildrenLayout();
		if (childrenLayout.getChildCount() <= 0) {
			return;
		} else {
			screenOverScroll(scrollProgress, v);
		}

	}

	@Override
	public void rightScreenOverScroll(float scrollProgress, View v) {
		CellLayoutChildren childrenLayout = ((CellLayout) v)
				.getChildrenLayout();
		if (childrenLayout.getChildCount() <= 0) {
			return;
		} else {
			screenOverScroll(scrollProgress, v);
		}

	}

	private void screenOverScroll(float scrollProgress, View v) {
		CellLayout cell = (CellLayout) v;
		CellLayoutChildren childrenLayout = cell.getChildrenLayout();

		int pageWidth = childrenLayout.getMeasuredWidth();
		int pageHeight = childrenLayout.getMeasuredHeight();
		double radius = pageWidth * 0.4;
		double radians = -(2 * Math.PI / (childrenLayout.getChildCount()));
		double degree = 360 / (childrenLayout.getChildCount());
		float circleCenterX = pageWidth / 2;
		float circleCenterY = pageHeight / 2;
		float absScrollProgress = Math.abs(scrollProgress);

		// sort child list.
		ArrayList<View> childList = new ArrayList<View>();
		for (int y = 0; y < cell.getCountY(); y++) {
			for (int x = 0; x < cell.getCountX(); x++) {
				View child = childrenLayout.getChildAt(x, y);
				if (child != null && !childList.contains(child)) {
					childList.add(child);
				}
			}
		}

		for (int i = 0; i < childList.size(); i++) {
			View child = childList.get(i);
			CellLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();

			float toX = (float) (circleCenterX + radius * Math.cos(radians * i)
					- child.getWidth() / 2 - child.getLeft());
			float toY = (float) (circleCenterY + radius * Math.sin(radians * i)
					- child.getHeight() / 2 - child.getTop());
			float toRotation = (float) -(90 + degree * i);

			float toScaleX = 1;
			float toScaleY = 1;
			if (lp.cellHSpan > 1 || lp.cellVSpan > 1) {
				toScaleX = (float) cell.getCellWidth() / child.getWidth();
				toScaleY = (float) cell.getCellHeight() / child.getHeight();
			}

			if (absScrollProgress <= 0.5f) {
				toX = toX * absScrollProgress * 2;
				toY = toY * absScrollProgress * 2;
				toRotation = toRotation * absScrollProgress * 2;
				if (lp.cellHSpan > 1 || lp.cellVSpan > 1) {
					toScaleX = 1 + (toScaleX - 1) * absScrollProgress * 2;
					toScaleY = 1 + (toScaleY - 1) * absScrollProgress * 2;
				}
			}

			child.setPivotX(child.getWidth() / 2);
			child.setPivotY(child.getHeight() / 2);
			child.setScaleX(toScaleX);
			child.setScaleY(toScaleY);
			child.setTranslationX(toX);
			child.setTranslationY(toY);
			child.setRotation(toRotation);
		}

		childrenLayout.setPivotX(childrenLayout.getWidth() / 2);
		childrenLayout.setPivotY(childrenLayout.getHeight() / 2);
		if (absScrollProgress > 0.5f) {
			if (scrollProgress < 0) {
				childrenLayout.setRotation(90 * (absScrollProgress - 0.5f) * 2);
			} else {
				childrenLayout
						.setRotation(-90 * (absScrollProgress - 0.5f) * 2);
			}
		} else {
			childrenLayout.setRotation(0);
		}
	}

	@Override
	public void resetAnimationData(View v) {
		CellLayout cell = (CellLayout) v;
		CellLayoutChildren childrenLayout = cell.getChildrenLayout();
		if (childrenLayout.getChildCount() <= 0) {
			return;
		}
		
		for (int i = 0; i < childrenLayout.getChildCount(); i++) {
			View child = childrenLayout.getChildAt(i);
			child.setPivotX(child.getWidth() / 2);
			child.setPivotY(child.getHeight() / 2);
			child.setScaleX(1);
			child.setScaleY(1);
			child.setTranslationX(0);
			child.setTranslationY(0);
			child.setRotation(0);
		}

		childrenLayout.setPivotX(childrenLayout.getWidth() / 2);
		childrenLayout.setPivotY(childrenLayout.getHeight() / 2);
		childrenLayout.setRotation(0);
	}

}
