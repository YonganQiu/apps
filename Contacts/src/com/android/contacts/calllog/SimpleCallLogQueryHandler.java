/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.calllog;

import com.android.common.io.MoreCloseables;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;
import com.google.android.collect.Lists;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.VoicemailContract.Status;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

/** Handles asynchronous queries to the call log. */
    public class SimpleCallLogQueryHandler extends AsyncQueryHandler {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String TAG = "CallLogQueryHandler";

    private static final int QUERY_ALL_UNKNOWN_CALLS_TOKEN = 55;
    /**
     * The time window from the current time within which an unread entry will be added to the new
     * section.
     */
    private static final long NEW_SECTION_TIME_WINDOW = TimeUnit.DAYS.toMillis(7);

    private final WeakReference<Listener> mListener;

    /** The cursor containing the new calls, or null if they have not yet been fetched. */
    @GuardedBy("this") private Cursor mAllUnknownCallsCursor;
    /** The cursor containing the old calls, or null if they have not yet been fetched. */
    @GuardedBy("this") private Cursor mOldCallsCursor;

    /**
     * Simple handler that wraps background calls to catch
     * {@link SQLiteException}, such as when the disk is full.
     */
    protected class CatchingWorkerHandler extends AsyncQueryHandler.WorkerHandler {
        public CatchingWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                // Perform same query while catching any exceptions
                super.handleMessage(msg);
            } catch (SQLiteDiskIOException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            } catch (SQLiteFullException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            } catch (SQLiteDatabaseCorruptException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            }
        }
    }

    @Override
    protected Handler createHandler(Looper looper) {
        // Provide our special handler that catches exceptions
        return new CatchingWorkerHandler(looper);
    }

    public SimpleCallLogQueryHandler(ContentResolver contentResolver, Listener listener) {
        super(contentResolver);
        mListener = new WeakReference<Listener>(listener);
    }

    /**
     * Fetches the list of calls from the call log.
     * <p>
     * It will asynchronously update the content of the list view when the fetch completes.
     */
    public void fetchAllUnknownCalls() {
        cancelFetch();
        invalidate();
        String selection = String.format("(%s IS NULL OR %s = 0) AND %s > ?",
                Calls.CACHED_NUMBER_TYPE, Calls.CACHED_NUMBER_TYPE, Calls.DATE);
        List<String> selectionArgs = Lists.newArrayList(
                Long.toString(System.currentTimeMillis() - NEW_SECTION_TIME_WINDOW));
        if (!TextUtils.isEmpty(mQueryString)) {
            selection = String.format("%s AND %s LIKE '%%%s%%'", selection, Calls.NUMBER, mQueryString);
        }
        //selection = String.format("%s) GROUP BY (%s", selection, Calls.NUMBER);
        Log.i(TAG, selection);
        Uri uri = Uri.parse("content://call_log/calls/group_by_number").buildUpon()
                .appendQueryParameter(CallLog.Calls.ALLOW_VOICEMAILS_PARAM_KEY, "true")
                .build();
        startQuery(QUERY_ALL_UNKNOWN_CALLS_TOKEN, null, /*Calls.CONTENT_URI_WITH_VOICEMAIL*/uri,
                SimpleCallLogQuery._PROJECTION, selection, selectionArgs.toArray(EMPTY_STRING_ARRAY),
                Calls.DEFAULT_SORT_ORDER);
    }

    String mQueryString;
    public void setQueryString(String queryString) {
    	mQueryString = queryString;
    }

    /** Cancel any pending fetch request. */
    private void cancelFetch() {
        cancelOperation(QUERY_ALL_UNKNOWN_CALLS_TOKEN);
    }

    /**
     * Invalidate the current list of calls.
     * <p>
     * This method is synchronized because it must close the cursors and reset them atomically.
     */
    private synchronized void invalidate() {
        MoreCloseables.closeQuietly(mAllUnknownCallsCursor);
        MoreCloseables.closeQuietly(mOldCallsCursor);
        mAllUnknownCallsCursor = null;
        mOldCallsCursor = null;
    }

    @Override
    protected synchronized void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (token == QUERY_ALL_UNKNOWN_CALLS_TOKEN) {
            // Store the returned cursor.
            mAllUnknownCallsCursor = cursor;
        } else {
            Log.w(TAG, "Unknown query completed: ignoring: " + token);
            return;
        }

        if (mAllUnknownCallsCursor != null) {
            updateAdapterData(mAllUnknownCallsCursor);
            mAllUnknownCallsCursor = null;
        }
    }

    /**
     * Updates the adapter in the call log fragment to show the new cursor data.
     */
    private void updateAdapterData(Cursor combinedCursor) {
        final Listener listener = mListener.get();
        if (listener != null) {
            listener.onCallsFetched(combinedCursor);
        }
    }

    /** Listener to completion of various queries. */
    public interface Listener {
        /**
         * Called when {@link CallLogQueryHandler#fetchAllCalls()} or
         * {@link CallLogQueryHandler#fetchVoicemailOnly()} complete.
         */
        void onCallsFetched(Cursor combinedCursor);
    }

    public static final class SimpleCallLogQuery {
        public static final String[] _PROJECTION = new String[] {
                Calls._ID,                       // 0
                Calls.NUMBER,                    // 1
        };

        public static final int ID = 0;
        public static final int NUMBER = 1;
    }
}
