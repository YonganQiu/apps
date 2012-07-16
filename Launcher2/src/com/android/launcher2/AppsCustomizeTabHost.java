/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.launcher.R;

import java.util.ArrayList;

public class AppsCustomizeTabHost extends TabHost implements LauncherTransitionable,
        TabHost.OnTabChangeListener  {
    static final String LOG_TAG = "AppsCustomizeTabHost";

    private static final String APPS_TAB_TAG = "APPS";
    private static final String WIDGETS_TAB_TAG = "WIDGETS";

    private final LayoutInflater mLayoutInflater;
    private ViewGroup mTabs;
    private ViewGroup mTabsContainer;
    private AppsCustomizePagedView mAppsCustomizePane;
    private boolean mSuppressContentCallback = false;
    private FrameLayout mAnimationBuffer;
    private LinearLayout mContent;

    private boolean mInTransition;
    private boolean mResetAfterTransition;
    private Animator mLauncherTransition;
  //{add by jingjiang.yu at 2012.03.27 begin for Bug247512
    private AnimatorSet mTabChangeAnimSet;
  //}add by jingjiang.yu end

    public AppsCustomizeTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
    }

    /**
     * Convenience methods to select specific tabs.  We want to set the content type immediately
     * in these cases, but we note that we still call setCurrentTabByTag() so that the tab view
     * reflects the new content (but doesn't do the animation and logic associated with changing
     * tabs manually).
     */
    private void setContentTypeImmediate(AppsCustomizePagedView.ContentType type) {
        onTabChangedStart();
        onTabChangedEnd(type);
    }
    void selectAppsTab() {
        setContentTypeImmediate(AppsCustomizePagedView.ContentType.Applications);
        setCurrentTabByTag(APPS_TAB_TAG);
    }
    void selectWidgetsTab() {
        setContentTypeImmediate(AppsCustomizePagedView.ContentType.Widgets);
        setCurrentTabByTag(WIDGETS_TAB_TAG);
    }

    /**
     * Setup the tab host and create all necessary tabs.
     */
    @Override
    protected void onFinishInflate() {
        // Setup the tab host
        setup();

        final ViewGroup tabsContainer = (ViewGroup) findViewById(R.id.tabs_container);
        final TabWidget tabs = (TabWidget) findViewById(com.android.internal.R.id.tabs);
        final AppsCustomizePagedView appsCustomizePane = (AppsCustomizePagedView)
                findViewById(R.id.apps_customize_pane_content);
        mTabs = tabs;
        mTabsContainer = tabsContainer;
        mAppsCustomizePane = appsCustomizePane;
        mAnimationBuffer = (FrameLayout) findViewById(R.id.animation_buffer);
        mContent = (LinearLayout) findViewById(R.id.apps_customize_content);
        if (tabs == null || mAppsCustomizePane == null) throw new Resources.NotFoundException();

        // Configure the tabs content factory to return the same paged view (that we change the
        // content filter on)
        TabContentFactory contentFactory = new TabContentFactory() {
            public View createTabContent(String tag) {
                return appsCustomizePane;
            }
        };

        // Create the tabs
        TextView tabView;
        String label;
        // {modified by zhong.chen 2012-7-14 for launcher apps sort
        label = mContext.getString(R.string.all_apps_button_label);
        View tabAppsView =  mLayoutInflater.inflate(R.layout.tab_apps_indicator, tabs, false);
        mLetterSwitcher = (TextSwitcher) tabAppsView.findViewById(R.id.app_label_switcher);
        mLetterSwitcher.setFactory(factory);
      
        Animation in = AnimationUtils.loadAnimation(mContext,
              android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(mContext,
              android.R.anim.fade_out);
        mLetterSwitcher.setInAnimation(in);
        mLetterSwitcher.setOutAnimation(out);
      
        tabView = (TextView) tabAppsView.findViewById(R.id.apps_label);
        tabView.setText(label);
        tabView.setContentDescription(label);
        addTab(newTabSpec(APPS_TAB_TAG).setIndicator(tabAppsView).setContent(contentFactory));
        // }modified by zhong.chen 2012-7-14 for launcher apps sort end
        label = mContext.getString(R.string.widgets_tab_label);
        tabView = (TextView) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        tabView.setText(label);
        tabView.setContentDescription(label);
        addTab(newTabSpec(WIDGETS_TAB_TAG).setIndicator(tabView).setContent(contentFactory));
        setOnTabChangedListener(this);

        // Setup the key listener to jump between the last tab view and the market icon
        AppsCustomizeTabKeyEventListener keyListener = new AppsCustomizeTabKeyEventListener();
        View lastTab = tabs.getChildTabViewAt(tabs.getTabCount() - 1);
        lastTab.setOnKeyListener(keyListener);
        View shopButton = findViewById(R.id.market_button);
        shopButton.setOnKeyListener(keyListener);

        // Hide the tab bar until we measure
        mTabsContainer.setAlpha(0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean remeasureTabWidth = (mTabs.getLayoutParams().width <= 0);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Set the width of the tab list to the content width
        if (remeasureTabWidth) {
            int contentWidth = mAppsCustomizePane.getPageContentWidth();
            if (contentWidth > 0 && mTabs.getLayoutParams().width != contentWidth) {
                // Set the width and show the tab bar
                mTabs.getLayoutParams().width = contentWidth;
                post(new Runnable() {
                    public void run() {
                        mTabs.requestLayout();
                        mTabsContainer.setAlpha(1f);
                    }
                });
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Intercept all touch events up to the bottom of the AppsCustomizePane so they do not fall
        // through to the workspace and trigger showWorkspace()
        if (event.getY() < mAppsCustomizePane.getBottom()) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void onTabChangedStart() {
        mAppsCustomizePane.hideScrollingIndicator(false);
    }

    private void reloadCurrentPage() {
        if (!LauncherApplication.isScreenLarge()) {
            mAppsCustomizePane.flashScrollingIndicator(true);
        }
        mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage());
        mAppsCustomizePane.requestFocus();
    }

    private void onTabChangedEnd(AppsCustomizePagedView.ContentType type) {
        mAppsCustomizePane.setContentType(type);
    }

    @Override
    public void onTabChanged(String tabId) {
        final AppsCustomizePagedView.ContentType type = getContentTypeForTabTag(tabId);
        if (mSuppressContentCallback) {
            mSuppressContentCallback = false;
            return;
        }

        // Animate the changing of the tab content by fading pages in and out
        final Resources res = getResources();
        final int duration = res.getInteger(R.integer.config_tabTransitionDuration);

        // We post a runnable here because there is a delay while the first page is loading and
        // the feedback from having changed the tab almost feels better than having it stick
        post(new Runnable() {
            @Override
            public void run() {
                if (mAppsCustomizePane.getMeasuredWidth() <= 0 ||
                        mAppsCustomizePane.getMeasuredHeight() <= 0) {
                    reloadCurrentPage();
                    return;
                }

                // Take the visible pages and re-parent them temporarily to mAnimatorBuffer
                // and then cross fade to the new pages
                int[] visiblePageRange = new int[2];
                mAppsCustomizePane.getVisiblePages(visiblePageRange);
                if (visiblePageRange[0] == -1 && visiblePageRange[1] == -1) {
                    // If we can't get the visible page ranges, then just skip the animation
                    reloadCurrentPage();
                    return;
                }
                ArrayList<View> visiblePages = new ArrayList<View>();
                for (int i = visiblePageRange[0]; i <= visiblePageRange[1]; i++) {
                    visiblePages.add(mAppsCustomizePane.getPageAt(i));
                }

                // We want the pages to be rendered in exactly the same way as they were when
                // their parent was mAppsCustomizePane -- so set the scroll on mAnimationBuffer
                // to be exactly the same as mAppsCustomizePane, and below, set the left/top
                // parameters to be correct for each of the pages
                mAnimationBuffer.scrollTo(mAppsCustomizePane.getScrollX(), 0);

                // mAppsCustomizePane renders its children in reverse order, so
                // add the pages to mAnimationBuffer in reverse order to match that behavior
                for (int i = visiblePages.size() - 1; i >= 0; i--) {
                    View child = visiblePages.get(i);
                    if (child instanceof PagedViewCellLayout) {
                        ((PagedViewCellLayout) child).resetChildrenOnKeyListeners();
                    } else if (child instanceof PagedViewGridLayout) {
                        ((PagedViewGridLayout) child).resetChildrenOnKeyListeners();
                    }
                    PagedViewWidget.setDeletePreviewsWhenDetachedFromWindow(false);
                    mAppsCustomizePane.removeView(child);
                    PagedViewWidget.setDeletePreviewsWhenDetachedFromWindow(true);
                    mAnimationBuffer.setAlpha(1f);
                    mAnimationBuffer.setVisibility(View.VISIBLE);
                    LayoutParams p = new FrameLayout.LayoutParams(child.getWidth(),
                            child.getHeight());
                    p.setMargins((int) child.getLeft(), (int) child.getTop(), 0, 0);
                    mAnimationBuffer.addView(child, p);
                }

                // Toggle the new content
                onTabChangedStart();
                onTabChangedEnd(type);

                // Animate the transition
                ObjectAnimator outAnim = ObjectAnimator.ofFloat(mAnimationBuffer, "alpha", 0f);
                outAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimationBuffer.setVisibility(View.GONE);
                        mAnimationBuffer.removeAllViews();
                    }
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mAnimationBuffer.setVisibility(View.GONE);
                        mAnimationBuffer.removeAllViews();
                    }
                });
                ObjectAnimator inAnim = ObjectAnimator.ofFloat(mAppsCustomizePane, "alpha", 1f);
                inAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        reloadCurrentPage();
                    }
                });
				// {modify by jingjiang.yu at 2012.03.27 begin for Bug247512
				if (mTabChangeAnimSet != null && mTabChangeAnimSet.isRunning()) {
					mTabChangeAnimSet.cancel();
				}
				mTabChangeAnimSet = new AnimatorSet();
				mTabChangeAnimSet.playTogether(outAnim, inAnim);
				mTabChangeAnimSet.setDuration(duration);
				mTabChangeAnimSet.start();
				// }modify by jingjiang.yu end
            }
        });
    }

    public void setCurrentTabFromContent(AppsCustomizePagedView.ContentType type) {
        mSuppressContentCallback = true;
        setCurrentTabByTag(getTabTagForContentType(type));
    }

    /**
     * Returns the content type for the specified tab tag.
     */
    public AppsCustomizePagedView.ContentType getContentTypeForTabTag(String tag) {
        if (tag.equals(APPS_TAB_TAG)) {
            return AppsCustomizePagedView.ContentType.Applications;
        } else if (tag.equals(WIDGETS_TAB_TAG)) {
            return AppsCustomizePagedView.ContentType.Widgets;
        }
        return AppsCustomizePagedView.ContentType.Applications;
    }

    /**
     * Returns the tab tag for a given content type.
     */
    public String getTabTagForContentType(AppsCustomizePagedView.ContentType type) {
        if (type == AppsCustomizePagedView.ContentType.Applications) {
            return APPS_TAB_TAG;
        } else if (type == AppsCustomizePagedView.ContentType.Widgets) {
            return WIDGETS_TAB_TAG;
        }
        return APPS_TAB_TAG;
    }

    /**
     * Disable focus on anything under this view in the hierarchy if we are not visible.
     */
    @Override
    public int getDescendantFocusability() {
        if (getVisibility() != View.VISIBLE) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        }
        return super.getDescendantFocusability();
    }

    void reset() {
        if (mInTransition) {
            // Defer to after the transition to reset
            mResetAfterTransition = true;
        } else {
            // Reset immediately
            mAppsCustomizePane.reset();
        }
    }

    private void enableAndBuildHardwareLayer() {
        // isHardwareAccelerated() checks if we're attached to a window and if that
        // window is HW accelerated-- we were sometimes not attached to a window
        // and buildLayer was throwing an IllegalStateException
        if (isHardwareAccelerated()) {
            // Turn on hardware layers for performance
            setLayerType(LAYER_TYPE_HARDWARE, null);

            // force building the layer, so you don't get a blip early in an animation
            // when the layer is created layer
            buildLayer();
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mLauncherTransition != null) {
            enableAndBuildHardwareLayer();
            mLauncherTransition.start();
            mLauncherTransition = null;
        }
    }

    /* LauncherTransitionable overrides */
    @Override
    public boolean onLauncherTransitionStart(Launcher l, Animator animation, boolean toWorkspace) {
        mInTransition = true;
        boolean delayLauncherTransitionUntilLayout = false;
        boolean animated = (animation != null);
        mLauncherTransition = null;

        // if the content wasn't visible before, delay the launcher animation until after a call
        // to layout -- this prevents a blip
        if (animated && mContent.getVisibility() == GONE) {
            mLauncherTransition = animation;
            delayLauncherTransitionUntilLayout = true;
        }
        mContent.setVisibility(VISIBLE);

        if (!toWorkspace) {
            // Make sure the current page is loaded (we start loading the side pages after the
            // transition to prevent slowing down the animation)
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage(), true);
        }
        if (animated && !delayLauncherTransitionUntilLayout) {
            enableAndBuildHardwareLayer();
        }

        if (!toWorkspace && !LauncherApplication.isScreenLarge()) {
            mAppsCustomizePane.showScrollingIndicator(false);
        }
        if (mResetAfterTransition) {
            mAppsCustomizePane.reset();
            mResetAfterTransition = false;
        }
        return delayLauncherTransitionUntilLayout;
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, Animator animation, boolean toWorkspace) {
        mInTransition = false;
        if (animation != null) {
            setLayerType(LAYER_TYPE_NONE, null);
        }

        if (!toWorkspace) {
            // Dismiss the workspace cling and show the all apps cling (if not already shown)
            l.dismissWorkspaceCling(null);
            mAppsCustomizePane.showAllAppsCling();
            // Make sure adjacent pages are loaded (we wait until after the transition to
            // prevent slowing down the animation)
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage());

            if (!LauncherApplication.isScreenLarge()) {
                mAppsCustomizePane.hideScrollingIndicator(false);
            }
        }
    }

    public void onResume() {
        if (getVisibility() == VISIBLE) {
            mContent.setVisibility(VISIBLE);
            // We unload the widget previews when the UI is hidden, so need to reload pages
            // Load the current page synchronously, and the neighboring pages asynchronously
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage(), true);
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage());
            
        }
    }

    public void onTrimMemory() {
        mContent.setVisibility(GONE);
        // Clear the widget pages of all their subviews - this will trigger the widget previews
        // to delete their bitmaps
        mAppsCustomizePane.clearAllWidgetPages();
    }

    boolean isTransitioning() {
        return mInTransition;
    }
    
    // {added by zhong.chen 2012-7-12 for launcher apps sort begin
    
    void updateLetterIndex(int mSelectIndex, final int y, int count) {
        int charIndex = 63 + mSelectIndex;
        String letterIndexCharacter = "#";
        if((charIndex >= 'a' && charIndex <= 'z')
                || (charIndex >= 'A' && charIndex <= 'Z')) {
            letterIndexCharacter = Character.toString((char)(charIndex));
        }
        /*if(null != letterIndexCharacter) {
            mAppsLabelPrefix.concat(letterIndexCharacter);
        }*/
        
        showLetterView(mSelectIndex, letterIndexCharacter);
        //mAppTab.setText(mAppTabLabel + mLetterIndexCharacter);
        mAppsCustomizePane.highlightCurrentLetterIndex(mSelectIndex, count);
    }
    
    private TextSwitcher mLetterSwitcher;
    private void showLetterView(int mSelectIndex, String label) {
        if(mSelectIndex > 0) {
            mLetterSwitcher.setVisibility(View.VISIBLE);
            mLetterSwitcher.setText(label);
        } else {
            mLetterSwitcher.setVisibility(View.INVISIBLE);
            mLetterSwitcher.setText(" ");
        }
        
    }
    
    private ViewSwitcher.ViewFactory factory = new ViewSwitcher.ViewFactory() {
        @Override
        public View makeView() {
            TextView tv = new TextView(mContext);
            //tv.setGravity(Gravity.BOTTOM);
            //tv.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            tv.setTextSize(24);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            return tv;
        }
        
    };
    
    /*
    private Paint mEventTextPaint = new Paint();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if(mAppsCustomizePane.getComparatorOrder() == ComparatorIndex.LETTER_INDEX
                && mAppsCustomizePane.getLetterSelectIndexIndex() > 0
                && mAppsCustomizePane.isAllAppPage()) {
            canvas.save();
            int color = getResources().getColor(R.color.drag_view_multiply_color);
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int textSize = Math.min(width, height);
            mEventTextPaint.setColor(color);
            mEventTextPaint.setTextSize(textSize);
            mEventTextPaint.setAlpha(32);
            mEventTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mEventTextPaint.setAntiAlias(true);
            mEventTextPaint.setTextAlign(Paint.Align.CENTER);
            
            canvas.drawText(mLetterIndexCharacter, width / 2, height / 2 + textSize / 3, mEventTextPaint);
            canvas.restore();
        }
        
    }*/
    // }added by zhong.chen 2012-7-12 for launcher apps sort end
}
