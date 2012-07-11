package com.android.contacts.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.QuickContactBadge;
import android.view.View;

public class QuickContactBadgeWithAccount extends QuickContactBadge{

	private Drawable mAccountIcon;
	
	public QuickContactBadgeWithAccount(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

    public void setAccountDrawable( Drawable draw ){
    	mAccountIcon = draw;
    	invalidate();
    }
    
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mAccountIcon == null) {
			return;
		}
		mAccountIcon.setBounds(0, getHeight() - 36, 36, getHeight());
		if (getPaddingTop() == 0 && getPaddingLeft() == 0) {
			mAccountIcon.draw(canvas);
		} else {
			int saveCount = canvas.getSaveCount();
			canvas.save();
			canvas.translate(getPaddingLeft(), getPaddingTop());
			mAccountIcon.draw(canvas);
			canvas.restoreToCount(saveCount);
		}
		
	}
}
