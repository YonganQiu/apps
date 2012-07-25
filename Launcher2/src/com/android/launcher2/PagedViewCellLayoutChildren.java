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

import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher.R;

/**
 * An abstraction of the original CellLayout which supports laying out items
 * which span multiple cells into a grid-like layout.  Also supports dimming
 * to give a preview of its contents.
 */
public class PagedViewCellLayoutChildren extends ViewGroup {
    static final String TAG = "PagedViewCellLayout";

    private boolean mCenterContent;

    private int mCellWidth;
    private int mCellHeight;
    private int mWidthGap;
    private int mHeightGap;

    public PagedViewCellLayoutChildren(Context context) {
        super(context);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public void setGap(int widthGap, int heightGap) {
        mWidthGap = widthGap;
        mHeightGap = heightGap;
        requestLayout();
    }

    public void setCellDimensions(int width, int height) {
        mCellWidth = width;
        mCellHeight = height;
        requestLayout();
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            PagedViewCellLayout.LayoutParams lp =
                (PagedViewCellLayout.LayoutParams) child.getLayoutParams();
            lp.setup(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                    getPaddingLeft(),
                    getPaddingTop());

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width,
                    MeasureSpec.EXACTLY);
            int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height,
                    MeasureSpec.EXACTLY);

            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    // {added by zhong.chen 2012-7-12 for launcher apps sort begin
    private ValueAnimator mLayoutAnimation;
    private int[] mNewLefts;
    private int[] mNewRights;
    private int[] mNewTops;
    private int[] mNewBottoms;
    //private float[] mRotations;
    //private final float START_ROTATION = .9999f;
    //private final float FINAL_ROTATION = 360f;
    boolean mChildrenDoAnim = false;
    // }added by zhong.chen 2012-7-12 for launcher apps sort end

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // {modified by zhong.chen 2012-7-12 for launcher apps sort
        final int count = getChildCount();

