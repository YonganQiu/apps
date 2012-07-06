package com.android.contacts.sim;

import com.android.contacts.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SIMReadReceiver extends BroadcastReceiver{

	private static final boolean DEBUG = true;
	private static final String TAG = "SIMReadReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		if(action == Intent.ACTION_BOOT_COMPLETED){
			//Added by gangzhou.qi at 2012-7-6 下午5:23:08
			if (DEBUG && Constants.TOTAL_DEBUG) {
				Log.d(TAG, "jiangzhou Receive the boot completed action!");
			}
			//Ended by gangzhou.qi at 2012-7-6 下午5:23:08
		}
	}

}
