/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher2;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;

import com.android.launcher.R;


/**
 * An icon on a PagedView, specifically for items in the launcher's paged view (with compound
 * drawables on the top).
 */
public class PagedViewIcon extends TextView implements Checkable {
    private static final String TAG = "PagedViewIcon";

    // holographic outline
    private final Paint mPaint = new Paint();
    private Bitmap mCheckedOutline;
    private Bitmap mHolographicOutline;
    private Bitmap mIcon;

    private int mAlpha = 255;
    private int mHolographicAlpha;

    private boolean mIsChecked;
    private ObjectAnimator mCheckedAlphaAnimator;
    private float mCheckedAlpha = 1.0f;
    private int mCheckedFadeInDuration;
    private int mCheckedFadeOutDuration;

    HolographicPagedViewIcon mHolographicOutlineView;
    //private HolographicOutlineHelper mHolographicOutlineHelper;
    
  //{add by zhongheng.zheng at 2012.7.10 begin for variable of new install sign
    private static final boolean DEBUG = false;
    private Bitmap mNewIcon;
    private boolean mIsNew = false;
  //}add by zhongheng.zheng end

    public PagedViewIcon(Context context) {
        this(context, null);
      //{add by zhongheng.zheng at 2012.7.10 begin new install sign
        //initBgBitmap(context);
      //}add by zhongheng.zheng end
    }

    public PagedViewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
      //{add by zhongheng.zheng at 2012.7.10 begin new install sign
        //initBgBitmap(context);
      //}add by zhongheng.zheng end
    }

    public PagedViewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Set up fade in/out constants
        final Resources r = context.getResources();
        final int alpha = r.getInteger(R.integer.config_dragAppsCustomizeIconFadeAlpha);
        if (alpha > 0) {
            mCheckedAlpha = r.getInteger(R.integer.config_dragAppsCustomizeIconFadeAlpha) / 256.0f;
            mCheckedFadeInDuration =
                r.getInteger(R.integer.config_dragAppsCustomizeIconFadeInDuration);
            mCheckedFadeOutDuration =
                r.getInteger(R.integer.config_dragAppsCustomizeIconFadeOutDuration);
        }

        mHolographicOutlineView = new HolographicPagedViewIcon(context, this);
        
      //{add by zhongheng.zheng at 2012.7.10 begin new install sign
        //initBgBitmap(context);
      //}add by zhongheng.zheng end
    }

    protected HolographicPagedViewIcon getHolographicOutlineView() {
        return mHolographicOutlineView;
    }

    protected Bitmap getHolographicOutline() {
        return mHolographicOutline;
    }

    public void applyFromApplicationInfo(ApplicationInfo info, boolean scaleUp,
            HolographicOutlineHelper holoOutlineHelper) {
        // {modified by zhong.chen 2012-7-28 for launcher apps sort
        //mHolographicOutlineHelper = holoOutlineHelper;
        if(null == info) {
            setVisibility(View.INVISIBLE);
            return;
        }
        // }modified by zhong.chen 2012-7-28 for launcher apps sort end
        mIcon = info.iconBitmap;
        setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(mIcon), null, null);
        setText(info.title);
        setTag(info);
      //{add by zhongheng.zheng at 2012.7.10 begin new install sign
		if (DEBUG) {
			Log.d(TAG, "info.title" + info.title);
			Log.d(TAG, "info.isEnabledNew" + info.isEnabledNew);
			Log.d(TAG, "info.launchCount" + info.launchCount);
		}
		if (info.isEnabledNew && info.launchCount <= 0) {
			mIsNew = true;
		} else {
			mIsNew = false;
		}
		// {added by zhong.chen 2012-7-28 for launcher apps sort begin
		if(getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }
		// }added by zhong.chen 2012-7-28 for launcher apps sort end
      //}add by zhongheng.zheng end
    }
    
	// {add by jingjiang.yu at 2012.07.09 begin for screen scroll.
	public void applyFromScrollAnimInfo(ScrollAnimStyleInfo animInfo) {
		setText(animInfo.getTitleId(getContext()));
		setTag(animInfo);

		Resources resources = getResources();
		mIcon = Utilities.createIconBitmap(
				resources.getDrawable(animInfo.getIconId(getContext())), 0, 0,
				getContext());
		setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(
				mIcon), null, null);
	}
  //}add by jingjiang.yu end

    public void setHolographicOutline(Bitmap holoOutline) {
        mHolographicOutline = holoOutline;
        getHolographicOutlineView().invalidate();
    }

    @Override
    public void setAlpha(float alpha) {
        final float viewAlpha = HolographicOutlineHelper.viewAlphaInterpolator(alpha);
        final float holographicAlpha = HolographicOutlineHelper.highlightAlphaInterpolator(alpha);
        int newViewAlpha = (int) (viewAlpha * 255);
        int newHolographicAlpha = (int) (holographicAlpha * 255);
        if ((mAlpha != newViewAlpha) || (mHolographicAlpha != newHolographicAlpha)) {
            mAlpha = newViewAlpha;
            mHolographicAlpha = newHolographicAlpha;
            super.setAlpha(viewAlpha);
        }
    }

    public void invalidateCheckedImage() {
        if (mCheckedOutline != null) {
            mCheckedOutline.recycle();
            mCheckedOutline = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mAlpha > 0) {
            super.onDraw(canvas);
        }

        Bitmap overlay = null;

        // draw any blended overlays
        if (mCheckedOutline != null) {
            mPaint.setAlpha(255);
            overlay = mCheckedOutline;
        }

        if (overlay != null) {
            final int offset = getScrollX();
            final int compoundPaddingLeft = getCompoundPaddingLeft();
            final int compoundPaddingRight = getCompoundPaddingRight();
            int hspace = getWidth() - compoundPaddingRight - compoundPaddingLeft;
            canvas.drawBitmap(overlay,
                    offset + compoundPaddingLeft + (hspace - overlay.getWidth()) / 2,
                    mPaddingTop,
                    mPaint);
        }
        
      //{add by zhongheng.zheng at 2012.7.10 begin new install sign
        if(mIsNew){
            initBgBitmap(this.getContext());
            int hspace = (getWidth() - mIcon.getWidth()) / 2;
            //int vspace = (getHeight() - mIcon.getHeight()) / 2;
            //Log.d(TAG,"hspace:" + hspace + " ; vspace:" + vspace);
            canvas.drawBitmap(mNewIcon, mIcon.getWidth() + hspace - mNewIcon.getWidth(), 0, null);
        }
      //}add by zhongheng.zheng end
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    void setChecked(boolean checked, boolean animate) {
        if (mIsChecked != checked) {
            mIsChecked = checked;

            float alpha;
            int duration;
            if (mIsChecked) {
                alpha = mCheckedAlpha;
                duration = mCheckedFadeInDuration;
            } else {
                alpha = 1.0f;
                duration = mCheckedFadeOutDuration;
            }

            // Initialize the animator
            if (mCheckedAlphaAnimator != null) {
                mCheckedAlphaAnimator.cancel();
            }
            if (animate) {
                mCheckedAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", getAlpha(), alpha);
                mCheckedAlphaAnimator.setDuration(duration);
                mCheckedAlphaAnimator.start();
            } else {
                setAlpha(alpha);
            }

            invalidate();
        }
    }

    @Override
    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }
    
  //{add by zhongheng.zheng at 2012.7.10 begin new install sign
    private void initBgBitmap(Context context) {
//        mPressedBgPaint = new Paint();
//        mPressedBgPaint.setAlpha(100);
        mNewIcon = Utilities.getAppIconNewBitmap(context);
    }
  //{add by zhongheng.zheng end
}