        int offsetX = 0;
        int maxRowWidth = 0;
        if (mCenterContent && count > 0) {
            // determine the max width of all the rows and center accordingly
            int maxRowX = 0;
            int minRowX = Integer.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    PagedViewCellLayout.LayoutParams lp =
                        (PagedViewCellLayout.LayoutParams) child.getLayoutParams();
                    minRowX = Math.min(minRowX, lp.x);
                    maxRowX = Math.max(maxRowX, lp.x + lp.width);
                }
            }
            maxRowWidth = maxRowX - minRowX;
            offsetX = (getMeasuredWidth() - maxRowWidth) / 2;
        }
        // }modified by zhong.chen 2012-7-12 for launcher apps sort end
        
        // {modified by zhong.chen 2012-7-12 for launcher apps sort begin animtion
        boolean animEnable = false;
        mNewLefts = new int[count];
        mNewRights = new int[count];
        mNewTops = new int[count];
        mNewBottoms = new int[count];
        //mRotations = new float[count];
        
        int width = getWidth();
        int height = getHeight();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                PagedViewCellLayout.LayoutParams lp =
                    (PagedViewCellLayout.LayoutParams) child.getLayoutParams();

                int childLeft = offsetX + lp.x;
                int childTop = lp.y;
                
                // {modified by zhong.chen 2012-7-25 for launcher apps sort
                if(mChildrenDoAnim && child instanceof PagedViewIcon) {
                    ApplicationInfo appInfo = (ApplicationInfo) child.getTag();
                    prepareAnimData(i, offsetX, child, width, height, appInfo, lp);
                    animEnable = true;
                } else {
                    child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);
                }
                // }modified by zhong.chen 2012-7-25 for launcher apps sort end
            }
        }
        if(mChildrenDoAnim && animEnable) {
            doLayoutAnim();
        }
        // }modified by zhong.chen 2012-7-12 for launcher apps sort end animation
    }
    
    // {added by zhong.chen 2012-7-25 for launcher apps sort begin
    void prepareAnimData(int index, int offsetX,
            final View child, int width, int height,
            final ApplicationInfo appInfo,
            final PagedViewCellLayout.LayoutParams lp) {

        int childLeft = offsetX + lp.x;
        int childTop = lp.y;

        mNewLefts[index] = childLeft;
        mNewTops[index] = childTop;
        mNewRights[index] = childLeft + lp.width;
        mNewBottoms[index] = childTop + lp.height;
        if (appInfo.mOldLeft == 0 && appInfo.mOldTop == 0
                && appInfo.mOldRight == 0
                && appInfo.mOldBottom == 0) {
            if (mNewLefts[index] > width / 2) {
                appInfo.mOldLeft = width + mCellWidth;
                appInfo.mOldRight = appInfo.mOldLeft + mCellWidth;
            } else {
                appInfo.mOldLeft = -mCellWidth;
                appInfo.mOldRight = appInfo.mOldLeft + mCellWidth;
            }
            if (mNewTops[index] > height / 2) {
                appInfo.mOldTop = height + mCellHeight;
                appInfo.mOldBottom = appInfo.mOldTop + mCellHeight;
            } else {
                appInfo.mOldTop = -mCellHeight;
                appInfo.mOldBottom = appInfo.mOldTop + mCellHeight;
            }
        }
    }
    
    void doLayoutAnim() {
        final int count = getChildCount();
        if (null != mLayoutAnimation) {
            mLayoutAnimation.cancel();
            mLayoutAnimation = null;
        }

        final int outDuration = getResources()
                .getInteger(R.integer.config_appsCustomizeZoomOutTime);

        mLayoutAnimation = ValueAnimator.ofFloat(0f, 1f).setDuration(outDuration);
        mLayoutAnimation.setInterpolator(new Workspace.ZoomOutInterpolator());
        mLayoutAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    if (child instanceof PagedViewIcon
                            && child.getVisibility() != GONE
                            && child.getTag() instanceof ApplicationInfo) {
                        ApplicationInfo appInfo = (ApplicationInfo) child.getTag();
                        appInfo.mOldLeft = mNewLefts[i];
                        appInfo.mOldTop = mNewTops[i];
                        appInfo.mOldRight = mNewRights[i];
                        appInfo.mOldBottom = mNewBottoms[i];
                        child.layout(mNewLefts[i], mNewTops[i], mNewRights[i], mNewBottoms[i]);
                    }
                }
            }
        });
        mLayoutAnimation.addUpdateListener(new LauncherAnimatorUpdateListener() {
            public void onAnimationUpdate(float a, float b) {
                if (b == 0f) {
                    return;
                }
                invalidate();
                for (int i = 0; i < count; i++) {
                    final View child = getChildAt(i);
                    if (child instanceof PagedViewIcon
                            && child.getVisibility() != GONE
                            && child.getTag() instanceof ApplicationInfo) {
                        ApplicationInfo appInfo = (ApplicationInfo) child.getTag();
                        child.fastInvalidate();
                        int left = (int) (a * appInfo.mOldLeft + b * mNewLefts[i]);
                        int top = (int) (a * appInfo.mOldTop + b * mNewTops[i]);
                        int right = (int) (a * appInfo.mOldRight + b * mNewRights[i]);
                        int bottom = (int) (a * appInfo.mOldBottom + b * mNewBottoms[i]);

                        child.layout(left, top, right, bottom);
                    }
                }
            }
        });
        mLayoutAnimation.start();
    }
    // }added by zhong.chen 2012-7-25 for launcher apps sort end

    void destroyHardwareLayer() {
        setLayerType(LAYER_TYPE_NONE, null);
    }
    void createHardwareLayer() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void enableCenteredContent(boolean enabled) {
        mCenterContent = enabled;
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            // Update the drawing caches
            if (!view.isHardwareAccelerated()) {
                view.buildDrawingCache(true);
            }
        }
    }
}
