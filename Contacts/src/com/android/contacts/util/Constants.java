/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.util;

import java.util.concurrent.TimeUnit;

import android.net.Uri;

public class Constants {
    public static final String MIME_TYPE_VIDEO_CHAT = "vnd.android.cursor.item/video-chat-address";

    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_SIP = "sip";

    /**
     * Log tag for performance measurement.
     * To enable: adb shell setprop log.tag.ContactsPerf VERBOSE
     */
    public static final String PERFORMANCE_TAG = "ContactsPerf";

    /**
     * Log tag for enabling/disabling StrictMode violation log.
     * To enable: adb shell setprop log.tag.ContactsStrictMode DEBUG
     */
    public static final String STRICT_MODE_TAG = "ContactsStrictMode";
    
    //begin: added by yunzhou.song
    public final static String ONE_KEY_DIAL_SETTING_SHARED_PREFS_NAME = "one.key.dial.setting";
    public final static String IP_NUMBER_SETTING_SHARED_PREFS_NAME = "ip.number.setting";
    public static final String PERF_KEY_CONTACT_ID = "contact_id";
    public static final String PERF_KEY_PHONE_ID = "phone_id";
    public static final String PERF_KEY_NUMBER = "phone_number";
    public static final String PREF_KEY_FILTES_PHONES = "filter_phones";
    
    public static final String EXTRA_MULTIPLE_CHOICE = "multiple_choice";
    public static final String EXTRA_GROUP_ID = "group_id";
    public static final String EXTRA_ACCOUNT_TYPE = "account_type";
    public static final String EXTRA_ACCOUNT_NAME = "account_name";
    //end: added by yunzhou.song
    //{Added by yongan.qiu on 2012-7-16 begin.
    public static final String EXTRA_CONTACT_LIST_FILTER = "contact_list_filter";
    public static final String EXTRA_SELECTION = "extra_selection";
    public static final String EXTRA_ACTION_TITLE = "action_title";
    public static final String EXTRA_ACTION_ICON = "action_icon";
    public static final String EXTRA_EMAIL_URIS =
            "com.android.contacts.extra.EMAIL_URIS";

    /**
     * The URI of call log that can return query cursor grouping by number.
     */
    public static final Uri CALL_LOG_GROUP_BY_NUMBER =
            Uri.parse("content://call_log/calls/group_by_number");
    //}Added by yongan.qiu end.
    
    //{Added by yongan.qiu on 2012-7-31 begin.
    public static final String ACTION_MULTI_PICK = "com.android.contacts.intent.action.MULTI_PICK";
    //}Added by yongan.qiu end.

    //{Added by yongan.qiu on 2012-8-7 begin.
    /** CONTENT_TYPE of sim contact. */
    public static final String SIM_CONTACT_CONTENT_TYPE = "sim/contact";
    //}Added by yongan.qiu end.

    //{Added by yongan.qiu on 2012-8-10 begin.
    /**
     * The time window from the current time within which call log will be added to the list.
     */
    public static final long NEW_SECTION_TIME_WINDOW = TimeUnit.DAYS.toMillis(7);
    //}Added by yongan.qiu end.

    //Added by gangzhou.qi at 2012-6-27 8:22:57
    public static final int CALL_TYPE_ALL = 0;
    public static final int CALL_TYPE_IN = 1;
    public static final int CALL_TYPE_OUT = 2;
    public static final int CALL_TYPE_MISSED = 3;
	//Ended by gangzhou.qi at 2012-6-27 8:22:57
    //Added by gangzhou.qi at 2012-7-5 9:59:42
    public static final boolean TOTAL_DEBUG = true;
	//Ended by gangzhou.qi at 2012-7-5 9:59:42
    //Begin by gangzhou.qi at 2012-7-13 10:09:04
    public static final String SIM_OPERATION_TYPE = "sim_operation_type";
    public static final String SIM_OPERATION_ARRAY = "sim_operation_array";
    public static final int SIM_OPERATION_UNKNOWN = 0;
    public static final int SIM_OPERATION_IMPORT = 1;
    public static final int SIM_OPERATION_EXPORT = 2;
    public static final int SIM_OPERATION_DELETE = 3;
    public static final int SIM_OPERATION_CANCEL = 4;
    public static final int SIM_OPERATION_IMPORT_ALL = 5;
    
    public static final String SIM_READING_STATE = "sim_reading_state";
	//Ended by gangzhou.qi at 2012-7-13 10:09:04
    public static final String MMS_SEND_TO = "android.intent.action.COMPOSE_MESSAGE";
}
