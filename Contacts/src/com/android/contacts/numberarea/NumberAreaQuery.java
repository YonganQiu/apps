package com.android.contacts.numberarea;

import com.android.contacts.numberarea.NumberArea.Area;
import com.android.contacts.numberarea.NumberArea.PhoneNumber;
import com.android.contacts.numberarea.NumberArea.Tables;
import com.android.contacts.util.Constants;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * A utility to query the location of number from local database.
 * @author yongan.qiu
 */
public class NumberAreaQuery {

    private static final String TAG = NumberAreaQuery.class.getSimpleName();

    private static final boolean DEBUG = Constants.TOTAL_DEBUG;

    private static final String[] PROJECTION = new String[] {Area.AREA};

    private static final int COLUMN_INDEX_AREA = 0;

    private static final String SELECTION = Area._ID + "=(SELECT " + PhoneNumber.AREA_ID
            + " FROM " + Tables.PHONE_NUMBER + " WHERE " + PhoneNumber.NUMBER + "=?)";

    public static String query(Context context, CharSequence number) {
        if (number != null && NumberArea.isValidPhoneNumber(number.toString())) {
            String numberStr = NumberArea.cropValidPhoneNumber(number.toString());
            Cursor cursor = context.getContentResolver().query(NumberArea.Area.CONTENT_URI, PROJECTION, SELECTION, new String[] {numberStr}, null);
            if (cursor != null && cursor.moveToFirst()) {
                String area = cursor.getString(COLUMN_INDEX_AREA);
                if (DEBUG) {
                    Log.d(TAG, "query: area is " + area);
                }
                return area;
            }
        }
        Log.w(TAG, "area query fail.");
        return null;
    }
}
