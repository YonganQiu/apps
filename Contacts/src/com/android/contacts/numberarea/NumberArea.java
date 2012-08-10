package com.android.contacts.numberarea;

import android.net.Uri;

/**
 * Contains constants used in area database.
 * @author yongan.qiu
 */
public class NumberArea {
    public static class PhoneNumber {
        public static final Uri CONTENT_URI =
                Uri.parse("content://number_area/phone_number");
        public static final String NUMBER = "number";
        public static final String AREA_ID = "area_id";
    }

    public static class Area {
        public static final Uri CONTENT_URI =
                Uri.parse("content://number_area/area");
        public static final String _ID = "_id";
        public static final String AREA = "area";
    }

    public static class PhoneNumberArea {
        public static final Uri CONTENT_URI =
                Uri.parse("content://number_area/phone_number_area");
    }

    public static class Tables {
        public static final String PHONE_NUMBER = "phone_number";
        public static final String AREA = "area";
        public static final String PHONE_NUMBER_JOIN_AREA = PHONE_NUMBER + " JOIN " + AREA
                + " ON " + "(" + PHONE_NUMBER + "." + PhoneNumber.AREA_ID
                + "=" + AREA + "." + Area._ID + ")";
    }

    public static final String AUTHORITY = "number_area";

    static final String DB_DIR = "/sdcard/";

    static final String DB_FILE = "callv1.1.db";

    private static final int NUMBER_PREFIX_LENGTH = 7;

    public static boolean isValidPhoneNumber(String number) {
        // Should be longer than NUMBER_PREFIX_LENGTH and start with 1(mobile phone number).
        if (number == null || !number.startsWith("1") || number.length() < NUMBER_PREFIX_LENGTH) {
            return false;
        }
        return true;
    }

    public static String cropValidPhoneNumber(String number) {
        if (number.length() > NUMBER_PREFIX_LENGTH) {
            number = number.substring(0, NUMBER_PREFIX_LENGTH);
        }
        return number;
    }
}
