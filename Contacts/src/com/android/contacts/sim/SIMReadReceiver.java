package com.android.contacts.sim;

import com.android.contacts.util.Constants;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/**
 * @author gangzhou.qi
 *
 */
public class SIMReadReceiver extends BroadcastReceiver{

	private static final boolean DEBUG = true;
	private static final String TAG = "SIMReadReceiver";
	private static String PHONEBOOK_READY = "android.intent.sim.phonebook.ready";
	private SIMCursorLoader mSIMCursorLoader;
	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		if (action == Intent.ACTION_BOOT_COMPLETED) {
			// context.startService(new
			// Intent(context,SIMPrepareService.class));
			// Added by gangzhou.qi at 2012-7-6 下午5:23:08
			if (DEBUG && Constants.TOTAL_DEBUG) {
				Log.d(TAG, "jiangzhou Receive the boot completed action!");
			}
			// Ended by gangzhou.qi at 2012-7-6 下午5:23:08
		} else if (action == PHONEBOOK_READY) {
			Log.d(TAG, "jiangzhou Receive hexiaobo's action!");
			Intent prepareSimIntent = new Intent(context,
					SimHelperService.class);
			prepareSimIntent.setAction(SimHelperService.ACTION_PREPARE);
			context.startService(prepareSimIntent);
		}
	}

}
