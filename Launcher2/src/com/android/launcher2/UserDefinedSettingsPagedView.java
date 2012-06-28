package com.android.launcher2;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.android.launcher.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * added by zhong.chen 2012-6-28 for launcher user-defined
 *
 */
public class UserDefinedSettingsPagedView extends PagedView implements 
    View.OnClickListener, View.OnKeyListener{

    private Launcher mLauncher;
    
    private final LayoutInflater mLayoutInflater;
    
    private int mContentWidth;
    
    //private int mWidgetCountX, mWidgetCountY;
    private int mMaxAppCellCountX, mMaxAppCellCountY;
    
    private PagedViewCellLayout mWidgetSpacingLayout;
    
    private ArrayList<ApplicationInfo> mWallpapers;
    private ArrayList<ApplicationInfo> mThemes;
    private ArrayList<ApplicationInfo> mAnimStyles;
    
    private int mNumWallpaperPages;
    private int mNumThemePages;
    private int mNumAnimStylePages;
    
    // Save and Restore
    private int mSaveInstanceStateItemIndex = -1;

    // Relating to the scroll and overscroll effects
    Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
    private static float CAMERA_DISTANCE = 6500;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    //{modify by jingjiang.yu at 2012.05.02 begin
    //private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.4f);
    //}modify by jingjiang.yu end
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);

    // Previews & outlines
    
    public enum ContentType {
        Wallpaper,
        Theme,
        AnimStyle
    }
    
    public UserDefinedSettingsPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mLauncher = (Launcher) context;
        
        mWallpapers = new ArrayList<ApplicationInfo>(10);
        mThemes = new ArrayList<ApplicationInfo>(1);
        mAnimStyles = new ArrayList<ApplicationInfo>(1);
       
        // Save the default widget preview background
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0, 0);
        mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountX, -1);
        mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountY, -1);
        mMaxAppCellCountX = 4;
        mMaxAppCellCountY = 1;
        //mWidgetCountX = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 2);
        //mWidgetCountY = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 2);
        mCellCountX = 4;
        mCellCountY = 1;
        a.recycle();
        mWidgetSpacingLayout = new PagedViewCellLayout(getContext());

        // The padding on the non-matched dimension for the default widget preview icons
        // (top + bottom)
        mFadeInAdjacentScreens = false;
    }
    
    @Override
    protected void init() {
        super.init();
        mCenterPagesVertically = false;
    }
    
    @Override
    protected void onUnhandledTap(MotionEvent ev) {
        if (LauncherApplication.isScreenLarge()) {
            // Dismiss AppsCustomize if we tap
            mLauncher.showWorkspace(true);
        }
    }
    
    int getSaveInstanceStateIndex() {
        if (mSaveInstanceStateItemIndex == -1) {
            mSaveInstanceStateItemIndex = getMiddleComponentIndexOnCurrentPage();
        }
        return mSaveInstanceStateItemIndex;
    }
    
    private int getMiddleComponentIndexOnCurrentPage() {
        int i = -1;
        if (getPageCount() > 0) {
            int currentPage = getCurrentPage();
            if (currentPage < mNumWallpaperPages) {
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountY;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = (currentPage * numItemsPerPage) + (childCount / 2);
                }
            } else if(currentPage >= mNumWallpaperPages && currentPage < mNumThemePages + mNumWallpaperPages){
                int numWallpapers = mWallpapers.size();
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountX;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = numWallpapers +
                        ((currentPage - mNumWallpaperPages) * numItemsPerPage) + (childCount / 2);
                }
            } else {
                int numThemes = mThemes.size() + mWallpapers.size();
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountX;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = numThemes +
                        ((currentPage - mNumWallpaperPages - mNumThemePages) * numItemsPerPage) + (childCount / 2);
                }
            }
        }
        return i;
    }

    @Override
    public void syncPages() {
        removeAllViews();

        Context context = getContext();
        PagedViewCellLayout layout;
        for (int j = 0; j < mNumWallpaperPages; ++j) {
            layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
        
        for (int i = 0; i < mNumThemePages; ++i) {
            layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
        
        for (int i = 0; i < mNumAnimStylePages; ++i) {
            layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
        

    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
        if(page < mNumWallpaperPages) {
            syncWallpaperPageItems(page, immediate);
        } else if (page >= mNumWallpaperPages && page < mNumThemePages + mNumWallpaperPages) {
            syncThemePageItems(page - mNumWallpaperPages, immediate);
        } else if(page >= mNumThemePages + mNumWallpaperPages){
            syncAnimStylePageItems(page - mNumWallpaperPages - mNumThemePages, immediate);
        }else {
            Log.e("zh.cn", "2.............................." + page);
        }

    }
    
    private void syncWallpaperPageItems(int page, boolean immediate) {
        //TODO
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mWallpapers.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);

        layout.removeAllViewsOnPage();
        ArrayList<Object> items = new ArrayList<Object>();
        ArrayList<Bitmap> images = new ArrayList<Bitmap>();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mWallpapers.get(i);
            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
                    R.layout.apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, null);
            icon.setOnClickListener(this);
            //icon.setOnLongClickListener(this);
            //icon.setOnTouchListener(this);
            //icon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i, new PagedViewCellLayout.LayoutParams(x,y, 1,1));

            items.add(info);
            images.add(info.iconBitmap);
        }

        layout.createHardwareLayers();
    }
    
    private void syncThemePageItems(int page, boolean immediate) {
        //TODO
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mThemes.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page + mNumWallpaperPages);

        layout.removeAllViewsOnPage();
        ArrayList<Object> items = new ArrayList<Object>();
        ArrayList<Bitmap> images = new ArrayList<Bitmap>();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mThemes.get(i);
            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
                    R.layout.apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, null);
            icon.setOnClickListener(this);
            //icon.setOnLongClickListener(this);
            //icon.setOnTouchListener(this);
            //icon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i, new PagedViewCellLayout.LayoutParams(x,y, 1,1));

            items.add(info);
            images.add(info.iconBitmap);
        }

        layout.createHardwareLayers();
    }
    
    private void syncAnimStylePageItems(int page, boolean immediate) {
        //TODO
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mAnimStyles.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page + mNumWallpaperPages + mNumThemePages);

        layout.removeAllViewsOnPage();
        ArrayList<Object> items = new ArrayList<Object>();
        ArrayList<Bitmap> images = new ArrayList<Bitmap>();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mAnimStyles.get(i);
            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
                    R.layout.apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, null);
            icon.setOnClickListener(this);
            //icon.setOnLongClickListener(this);
            //icon.setOnTouchListener(this);
            //icon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i, new PagedViewCellLayout.LayoutParams(x,y, 1,1));

            items.add(info);
            images.add(info.iconBitmap);
        }

        layout.createHardwareLayers();
    }
    
    private void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(mCellCountX, mCellCountY);
        layout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.  That said, we already know the
        // expected page width, so we can actually optimize by hiding all the TextView-based
        // children that are expensive to measure, and let that happen naturally later.
        setVisibilityOnChildren(layout, View.GONE);
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
        setVisibilityOnChildren(layout, View.VISIBLE);
    }
    
    private void setVisibilityOnChildren(ViewGroup layout, int visibility) {
        int childCount = layout.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            layout.getChildAt(i).setVisibility(visibility);
        }
    }
    
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!isDataReady()) {
            if (checkDataReady()) {
                setDataIsReady();
                setMeasuredDimension(width, height);
                onDataReady(width, height);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    
    public void onPackagesUpdated() {
        // TODO: this isn't ideal, but we actually need to delay here. This call is triggered
        // by a broadcast receiver, and in order for it to work correctly, we need to know that
        // the AppWidgetService has already received and processed the same broadcast. Since there
        // is no guarantee about ordering of broadcast receipt, we just delay here. Ideally,
        // we should have a more precise way of ensuring the AppWidgetService is up to date.
        postDelayed(new Runnable() {
           public void run() {
               updatePackages();
           }
        }, 500);
    }
    
    public void updatePackages() {
        /*boolean wasEmpty = mWidgets.isEmpty();
        mWidgets.clear();
        List<AppWidgetProviderInfo> widgets =
            AppWidgetManager.getInstance(mLauncher).getInstalledProviders();
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = mPackageManager.queryIntentActivities(shortcutsIntent, 0);
        for (AppWidgetProviderInfo widget : widgets) {
            if (widget.minWidth > 0 && widget.minHeight > 0) {
                mWidgets.add(widget);
            } else {
                Log.e(LOG_TAG, "Widget " + widget.provider + " has invalid dimensions (" +
                        widget.minWidth + ", " + widget.minHeight + ")");
            }
        }
        mWidgets.addAll(shortcuts);
        Collections.sort(mWidgets,
                new LauncherModel.WidgetAndShortcutNameComparator(mPackageManager));
        updatePageCounts();

        if (wasEmpty) {
            if (testDataReady()) requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }*/
    }
    
    protected void onDataReady(int width, int height) {
        // Note that we transpose the counts in portrait so that we get a similar layout
        boolean isLandscape = getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
        int maxCellCountX = Integer.MAX_VALUE;
        int maxCellCountY = Integer.MAX_VALUE;
        if (LauncherApplication.isScreenLarge()) {
            maxCellCountX = (isLandscape ? LauncherModel.getCellCountX() :
                LauncherModel.getCellCountY());
            maxCellCountY = (isLandscape ? LauncherModel.getCellCountY() :
                LauncherModel.getCellCountX());
        }
        if (mMaxAppCellCountX > -1) {
            maxCellCountX = Math.min(maxCellCountX, mMaxAppCellCountX);
        }
        if (mMaxAppCellCountY > -1) {
            maxCellCountY = Math.min(maxCellCountY, mMaxAppCellCountY);
        }

        // Now that the data is ready, we can calculate the content width, the number of cells to
        // use for each page
        mWidgetSpacingLayout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        mWidgetSpacingLayout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);
        mWidgetSpacingLayout.calculateCellCount(width, height, maxCellCountX, maxCellCountY);
        mCellCountX = mWidgetSpacingLayout.getCellCountX();
        mCellCountY = mWidgetSpacingLayout.getCellCountY();
        //mCellCountX = 4;
        //mCellCountY = 1;
        updatePageCounts();

        // Force a measure to update recalculate the gaps
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        mWidgetSpacingLayout.measure(widthSpec, heightSpec);
        mContentWidth = mWidgetSpacingLayout.getContentWidth();

        UserDefinedSettingsTabHost host = (UserDefinedSettingsTabHost) getTabHost();
        final boolean hostIsTransitioning = host.isTransitioning();

        // Restore the page
        int page = getPageForComponent(mSaveInstanceStateItemIndex);
        invalidatePageData(Math.max(0, page), hostIsTransitioning);

        // Show All Apps cling if we are finished transitioning, otherwise, we will try again when
        // the transition completes in UserDefinedSettingsTabHost (otherwise the wrong offsets will be
        // returned while animating)
        if (!hostIsTransitioning) {
            post(new Runnable() {
                @Override
                public void run() {
                    showAllAppsCling();
                }
            });
        }
    }
    
    private void updatePageCounts() {
        mNumWallpaperPages = (int) Math.ceil((float) mWallpapers.size() / 
                (mCellCountX * mCellCountY));
        mNumThemePages = (int) Math.ceil((float) mThemes.size() / 
                (mCellCountX * mCellCountY));
        mNumAnimStylePages = (int) Math.ceil((float) mAnimStyles.size() / 
                (mCellCountX * mCellCountY));
    }
    
    public boolean checkDataReady() {
        return null != mWallpapers && !mWallpapers.isEmpty() 
                && null != mThemes && !mThemes.isEmpty()
                        && null != mAnimStyles && !mAnimStyles.isEmpty();
    }
    
    void restorePageForIndex(int index) {
        if (index < 0) return;
        mSaveInstanceStateItemIndex = index;
    }
    
    public void reset() {
        UserDefinedSettingsTabHost tabHost = getTabHost();
        String tag = tabHost.getCurrentTabTag();
        if (tag != null) {
            if (!tag.equals(tabHost.getTabTagForContentType(ContentType.Wallpaper))) {
                tabHost.setCurrentTabFromContent(ContentType.Wallpaper);
            }
        }
        if (mCurrentPage != 0) {
            invalidatePageData(0);
        }
    }
    
    public void initData() {
        setWallpapers();
        
        setThemes();
        
        setAnimStyles();
    }
    
    public void setWallpapers() {
        loadWallpapersFromApps();
        int ids[] = new int[]{R.drawable.wallpaper_4, R.drawable.wallpaper_7};
        int ids_small[] = new int[]{R.drawable.wallpaper_4_small, R.drawable.wallpaper_7_small};
        String names[] = new String[]{"Classical", "Reaationary"};
        ApplicationInfo appInfo;
        int i = 0;
        for (int wallpaperId : ids) 
        {
            appInfo = new ApplicationInfo();
            appInfo.title = names[i];
            appInfo.id = wallpaperId;
            appInfo.componentName = new ComponentName("Wallpaper", "Wallpaper");
            appInfo.iconBitmap = createIconBitmap(ids_small[i], mContext);
            i++;
            mWallpapers.add(appInfo);
        }
        
        Collections.sort(mWallpapers, LauncherModel.APP_NAME_COMPARATOR);
        
        updatePageCounts();

        // The next layout pass will trigger data-ready if both widgets and apps are set, so 
        // request a layout to do this test and invalidate the page data when ready.
        if (checkDataReady()) requestLayout();
    }
    
    private void loadWallpapersFromApps() {
        PackageManager manager = mLauncher.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_SET_WALLPAPER, null);
        mainIntent.addCategory(Intent.CATEGORY_DEFAULT);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) {
            final int count = apps.size();

            if (mWallpapers == null) {
                mWallpapers = new ArrayList<ApplicationInfo>(count);
            }
            mWallpapers.clear();

            for (int i = 0; i < count; i++) {
                ApplicationInfo application = new ApplicationInfo();
                ResolveInfo info = apps.get(i);

                application.title = info.loadLabel(manager);
                application.setActivity(new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                application.iconBitmap = createIconBitmap(info.activityInfo.loadIcon(manager), mContext);//info.activityInfo.loadIcon(manager);

                mWallpapers.add(application);
            }
        }
    }
    
    public void setThemes() {
        int ids[] = new int[] {
                R.drawable.theme_1, R.drawable.theme_2, R.drawable.theme_3
        };
        String names[] = new String[] {
                "Default", "Classical", "Reaationary"
        };
        ApplicationInfo appInfo;
        int i = 0;
        for (int id : ids)
        {
            appInfo = new ApplicationInfo();
            appInfo.title = names[i];
            i++;
            appInfo.id = id;
            appInfo.componentName = new ComponentName("Theme", "Theme");
            appInfo.iconBitmap = createIconBitmap(id, mContext);
            mThemes.add(appInfo);
        }
        
        Collections.sort(mThemes, LauncherModel.APP_NAME_COMPARATOR);
        
        updatePageCounts();

        // The next layout pass will trigger data-ready if both widgets and apps are set, so 
        // request a layout to do this test and invalidate the page data when ready.
        if (checkDataReady()) requestLayout();
    }
    
    public void setAnimStyles() {
        int ids[] = new int[] {
                R.drawable.effect_ball, R.drawable.effect_cylinder, 
                R.drawable.effect_moren, R.drawable.effect_roll,
                R.drawable.effect_wallpicroll, R.drawable.effect_wave
        };
        String names[] = new String[] {
                "Ball", "Cylinder", "Moren",
                "Roll", "Wallpicroll", "Wave"
        };
        ApplicationInfo appInfo;
        int i = 0;
        for (int id : ids)
        {
            appInfo = new ApplicationInfo();
            appInfo.title = names[i];
            i++;
            appInfo.id = id;
            appInfo.componentName = new ComponentName("Effect", "Effect");
            appInfo.iconBitmap = createIconBitmap(id, mContext);
            mAnimStyles.add(appInfo);
        }
        
        Collections.sort(mAnimStyles, LauncherModel.APP_NAME_COMPARATOR);
        
        updatePageCounts();

        // The next layout pass will trigger data-ready if both widgets and apps are set, so 
        // request a layout to do this test and invalidate the page data when ready.
        if (checkDataReady()) requestLayout();
    }
    
    Bitmap createIconBitmap(int resId, Context context) {
        Resources resources = getResources();
        return Utilities.createIconBitmap(resources.getDrawable(resId), context);
    }
    
    Bitmap createIconBitmap(Drawable icon, Context context) {
        return Utilities.createIconBitmap(icon, context);
    }
    
    private UserDefinedSettingsTabHost getTabHost() {
        return (UserDefinedSettingsTabHost) mLauncher.findViewById(R.id.user_defined_settings);
    }
    
    int getPageForComponent(int index) {
        if (index < 0) return 0;

        int numItemsPerPage = mCellCountX * mCellCountY;
        if (index < mWallpapers.size()) {
            return (index / numItemsPerPage);
        } else if(index >= mWallpapers.size() && index < mThemes.size() + mWallpapers.size()){
            return mNumWallpaperPages + ((index - mWallpapers.size()) / numItemsPerPage);
        } else {
            return mNumWallpaperPages + mNumThemePages 
                    + ((index - mWallpapers.size() - mThemes.size()) / numItemsPerPage);
        }
        
    }
    
    void showAllAppsCling() {
    }
    
    private class WallpaperInfo extends ApplicationInfo {
        //TODO
    }
    
    private class ThemeInfo extends ApplicationInfo {
      //TODO
    }


    private class AnimStyleInfo extends ApplicationInfo {
      //TODO
    }
    
    private boolean mBack = false;
    @Override
    public void onClick(View v) {

        if (!mLauncher.isUserDefinedOpen())
            return;

        if (v instanceof PagedViewIcon) {
            // Animate some feedback to the click
            final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();
            animateClickFeedback(v, new Runnable() {
                @Override
                public void run() {
                    if (null != appInfo.intent) {
                        mLauncher.startActivitySafely(appInfo.intent, appInfo);
                    } else {
                        try {
                            ComponentName cn = appInfo.componentName;
                            if (null != cn && "Wallpaper".equals(cn.getClassName())) {
                                WallpaperManager wpm = (WallpaperManager) mContext
                                        .getSystemService(Context.WALLPAPER_SERVICE);
                                wpm.setResource((int) appInfo.id);
                            } else {
                                Workspace w = mLauncher.getWorkspace();
                                int pages = w.getPageCount();
                                int cPage = w.getCurrentPage();
                                if (!mBack) {
                                    cPage++;
                                } else {
                                    cPage--;
                                }
                                if (cPage == pages) {
                                    mBack = true;
                                }
                                if (cPage < 0) {
                                    mBack = false;
                                }
                                cPage = Math.min(cPage, pages - 1);
                                cPage = Math.max(cPage, 0);

                                w.snapToPage(cPage);
                            }
                        } catch (IOException e) {
                        }
                    }

                }
            });
        }

    }
    
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeKeyEvent(v,  keyCode, event);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAllTasks();
    }
    
    public void clearAllWidgetPages() {
        cancelAllTasks();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getPageAt(i);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            }
        }
    }
    
    private void cancelAllTasks() {
        // Clean up all the async tasks
        /*Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            task.cancel(false);
            iter.remove();
        }*/
    }
    
    public void setContentType(ContentType type) {
        /*
         if (type == ContentType.Widgets) {
            invalidatePageData(mNumAppsPages, true);
        } else if (type == ContentType.Applications) {
            invalidatePageData(0, true);
        }
         */
        if (type == ContentType.Wallpaper) {
            invalidatePageData(0, true);
        } else if (type == ContentType.Theme) {
            invalidatePageData(mNumWallpaperPages, true);
        } else {
            invalidatePageData(mNumWallpaperPages + mNumThemePages, true);
        }
    }
    
    protected void snapToPage(int whichPage, int delta, int duration) {
        super.snapToPage(whichPage, delta, duration);
        updateCurrentTab(whichPage);

        // Update the thread priorities given the direction lookahead
        /*Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int pageIndex = task.page + mNumAppsPages;
            if ((mNextPage > mCurrentPage && pageIndex >= mCurrentPage) ||
                (mNextPage < mCurrentPage && pageIndex <= mCurrentPage)) {
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            } else {
                task.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            }
        }*/
    }
    
    /*private int getThreadPriorityForPage(int page) {
        // TODO-APPS_CUSTOMIZE: detect number of cores and set thread priorities accordingly below
        int pageDiff = getWidgetPageLoadPriority(page);
        if (pageDiff <= 0) {
            return Process.THREAD_PRIORITY_LESS_FAVORABLE;
        } else if (pageDiff <= 1) {
            return Process.THREAD_PRIORITY_LOWEST;
        } else {
            return Process.THREAD_PRIORITY_LOWEST;
        }
    }*/
    
    /*private int getWidgetPageLoadPriority(int page) {
        // If we are snapping to another page, use that index as the target page index
        int toPage = mCurrentPage;
        if (mNextPage > -1) {
            toPage = mNextPage;
        }

        // We use the distance from the target page as an initial guess of priority, but if there
        // are no pages of higher priority than the page specified, then bump up the priority of
        // the specified page.
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        int minPageDiff = Integer.MAX_VALUE;
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            minPageDiff = Math.abs(task.page + mNumAppsPages - toPage);
        }

        int rawPageDiff = Math.abs(page - toPage);
        return rawPageDiff - Math.min(rawPageDiff, minPageDiff);
    }*/
    
    private void updateCurrentTab(int currentPage) {
        UserDefinedSettingsTabHost tabHost = getTabHost();
        if (tabHost != null) {
            String tag = tabHost.getCurrentTabTag();
            if (tag != null) {
                 if (currentPage < mNumWallpaperPages &&
                        !tag.equals(tabHost.getTabTagForContentType(ContentType.Wallpaper))) {
                    tabHost.setCurrentTabFromContent(ContentType.Wallpaper);
                } else if (currentPage < mNumThemePages + mNumWallpaperPages
                        && currentPage >= mNumWallpaperPages 
                        && !tag.equals(tabHost.getTabTagForContentType(ContentType.Theme))) {
                    tabHost.setCurrentTabFromContent(ContentType.Theme);
                } 
                else if(currentPage < mNumAnimStylePages + mNumThemePages + mNumWallpaperPages
                        && currentPage >= mNumThemePages + mNumWallpaperPages
                        && !tag.equals(tabHost.getTabTagForContentType(ContentType.AnimStyle))){
                    tabHost.setCurrentTabFromContent(ContentType.AnimStyle);
                } else {
                    Log.e("zh.cn", "...............currentPage: " + currentPage + " ,tag: " + tag);
                }
            }
        }
    }
    
    View getPageAt(int index) {
        return getChildAt(getChildCount() - index - 1);
    }
    
    @Override
    protected int indexToPage(int index) {
        return getChildCount() - index - 1;
    }
    
    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        for (int i = 0; i < getChildCount(); i++) {
            View v = getPageAt(i);
            if (v != null) {
                float scrollProgress = getScrollProgress(screenCenter, v, i);

                float interpolatedProgress =
                        mZInterpolator.getInterpolation(Math.abs(Math.min(scrollProgress, 0)));
                float scale = (1 - interpolatedProgress) +
                        interpolatedProgress * TRANSITION_SCALE_FACTOR;
                float translationX = Math.min(0, scrollProgress) * v.getMeasuredWidth();

                float alpha;

                if (!LauncherApplication.isScreenLarge() || scrollProgress < 0) {
                    alpha = scrollProgress < 0 ? mAlphaInterpolator.getInterpolation(
                        1 - Math.abs(scrollProgress)) : 1.0f;
                } else {
                    // On large screens we need to fade the page as it nears its leftmost position
                    alpha = mLeftScreenAlphaInterpolator.getInterpolation(1 - scrollProgress);
                }

                v.setCameraDistance(mDensity * CAMERA_DISTANCE);
                int pageWidth = v.getMeasuredWidth();
                int pageHeight = v.getMeasuredHeight();

                if (PERFORM_OVERSCROLL_ROTATION) {
                    if (i == 0 && scrollProgress < 0) {
                        // Overscroll to the left
                        v.setPivotX(TRANSITION_PIVOT * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the first page, we don't want the page to have any lateral motion
                        translationX = 0;
                    } else if (i == getChildCount() - 1 && scrollProgress > 0) {
                        // Overscroll to the right
                        v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the last page, we don't want the page to have any lateral motion.
                        translationX = 0;
                    } else {
                        v.setPivotY(pageHeight / 2.0f);
                        v.setPivotX(pageWidth / 2.0f);
                        v.setRotationY(0f);
                    }
                }

                v.setTranslationX(translationX);
                v.setScaleX(scale);
                v.setScaleY(scale);
                v.setAlpha(alpha);

                // If the view has 0 alpha, we set it to be invisible so as to prevent
                // it from accepting touches
                if (alpha < ViewConfiguration.ALPHA_THRESHOLD) {
                    v.setVisibility(INVISIBLE);
                } else if (v.getVisibility() != VISIBLE) {
                    v.setVisibility(VISIBLE);
                }
            }
        }
    }

    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    /**
     * Used by the parent to get the content width to set the tab bar to
     * @return
     */
    public int getPageContentWidth() {
        return mContentWidth;
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();

        // We reset the save index when we change pages so that it will be recalculated on next
        // rotation
        mSaveInstanceStateItemIndex = -1;
    }
    
    public void setup(Launcher launcher) {
        mLauncher = launcher;
    }
    
    final static int sLookBehindPageCount = 2;
    final static int sLookAheadPageCount = 2;
    protected int getAssociatedLowerPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMinIndex = Math.max(Math.min(page - sLookBehindPageCount, count - windowSize), 0);
        return windowMinIndex;
    }
    protected int getAssociatedUpperPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMaxIndex = Math.min(Math.max(page + sLookAheadPageCount, windowSize - 1),
                count - 1);
        return windowMaxIndex;
    }

    @Override
    protected String getCurrentPageDescription() {
        /*
         int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int stringId = R.string.default_scroll_format;
        int count = 0;
        
        if (page < mNumAppsPages) {
            stringId = R.string.apps_customize_apps_scroll_format;
            count = mNumAppsPages;
        } else {
            page -= mNumAppsPages;
            stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWidgetPages;
        }

        return String.format(mContext.getString(stringId), page + 1, count);
         */
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int stringId = R.string.default_scroll_format;
        int count = 0;
        
        if (page < mNumWallpaperPages) {
            stringId = R.string.apps_customize_apps_scroll_format;
            count = mNumWallpaperPages;
        } else if(page >= mNumWallpaperPages && page < mNumThemePages + mNumWallpaperPages){
            page -= mNumWallpaperPages;
            stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWallpaperPages + mNumThemePages;
        } else {
            page -= mNumWallpaperPages;
            page -= mNumThemePages;
            stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWallpaperPages + mNumThemePages + mNumAnimStylePages;
        }

        return String.format(mContext.getString(stringId), page + 1, count);
    }

}
