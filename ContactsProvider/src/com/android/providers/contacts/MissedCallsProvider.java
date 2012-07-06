package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class MissedCallsProvider extends ContentProvider {

	public static final String AUTHORITY = "missed_calls";
	public static final String PATH = "number";

	private static final String TAG = MissedCallsProvider.class.getSimpleName();

	private static final String SHARED_PREFS_NAME = "MissedCallsProvider";

	private static final String KEY_MISSED_CALLS = "missed_calls";
	
	private static final int URI_CODE_MISSED_CALLS = 1;
	private static final int URI_CODE_MISSED_CALLS_ID = 2;

	public static final String URI_MIME_MISSED_CALLS = "vnd.android.cursor.dir/vnd.missed_calls";
	public static final String URI_ITEM_MIME_MISSED_CALLS = "vnd.android.cursor.item/vnd.missed_calls";

	private static UriMatcher mUriMatcher;

	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(AUTHORITY, PATH, URI_CODE_MISSED_CALLS);
		mUriMatcher.addURI(AUTHORITY, PATH + "/#",
				URI_CODE_MISSED_CALLS_ID);
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}

	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case URI_CODE_MISSED_CALLS:
			return URI_MIME_MISSED_CALLS;
		case URI_CODE_MISSED_CALLS_ID:
			return URI_ITEM_MIME_MISSED_CALLS;
		default:
			Log.e(TAG, "Unknown URI:" + uri);
			throw new IllegalArgumentException("Unknown URI:" + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SharedPreferences sharedPrefs = getSharedPreferences(Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt(KEY_MISSED_CALLS, values.getAsInteger(KEY_MISSED_CALLS));
		editor.commit();
		return Uri.parse("content://" + AUTHORITY + "/" + PATH + "/1");
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SharedPreferences sharedPrefs = getSharedPreferences(Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.clear();
		editor.commit();
		return 1;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SharedPreferences sharedPrefs = getSharedPreferences(Context.MODE_WORLD_READABLE);
		MatrixCursor cursor = new MatrixCursor(new String[]{KEY_MISSED_CALLS}, 1);
		cursor.addRow(new Object[]{sharedPrefs.getInt(KEY_MISSED_CALLS, 0)});
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SharedPreferences sharedPrefs = getSharedPreferences(Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt(KEY_MISSED_CALLS, values.getAsInteger(KEY_MISSED_CALLS));
		editor.commit();
		return 1;
	}
	
	private SharedPreferences getSharedPreferences(int mode) {
		return getContext().getSharedPreferences(SHARED_PREFS_NAME, mode);
	}
}