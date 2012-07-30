
package com.android.launcher2;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.android.launcher.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * added by zhong.chen 2012-6-28 for launcher user-defined
 */
public class UserDefinedSettingsPagedView extends PagedView implements
        View.OnClickListener, View.OnKeyListener {

    private Launcher mLauncher;

    private final LayoutInflater mLayoutInflater;

    private int mContentWidth;

    // private int mWidgetCountX, mWidgetCountY;
    private int mMaxAppCellCountX, mMaxAppCellCountY;

    private PagedViewCellLayout mWidgetSpacingLayout;

    private ArrayList<ApplicationInfo> mWallpapers;
    private ArrayList<ApplicationInfo> mThemes;
    private ArrayList<ScrollAnimStyleInfo> mEffects;

    private int mNumWallpaperPages;
    private int mNumThemePages;
    private int mNumEffectPages;

    // Save and Restore
    private int mSaveInstanceStateItemIndex = -1;

    // Relating to the scroll and overscroll effects
    Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
    private static float CAMERA_DISTANCE = 6500;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    // private AccelerateInterpolator mAlphaInterpolator = new
    // AccelerateInterpolator(0.4f);
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);

    // Previews & outlines

    public enum ContentType {
        Wallpaper,
        Theme,
        Effect
    }

    public UserDefinedSettingsPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mLauncher = (Launcher) context;

        mWallpapers = new ArrayList<ApplicationInfo>(10);
        mThemes = new ArrayList<ApplicationInfo>(1);
        mEffects = ((LauncherApplication) context.getApplicationContext()).getScrollAnimList();
        updatePageCounts();

        // Save the default widget preview background
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0,
                0);
        //mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountX, -1);
        //mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountY, -1);
        mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 4);
        mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 1);
        mCellCountX =
                a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 4);
        mCellCountY =
                a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 1);
        a.recycle();
        mWidgetSpacingLayout = new PagedViewCellLayout(getContext());

        // The padding on the non-matched dimension for the default widget
        // preview icons
        // (top + bottom)
        mFadeInAdjacentScreens = false;
    }

    @Override
    protected void init() {
        super.init();
        mCenterPagesVertically = false;
    }

    /*@Override
    protected void onUnhandledTap(MotionEvent ev) {
        if (LauncherApplication.isScreenLarge()) {
            // Dismiss AppsCustomize if we tap
            mLauncher.showWorkspace(true);
        }
    }*/

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
            } else if (currentPage >= mNumWallpaperPages
                    && currentPage < mNumThemePages + mNumWallpaperPages) {
                int numWallpapers = mWallpapers.size();
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountX;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = numWallpapers +
                            ((currentPage - mNumWallpaperPages) * numItemsPerPage)
                            + (childCount / 2);
                }
            } else {
                int numThemes = mThemes.size() + mWallpapers.size();
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountX;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = numThemes +
                            ((currentPage - mNumWallpaperPages - mNumThemePages) * numItemsPerPage)
                            + (childCount / 2);
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
        for (int i = 0; i < mNumEffectPages; ++i) {
            layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
        
        for (int i = 0; i < mNumThemePages; ++i) {
            layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }

        for (int j = 0; j < mNumWallpaperPages; ++j) {
            layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
        if (page < mNumWallpaperPages) {
            syncWallpaperPageItems(page, immediate);
        } else if (page >= mNumWallpaperPages && page < mNumThemePages + mNumWallpaperPages) {
            syncThemePageItems(page - mNumWallpaperPages, immediate);
        } else if (page >= mNumThemePages + mNumWallpaperPages) {
            syncEffectPageItems(page - mNumWallpaperPages - mNumThemePages, immediate);
        } else {
            //do nothing
        }

    }

    private void syncWallpaperPageItems(int page, boolean immediate) {
        // TODO
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mWallpapers.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);

        layout.removeAllViewsOnPage();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mWallpapers.get(i);
            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
                    R.layout.apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, null);
            icon.setOnClickListener(this);
            // icon.setOnLongClickListener(this);
            // icon.setOnTouchListener(this);
            // icon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i,
                    new PagedViewCellLayout.LayoutParams(x, y, 1, 1));
        }

        layout.createHardwareLayers();
    }

    private void syncThemePageItems(int page, boolean immediate) {
        // TODO
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mThemes.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page + mNumWallpaperPages);

        layout.removeAllViewsOnPage();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mThemes.get(i);
            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
                    R.layout.apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, null);
            icon.setOnClickListener(this);
            // icon.setOnLongClickListener(this);
            // icon.setOnTouchListener(this);
            // icon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i,
                    new PagedViewCellLayout.LayoutParams(x, y, 1, 1));
        }

        layout.createHardwareLayers();
    }

	private void syncEffectPageItems(int page, boolean immediate) {
		int numCells = mCellCountX * mCellCountY;
		int startIndex = page * numCells;
		int endIndex = Math.min(startIndex + numCells, mEffects.size());
		PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page
				+ mNumWallpaperPages + mNumThemePages);

		layout.removeAllViewsOnPage();
		for (int i = startIndex; i < endIndex; ++i) {
			ScrollAnimStyleInfo animInfo = mEffects.get(i);
			PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
					R.layout.apps_customize_application, layout, false);
			icon.applyFromScrollAnimInfo(animInfo);
			icon.setOnClickListener(this);

			int index = i - startIndex;
			int x = index % mCellCountX;
			int y = index / mCellCountX;
			layout.addViewToCellLayout(icon, -1, i,
					new PagedViewCellLayout.LayoutParams(x, y, 1, 1));
		}

		layout.getChildrenLayout().mChildrenDoAnim = false;
		layout.createHardwareLayers();
	}

    private void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(mCellCountX, mCellCountY);
        layout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

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

    protected void onDataReady(int width, int height) {
        // Note that we transpose the counts in portrait so that we get a
        // similar layout
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

        // Now that the data is ready, we can calculate the content width, the
        // number of cells to
        // use for each page
        mWidgetSpacingLayout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        mWidgetSpacingLayout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);
        mWidgetSpacingLayout.calculateCellCount(width, height, maxCellCountX, maxCellCountY);
        mCellCountX = mWidgetSpacingLayout.getCellCountX();
        mCellCountY = mWidgetSpacingLayout.getCellCountY();
        // mCellCountX = 4;
        // mCellCountY = 1;
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
    }

    private void updatePageCounts() {
        mNumWallpaperPages = (int) Math.ceil((float) mWallpapers.size() /
                (mCellCountX * mCellCountY));
        mNumThemePages = (int) Math.ceil((float) mThemes.size() /
                (mCellCountX * mCellCountY));
        mNumEffectPages = (int) Math.ceil((float) mEffects.size() /
                (mCellCountX * mCellCountY));
    }

    public boolean checkDataReady() {
        return null != mWallpapers && !mWallpapers.isEmpty()
                && null != mThemes && !mThemes.isEmpty()
                && null != mEffects && !mEffects.isEmpty();
    }

    void restorePageForIndex(int index) {
        if (index < 0)
            return;
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
        if (null == mWallpapers) {
            mWallpapers = new ArrayList<ApplicationInfo>();
        }
        mWallpapers.clear();
        if (null == mThemes) {
            mThemes = new ArrayList<ApplicationInfo>();
        }
        mThemes.clear();
        setWallpapers();

        setThemes();
    }

    public void setWallpapers() {
        //TODO FIXME {
        loadWallpapersFromApps();
        int ids[] = new int[] {
                R.drawable.wallpaper_1, R.drawable.wallpaper_2,
                R.drawable.wallpaper_3, R.drawable.wallpaper_4,
                R.drawable.wallpaper_5, R.drawable.wallpaper_6,
                R.drawable.wallpaper_7, R.drawable.wallpaper_8,
                R.drawable.wallpaper_9, R.drawable.wallpaper_10
        };
        int ids_small[] = new int[] {
                R.drawable.wallpaper_1_small, R.drawable.wallpaper_2_small,
                R.drawable.wallpaper_3_small, R.drawable.wallpaper_4_small,
                R.drawable.wallpaper_5_small, R.drawable.wallpaper_6_small,
                R.drawable.wallpaper_7_small, R.drawable.wallpaper_8_small,
                R.drawable.wallpaper_9_small, R.drawable.wallpaper_10_small
        };
        String names[] = new String[] {
                "Classical", "Reaationary",
                "Brave", "Braw", 
                "Happy", "Nice", 
                "Rorty", "Dulcet", 
                "Splendid", "Wonderful"
        };
        ArrayList<ApplicationInfo> wallpapers = new ArrayList<ApplicationInfo>(ids.length);
        ApplicationInfo appInfo;
        int i = 0;
        SharedPreferences prefs =
                mLauncher.getSharedPreferences(Launcher.PREFS_KEY, Context.MODE_PRIVATE);
        int selectedWallpaper = prefs.getInt("selected_wallpaper", -1);
        for (int wallpaperId : ids)
        {
            appInfo = new ApplicationInfo();
            appInfo.title = names[i];
            appInfo.id = wallpaperId;
            appInfo.componentName = new ComponentName("Wallpaper", "Wallpaper");
            appInfo.iconBitmap = createIconBitmap(ids_small[i], mContext,
                    wallpaperId == selectedWallpaper, i == 0);
            i++;
            wallpapers.add(appInfo);
        }

        //} TODO FIXME
        Collections.sort(wallpapers, LauncherModel.APP_NAME_COMPARATOR);

        mWallpapers.addAll(wallpapers);

        updatePageCounts();

        // The next layout pass will trigger data-ready if both widgets and apps
        // are set, so
        // request a layout to do this test and invalidate the page data when
        // ready.
        if (checkDataReady())
            requestLayout();
    }

    private void loadWallpapersFromApps() {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.title = "Others";
        appInfo.id = 0;

        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper,
                mLauncher.getText(R.string.chooser_wallpaper));

        appInfo.intent = chooser;
        /*
         * BitmapFactory.Options options = new BitmapFactory.Options();
         * options.inPreferredConfig = Bitmap.Config.ARGB_8888;
         * appInfo.iconBitmap = BitmapFactory.decodeResource(getResources(),
         * R.drawable.wallpaper_app, options);
         */
        appInfo.iconBitmap = createIconBitmap(R.drawable.wallpaper_app, mContext, false, false);
        mWallpapers.add(appInfo);
        /*
         * PackageManager manager = mLauncher.getPackageManager(); Intent
         * mainIntent = new Intent(Intent.ACTION_SET_WALLPAPER, null); final
         * List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent,
         * 0); Collections.sort(apps, new
         * ResolveInfo.DisplayNameComparator(manager)); if (apps != null) {
         * final int count = apps.size(); if (mWallpapers == null) { mWallpapers
         * = new ArrayList<ApplicationInfo>(count); } mWallpapers.clear();
         * ApplicationInfo application = null; ResolveInfo info = null;
         * ComponentName cn = null; Intent intent = null; for (int i = 0; i <
         * count; i++) { application = new ApplicationInfo(); info =
         * apps.get(i); application.title = info.loadLabel(manager); cn = new
         * ComponentName( info.activityInfo.applicationInfo.packageName,
         * info.activityInfo.name); application.setActivity(cn,
         * Intent.FLAG_ACTIVITY_NEW_TASK |
         * Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); application.iconBitmap =
         * createIconBitmap(info.activityInfo.loadIcon(manager),
         * mContext);//info.activityInfo.loadIcon(manager); intent = new
         * Intent(Intent.ACTION_SET_WALLPAPER, null); intent.setComponent(cn);
         * application.intent = intent; mWallpapers.add(application); } }
         */
    }

    public void setThemes() {
        //TODO FIXME {
        int ids[] = new int[] {
                R.drawable.theme_1, R.drawable.theme_2, R.drawable.theme_3
        };
        String names[] = new String[] {
                "Default", "Classical", "Reaationary"
        };
        ApplicationInfo appInfo;
        int i = 0;
        SharedPreferences prefs =
                mLauncher.getSharedPreferences(Launcher.PREFS_KEY, Context.MODE_PRIVATE);
        int selectedTheme = prefs.getInt("selected_theme", -1);
        for (int id : ids)
        {
            appInfo = new ApplicationInfo();
            appInfo.title = names[i];
            i++;
            appInfo.id = id;
            appInfo.componentName = new ComponentName("Theme", "Theme");
            appInfo.iconBitmap = createIconBitmap(id, mContext, selectedTheme == id, i == 2);
            mThemes.add(appInfo);
        }

        //} TODO FIXME
        //Collections.sort(mThemes, LauncherModel.APP_NAME_COMPARATOR);

        updatePageCounts();

        // The next layout pass will trigger data-ready if both widgets and apps
        // are set, so
        // request a layout to do this test and invalidate the page data when
        // ready.
        if (checkDataReady())
            requestLayout();
    }

    Bitmap createIconBitmap(int resId, Context context, boolean isSelected, boolean isHot) {
        Resources resources = getResources();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Bitmap bgBitmap =
        // Utilities.createIconBitmap(resources.getDrawable(resId), -5, -5,
        // context);/*BitmapFactory.decodeResource(resources,
        // R.drawable.user_defined_icon_bd, options);*/
        // int width = bgBitmap.getWidth();
        // int height = bgBitmap.getHeight();
        // Bitmap originalImage =
        // Utilities.createIconBitmap(resources.getDrawable(R.drawable.user_defined_icon_bd),
        // 0, 0, context);
        // Bitmap originalImage = BitmapFactory.decodeResource(resources,
        // R.drawable.user_defined_icon_bd, options);

        // bgBitmap = Bitmap.createBitmap(bgBitmap, 0, 0, width, height);

        // Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, 0,
        // width, height);
        Bitmap bgBitmap = null;
        Bitmap originalImage = null;
        int width;
        bgBitmap = Utilities.createIconBitmap(resources.getDrawable(resId), 0, 0, context);

        if (isSelected) {
            width = bgBitmap.getWidth();
            originalImage = BitmapFactory.decodeResource(resources,
                    R.drawable.ic_selected, options);
            Canvas canvas = new Canvas(bgBitmap);
            canvas.drawBitmap(originalImage, width - originalImage.getWidth() + 5, -5, null);
        } else if (isHot) {
            width = bgBitmap.getWidth();
            originalImage = BitmapFactory.decodeResource(resources, R.drawable.ic_hot, options);
            Canvas canvas = new Canvas(bgBitmap);
            canvas.drawBitmap(originalImage, width - originalImage.getWidth(), 0, null);
        }

        return bgBitmap;

        // return
        // bgBitmap/*Utilities.createIconBitmap2(resources.getDrawable(resId),
        // context)*/;
    }

    Bitmap createIconBitmap(Drawable icon, Context context) {
        return Utilities.createIconBitmap(icon, context);
    }

    private UserDefinedSettingsTabHost getTabHost() {
        return (UserDefinedSettingsTabHost) mLauncher.findViewById(R.id.user_defined_settings);
    }

    int getPageForComponent(int index) {
        if (index < 0)
            return 0;

        int numItemsPerPage = mCellCountX * mCellCountY;
        if (index < mWallpapers.size()) {
            return (index / numItemsPerPage);
        } else if (index >= mWallpapers.size() && index < mThemes.size() + mWallpapers.size()) {
            return mNumWallpaperPages + ((index - mWallpapers.size()) / numItemsPerPage);
        } else {
            return mNumWallpaperPages + mNumThemePages
                    + ((index - mWallpapers.size() - mThemes.size()) / numItemsPerPage);
        }

    }

    void showAllAppsCling() {
    }

    private class WallpaperInfo extends ApplicationInfo {
        // TODO
    }

    private class ThemeInfo extends ApplicationInfo {
        // TODO
    }

    private class EffectInfo extends ApplicationInfo {
        // TODO
    }

    private boolean mBack = false;

    @Override
	public void onClick(View v) {

		if (!mLauncher.isUserDefinedOpen())
			return;

		if (v instanceof PagedViewIcon) {
			Object tag = v.getTag();
			if (tag instanceof ScrollAnimStyleInfo) {
				effectIconOnClick(v);
				return;
			}
			
			// Animate some feedback to the click
			final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();
			animateClickFeedback(v, new Runnable() {
				@Override
				public void run() {
					if (null != appInfo.intent) {
						mLauncher.startActivitySafely(appInfo.intent, appInfo);
					} else {
						try {
						    //TODO FIXME {
							ComponentName cn = appInfo.componentName;
							SharedPreferences prefs = mLauncher
									.getSharedPreferences(
									        Launcher.PREFS_KEY,
											Context.MODE_PRIVATE);
							SharedPreferences.Editor editor = prefs.edit();
							if (null != cn
									&& "Wallpaper".equals(cn.getClassName())) {
								WallpaperManager wpm = (WallpaperManager) mContext
										.getSystemService(Context.WALLPAPER_SERVICE);
								wpm.setResource((int) appInfo.id);
								editor.putInt("selected_wallpaper",
										(int) appInfo.id);
								editor.commit();
								UserDefinedSettingsPagedView.this.initData();
								UserDefinedSettingsPagedView.this
										.invalidatePageData();
							} else {
								if ("Theme".equals(cn.getClassName())) {
									editor.putInt("selected_theme",
											(int) appInfo.id);
									editor.commit();
									UserDefinedSettingsPagedView.this
											.initData();
									UserDefinedSettingsPagedView.this
											.invalidatePageData();
								}
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
									cPage -= 2;
								}
								if (cPage < 0) {
									mBack = false;
									cPage += 2;
								}
								cPage = Math.min(cPage, pages - 1);
								cPage = Math.max(cPage, 0);

								w.snapToPage(cPage);
							}
							//} TODO FIXME
						} catch (IOException e) {
						}
					}

				}
			});
		}

	}
    
	private void effectIconOnClick(View v) {
		final ScrollAnimStyleInfo animInfo = (ScrollAnimStyleInfo) v.getTag();
		animateClickFeedback(v, new Runnable() {
			@Override
			public void run() {
				mLauncher.getWorkspace().updateSelectedScrollAnimId(animInfo);
			}
		});
	}

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeKeyEvent(v, keyCode, event);
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
        /*
         * Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
         * while (iter.hasNext()) { AppsCustomizeAsyncTask task =
         * (AppsCustomizeAsyncTask) iter.next(); task.cancel(false);
         * iter.remove(); }
         */
    }

    public void setContentType(ContentType type) {
        /*
         * if (type == ContentType.Widgets) { invalidatePageData(mNumAppsPages,
         * true); } else if (type == ContentType.Applications) {
         * invalidatePageData(0, true); }
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
        /*
         * Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
         * while (iter.hasNext()) { AppsCustomizeAsyncTask task =
         * (AppsCustomizeAsyncTask) iter.next(); int pageIndex = task.page +
         * mNumAppsPages; if ((mNextPage > mCurrentPage && pageIndex >=
         * mCurrentPage) || (mNextPage < mCurrentPage && pageIndex <=
         * mCurrentPage)) {
         * task.setThreadPriority(getThreadPriorityForPage(pageIndex)); } else {
         * task.setThreadPriority(Process.THREAD_PRIORITY_LOWEST); } }
         */
    }

    /*
     * private int getThreadPriorityForPage(int page) { // TODO-APPS_CUSTOMIZE:
     * detect number of cores and set thread priorities accordingly below int
     * pageDiff = getWidgetPageLoadPriority(page); if (pageDiff <= 0) { return
     * Process.THREAD_PRIORITY_LESS_FAVORABLE; } else if (pageDiff <= 1) {
     * return Process.THREAD_PRIORITY_LOWEST; } else { return
     * Process.THREAD_PRIORITY_LOWEST; } }
     */

    /*
     * private int getWidgetPageLoadPriority(int page) { // If we are snapping
     * to another page, use that index as the target page index int toPage =
     * mCurrentPage; if (mNextPage > -1) { toPage = mNextPage; } // We use the
     * distance from the target page as an initial guess of priority, but if
     * there // are no pages of higher priority than the page specified, then
     * bump up the priority of // the specified page.
     * Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator(); int
     * minPageDiff = Integer.MAX_VALUE; while (iter.hasNext()) {
     * AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
     * minPageDiff = Math.abs(task.page + mNumAppsPages - toPage); } int
     * rawPageDiff = Math.abs(page - toPage); return rawPageDiff -
     * Math.min(rawPageDiff, minPageDiff); }
     */

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
                else if (currentPage < mNumEffectPages + mNumThemePages + mNumWallpaperPages
                        && currentPage >= mNumThemePages + mNumWallpaperPages
                        && !tag.equals(tabHost.getTabTagForContentType(ContentType.Effect))) {
                    tabHost.setCurrentTabFromContent(ContentType.Effect);
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
                    // On large screens we need to fade the page as it nears its
                    // leftmost position
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
                        // On the first page, we don't want the page to have any
                        // lateral motion
                        translationX = 0;
                    } else if (i == getChildCount() - 1 && scrollProgress > 0) {
                        // Overscroll to the right
                        v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the last page, we don't want the page to have any
                        // lateral motion.
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

                // If the view has 0 alpha, we set it to be invisible so as to
                // prevent
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
     * 
     * @return
     */
    public int getPageContentWidth() {
        return mContentWidth;
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();

        // We reset the save index when we change pages so that it will be
        // recalculated on next
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
         * int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
         * int stringId = R.string.default_scroll_format; int count = 0; if
         * (page < mNumAppsPages) { stringId =
         * R.string.apps_customize_apps_scroll_format; count = mNumAppsPages; }
         * else { page -= mNumAppsPages; stringId =
         * R.string.apps_customize_widgets_scroll_format; count =
         * mNumWidgetPages; } return String.format(mContext.getString(stringId),
         * page + 1, count);
         */
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int stringId = R.string.default_scroll_format;
        int count = 0;

        if (page < mNumWallpaperPages) {
            //stringId = R.string.apps_customize_apps_scroll_format;
            count = mNumWallpaperPages;
        } else if (page >= mNumWallpaperPages && page < mNumThemePages + mNumWallpaperPages) {
            page -= mNumWallpaperPages;
            //stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWallpaperPages + mNumThemePages;
        } else {
            page -= mNumWallpaperPages;
            page -= mNumThemePages;
            //stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWallpaperPages + mNumThemePages + mNumEffectPages;
        }

        return String.format(mContext.getString(stringId), page + 1, count);
    }

}
