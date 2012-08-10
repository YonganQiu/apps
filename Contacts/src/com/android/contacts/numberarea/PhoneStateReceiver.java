package com.android.contacts.numberarea;

import com.android.contacts.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Show area of incomeing call or outgoing call in call-screen when calling.
 * @author yongan.qiu
 */
public class PhoneStateReceiver extends BroadcastReceiver {

    private static final String TAG = PhoneStateReceiver.class.getSimpleName();

    private static final boolean DEBUG = Constants.TOTAL_DEBUG;

    private boolean isInComeCall = false;

    private String inComeCallNumber = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "Receive intent : " + intent);
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            isInComeCall = false;
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            if (DEBUG) Log.d(TAG, "call OUT:" + phoneNumber);
        } else {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            switch (tm.getCallState()) {
                case TelephonyManager.CALL_STATE_RINGING:
                    isInComeCall = true;
                    inComeCallNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    if (DEBUG) Log.d(TAG, "RINGING :" + inComeCallNumber);
                    String area = NumberAreaQuery.query(context, inComeCallNumber);
                    // If not found in local database, then query it online.
                    if (area == null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String areaByOnline = OnlineUpdateUtil.getNumberArea(inComeCallNumber);
                                if (DEBUG) Log.d(TAG, "area query by online is " + areaByOnline);
                                // TODO should show it somewhere, for example the in-call screen
                                // TODO Save this area to local database!
                            }
                        }).start();
                    } else {
                        // TODO should show the area somewhere, for example the in-call screen.
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isInComeCall) {
                        if (DEBUG) Log.d(TAG, "incoming ACCEPT :" + inComeCallNumber);
                    }
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (isInComeCall) {
                        if (DEBUG) Log.d(TAG, "incoming IDLE");
                    }
                    break;
            }
        }
    }
}
