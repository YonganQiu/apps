package com.android.launcher2;

import java.util.ArrayList;
import java.util.List;

import com.android.launcher2.LauncherSettings.Favorites;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher.R;

public class AddWidgetReceiver extends BroadcastReceiver {

	private static final String TAG = "AddWidgetReceiver";

    private final int[] mCoordinates = new int[2];
    AppWidgetManager mAppWidgetManager;
    boolean mFindEmptyCells;
    @Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onReceive(): " + intent);
		ComponentName appWidgetProvider = intent.getParcelableExtra("appwidget_provider");
		Log.i(TAG, "onReceive(): " + appWidgetProvider + "");
		
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        Launcher launcher = app.getLauncher();
        
        boolean success = false;
        Intent resultIntent = new Intent("com.android.launcher.action.ADD_WIDGET_RESULT");
        Bundle extras = intent.getExtras();

        if (launcher == null) {
            Log.e(TAG, "Launcher is not ready!");
            //Toast to tell user that home has not been setup.
            Toast.makeText(context, R.string.home_not_ready, Toast.LENGTH_SHORT).show();
            extras.putBoolean("success", false);
        } else {
            if (mAppWidgetManager == null) {
            	mAppWidgetManager = AppWidgetManager.getInstance(launcher);
            }
            List<AppWidgetProviderInfo> appWidgetInfos = mAppWidgetManager.getInstalledProviders();
            AppWidgetProviderInfo appWidgetInfo = findAppWidgetProviderInfo(appWidgetInfos, appWidgetProvider);

            int screen = Launcher.getScreen();
            int appWidgetId = addWidget(context, appWidgetInfo, screen);
            success = (appWidgetId >= 0);
            if (!success) {
                // The target screen is full, let's try the other screens
                // {modify by jingjiang.yu at 2012.08.04 begin for scale preview.
                // for (int i = 0; i < Launcher.SCREEN_COUNT; i++) {
                for (int i = 0; i < Launcher.screenCount; i++) {
                // }modify by jingjiang.yu end
                    appWidgetId = addWidget(context, appWidgetInfo, i);
                    success = (appWidgetId >= 0);
                    if (i != screen && success) break;
                }
            }
            if (success) {
                extras.putBoolean("success", true);
                extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                Log.i(TAG, "onReceive(): add widget success, appWidgetId = " + appWidgetId);
            } else {
                if (mFindEmptyCells) {
                    Toast.makeText(context, R.string.out_of_space, Toast.LENGTH_SHORT).show();
                }
                Log.i(TAG, "onReceive(): add widget failed, appWidgetId = " + appWidgetId);
                extras.putBoolean("success", false);
            }
        }


        resultIntent.putExtras(extras);
        context.sendBroadcast(resultIntent);
	}
	
    private int addWidget(Context context, AppWidgetProviderInfo appWidgetInfo, int screen) {
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        Launcher launcher = app.getLauncher();
        int[] span = launcher.getSpanForWidget(appWidgetInfo, null);
        mFindEmptyCells = findEmptyCells(context, mCoordinates, span, screen);
        int appWidgetId = -1;
        if (mFindEmptyCells) {
            PendingAddWidgetInfo info = new PendingAddWidgetInfo(appWidgetInfo, null, null);
            appWidgetId = launcher.addAppWidgetFromExternal(info, Favorites.CONTAINER_DESKTOP, screen, null, null);
        }
        return appWidgetId;
    }

    private AppWidgetProviderInfo findAppWidgetProviderInfo(List<AppWidgetProviderInfo> appWidgetInfos, ComponentName componentName) {
        if (appWidgetInfos == null || appWidgetInfos.size() == 0) {
            return null;
        }
        for (AppWidgetProviderInfo info : appWidgetInfos) {
            if (info.provider.equals(componentName)) {
                return info;
            }
        }
        return null;
        
    }
    
    private static boolean findEmptyCells(Context context, int[] xy, int[] span, int screen) {
        final int xCount = LauncherModel.getCellCountX();
        final int yCount = LauncherModel.getCellCountY();
        boolean[][] occupied = new boolean[xCount][yCount];

        ArrayList<ItemInfo> items = LauncherModel.getItemsInLocalCoordinates(context);
        ItemInfo item = null;
        int cellX, cellY, spanX, spanY;
        for (int i = 0; i < items.size(); ++i) {
            item = items.get(i);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (item.screen == screen) {
                    cellX = item.cellX;
                    cellY = item.cellY;
                    spanX = item.spanX;
                    spanY = item.spanY;
                    for (int x = cellX; x < cellX + spanX && x < xCount; x++) {
                        for (int y = cellY; y < cellY + spanY && y < yCount; y++) {
                            occupied[x][y] = true;
                        }
                    }
                }
            }
        }

        return CellLayout.findVacantCell(xy, span[0], span[1], xCount, yCount, occupied);
    }

}
