package com.android.launcher2;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

public class RemoveWidgetReceiver extends BroadcastReceiver {

	private static final String TAG = "RemoveWidgetReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onReceive(): " + intent);
		int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		Log.i(TAG, "onReceive(): appWidgetId = " + appWidgetId);
		if (appWidgetId < 0) {
			Log.e(TAG, "onReceive(): appwidget id not available!");
			return;
		}
		
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        Launcher launcher = app.getLauncher();
        if (launcher == null) {
            Log.e(TAG, "Launcher is not ready!");
            //Toast to tell user that home has not been setup.
            return;
        }

        int N = LauncherModel.sAppWidgets.size();
        LauncherAppWidgetInfo appWidgetInfo = null;
        for (int i=0; i<N; i++) {
            final LauncherAppWidgetInfo widget = LauncherModel.sAppWidgets.get(i);
            if (widget.appWidgetId == appWidgetId) {
                appWidgetInfo = widget;
                break;
            }
        }
        if (appWidgetInfo != null) {
            //Record the widget.
            View view = appWidgetInfo.hostView;
            // Remove the widget from the workspace
            launcher.removeAppWidget(appWidgetInfo);
            Workspace workspace = launcher.getWorkspace();
            workspace.getParentCellLayoutForView(view).removeView(view);
            LauncherModel.deleteItemFromDatabase(launcher, appWidgetInfo);

            final LauncherAppWidgetInfo launcherAppWidgetInfo = appWidgetInfo;
            final LauncherAppWidgetHost appWidgetHost = launcher.getAppWidgetHost();
            if (appWidgetHost != null) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                    }
                }.start();
            }
        }
	}

}
