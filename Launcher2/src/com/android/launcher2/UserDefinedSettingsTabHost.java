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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.android.launcher.R;

import java.util.ArrayList;

/**
 * added by zhong.chen 2012-6-28 for launcher user-defined
 */
public class UserDefinedSettingsTabHost extends TabHost implements LauncherTransitionable,
        TabHost.OnTabChangeListener {
    static final String LOG_TAG = "UserDefinedSettingsTabHost";

    private final LayoutInflater mLayoutInflater;
    private ViewGroup mTabs;
    private ViewGroup mTabsContainer;
    private LinearLayout mContent;

    private boolean mSuppressContentCallback = false;

    private UserDefinedSettingsPagedView mUserDefinedSettingsPane;

    private FrameLayout mAnimationBuffer;
    private AnimatorSet mTabChangeAnimSet;

    private boolean mInTransition;
    private boolean mResetAfterTransition;
    private Animator mLauncherTransition;

    interface TabType {
        static final String WALLPAPER_TAB_TAG = "WALLPAPER";
        static final String THEME_TAB_TAG = "THEME";
        static final String EFFECT_TAB_TAG = "EFFECT";
    }

    public UserDefinedSettingsTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
    }

    /**
     * Convenience methods to select specific tabs. We want to set the content
     * type immediately in these cases, but we note that we still call
     * setCurrentTabByTag() so that the tab view reflects the new content (but
     * doesn't do the animation and logic associated with changing tabs
     * manually).
     */
    private void setContentTypeImmediate(UserDefinedSettingsPagedView.ContentType type) {
        onTabChangedStart();
        onTabChangedEnd(type);
    }

    void selectWallpapersTab() {
        setContentTypeImmediate(UserDefinedSettingsPagedView.ContentType.Wallpaper);
        setCurrentTabByTag(TabType.WALLPAPER_TAB_TAG);
    }

    void selectThemesTab() {
        setContentTypeImmediate(UserDefinedSettingsPagedView.ContentType.Theme);
        setCurrentTabByTag(TabType.THEME_TAB_TAG);
    }

    void selectEffectsTab() {
        setContentTypeImmediate(UserDefinedSettingsPagedView.ContentType.Effect);
        setCurrentTabByTag(TabType.EFFECT_TAB_TAG);
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
        final UserDefinedSettingsPagedView appsCustomizePane = (UserDefinedSettingsPagedView)
                findViewById(R.id.apps_customize_pane_content);
        mTabs = tabs;
        mTabsContainer = tabsContainer;
        mUserDefinedSettingsPane = appsCustomizePane;
        mAnimationBuffer = (FrameLayout) findViewById(R.id.animation_buffer);
        mContent = (LinearLayout) findViewById(R.id.apps_customize_content);
        if (tabs == null || mUserDefinedSettingsPane == null)
            throw new Resources.NotFoundException();

        // Configure the tabs content factory to return the same paged view
        // (that we change the
        // content filter on)
        TabContentFactory contentFactory = new TabContentFactory() {
            public View createTabContent(String tag) {
                return appsCustomizePane;
            }
        };

        // Create the tabs
        TextView tabView;
        String label;
        label = mContext.getString(R.string.user_defined_wallpaper);
        tabView = (TextView) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        tabView.setText(label);
        tabView.setContentDescription(label);
        addTab(newTabSpec(TabType.WALLPAPER_TAB_TAG).setIndicator(tabView).setContent(
                contentFactory));

        label = mContext.getString(R.string.user_defined_theme);
        tabView = (TextView) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        tabView.setText(label);
        tabView.setContentDescription(label);
        addTab(newTabSpec(TabType.THEME_TAB_TAG).setIndicator(tabView).setContent(contentFactory));

        label = mContext.getString(R.string.user_defined_effect);
        tabView = (TextView) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        tabView.setText(label);
        tabView.setContentDescription(label);
        addTab(newTabSpec(TabType.EFFECT_TAB_TAG).setIndicator(tabView).setContent(contentFactory));

        setOnTabChangedListener(this);

        // Hide the tab bar until we measure
        mTabsContainer.setAlpha(0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean remeasureTabWidth = (mTabs.getLayoutParams().width <= 0);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Set the width of the tab list to the content width
        if (remeasureTabWidth) {
            int contentWidth = mUserDefinedSettingsPane.getPageContentWidth();
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
        // Intercept all touch events up to the bottom of the AppsCustomizePane
        // so they do not fall
        // through to the workspace and trigger showWorkspace()
        if (event.getY() < mUserDefinedSettingsPane.getBottom()) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void onTabChangedStart() {
        mUserDefinedSettingsPane.hideScrollingIndicator(false);
    }

    private void reloadCurrentPage() {
        if (!LauncherApplication.isScreenLarge()) {
            mUserDefinedSettingsPane.flashScrollingIndicator(true);
        }
        mUserDefinedSettingsPane.loadAssociatedPages(mUserDefinedSettingsPane.getCurrentPage());
        mUserDefinedSettingsPane.requestFocus();
    }

    private void onTabChangedEnd(UserDefinedSettingsPagedView.ContentType type) {
        mUserDefinedSettingsPane.setContentType(type);
    }

    @Override
    public void onTabChanged(String tabId) {
        final UserDefinedSettingsPagedView.ContentType type = getContentTypeForTabTag(tabId);
        if (mSuppressContentCallback) {
            mSuppressContentCallback = false;
            return;
        }

        final Resources res = getResources();
        final int duration = res.getInteger(R.integer.config_tabTransitionDuration);

        post(new Runnable() {
            @Override
            public void run() {
                if (mUserDefinedSettingsPane.getMeasuredWidth() <= 0 ||
                        mUserDefinedSettingsPane.getMeasuredHeight() <= 0) {
                    reloadCurrentPage();
                    return;
                }

                int[] visiblePageRange = new int[2];
                mUserDefinedSettingsPane.getVisiblePages(visiblePageRange);
                if (visiblePageRange[0] == -1 && visiblePageRange[1] == -1) {
                    // If we can't get the visible page ranges, then just skip
                    // the animation
                    reloadCurrentPage();
                    return;
                }
                ArrayList<View> visiblePages = new ArrayList<View>();
                for (int i = visiblePageRange[0]; i <= visiblePageRange[1]; i++) {
                    visiblePages.add(mUserDefinedSettingsPane.getPageAt(i));
                }

                mAnimationBuffer.scrollTo(mUserDefinedSettingsPane.getScrollX(), 0);

                for (int i = visiblePages.size() - 1; i >= 0; i--) {
                    View child = visiblePages.get(i);
                    if (child instanceof PagedViewCellLayout) {
                        ((PagedViewCellLayout) child).resetChildrenOnKeyListeners();
                    } else if (child instanceof PagedViewGridLayout) {
                        ((PagedViewGridLayout) child).resetChildrenOnKeyListeners();
                    }
                    PagedViewWidget.setDeletePreviewsWhenDetachedFromWindow(false);
                    mUserDefinedSettingsPane.removeView(child);
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
                ObjectAnimator inAnim = ObjectAnimator.ofFloat(mUserDefinedSettingsPane, "alpha",
                        1f);
                inAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        reloadCurrentPage();
                    }
                });
                if (mTabChangeAnimSet != null && mTabChangeAnimSet.isRunning()) {
                    mTabChangeAnimSet.cancel();
                }
                mTabChangeAnimSet = new AnimatorSet();
                mTabChangeAnimSet.playTogether(outAnim, inAnim);
                mTabChangeAnimSet.setDuration(duration);
                mTabChangeAnimSet.start();
            }
        });
    }

    public void setCurrentTabFromContent(UserDefinedSettingsPagedView.ContentType type) {
        mSuppressContentCallback = true;
        setCurrentTabByTag(getTabTagForContentType(type));
    }

    /**
     * Returns the content type for the specified tab tag.
     */
    public UserDefinedSettingsPagedView.ContentType getContentTypeForTabTag(String tag) {
        if (tag.equals(TabType.WALLPAPER_TAB_TAG)) {
            return UserDefinedSettingsPagedView.ContentType.Wallpaper;
        } else if (tag.equals(TabType.THEME_TAB_TAG)) {
            return UserDefinedSettingsPagedView.ContentType.Theme;
        } else {
            return UserDefinedSettingsPagedView.ContentType.Effect;
        }

    }

    /**
     * Returns the tab tag for a given content type.
     */
    public String getTabTagForContentType(UserDefinedSettingsPagedView.ContentType type) {
        if (type == UserDefinedSettingsPagedView.ContentType.Wallpaper) {
            return TabType.WALLPAPER_TAB_TAG;
        } else if (type == UserDefinedSettingsPagedView.ContentType.Theme) {
            return TabType.THEME_TAB_TAG;
        } else {
            return TabType.EFFECT_TAB_TAG;
        }

    }

    /**
     * Disable focus on anything under this view in the hierarchy if we are not
     * visible.
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
            mUserDefinedSettingsPane.reset();
        }
    }

    private void enableAndBuildHardwareLayer() {
        // isHardwareAccelerated() checks if we're attached to a window and if
        // that
        // window is HW accelerated-- we were sometimes not attached to a window
        // and buildLayer was throwing an IllegalStateException
        if (isHardwareAccelerated()) {
            // Turn on hardware layers for performance
            setLayerType(LAYER_TYPE_HARDWARE, null);

            // force building the layer, so you don't get a blip early in an
            // animation
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

        // if the content wasn't visible before, delay the launcher animation
        // until after a call
        // to layout -- this prevents a blip
        if (animated && mContent.getVisibility() == GONE) {
            mLauncherTransition = animation;
            delayLauncherTransitionUntilLayout = true;
        }
        mContent.setVisibility(VISIBLE);

        if (!toWorkspace) {
            // Make sure the current page is loaded (we start loading the side
            // pages after the
            // transition to prevent slowing down the animation)
            mUserDefinedSettingsPane.loadAssociatedPages(mUserDefinedSettingsPane.getCurrentPage(),
                    true);
        }
        if (animated && !delayLauncherTransitionUntilLayout) {
            enableAndBuildHardwareLayer();
        }

        if (!toWorkspace && !LauncherApplication.isScreenLarge()) {
            mUserDefinedSettingsPane.showScrollingIndicator(false);
        }
        if (mResetAfterTransition) {
            mUserDefinedSettingsPane.reset();
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
            // Dismiss the workspace cling and show the all apps cling (if not
            // already shown)
            l.dismissWorkspaceCling(null);
            mUserDefinedSettingsPane.showAllAppsCling();
            // Make sure adjacent pages are loaded (we wait until after the
            // transition to
            // prevent slowing down the animation)
            mUserDefinedSettingsPane.loadAssociatedPages(mUserDefinedSettingsPane.getCurrentPage());

            if (!LauncherApplication.isScreenLarge()) {
                mUserDefinedSettingsPane.hideScrollingIndicator(false);
            }
        }
    }

    public void onResume() {
        if (getVisibility() == VISIBLE) {
            mContent.setVisibility(VISIBLE);
            mUserDefinedSettingsPane.loadAssociatedPages(mUserDefinedSettingsPane.getCurrentPage(),
                    true);
            mUserDefinedSettingsPane.loadAssociatedPages(mUserDefinedSettingsPane.getCurrentPage());
        }
    }

    public void onTrimMemory() {
        mContent.setVisibility(GONE);
        // mAppsCustomizePane.clearAllWidgetPages();//FIXME
    }

    boolean isTransitioning() {
        return mInTransition;
    }

}
