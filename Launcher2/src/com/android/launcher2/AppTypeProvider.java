package com.android.launcher2;

/**
 * added by zhongheng.zheng 2012.8.14 for app type provider
 * 
 */


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class AppTypeProvider extends ContentProvider{
	
	private static final String LOG_TAG = "AppTypeProvider";
	private static final boolean DEBUG = true;

	private SQLiteOpenHelper mOpenHelper;

	private static final int URI_APP = 1;
	private static final int URI_APP_ID = 2;
	private static final int URI_TYPE = 3;
	private static final int URI_TYPE_ID = 4;
	private static final int URI_ASSOC_APP_TYPE = 5;
	private static final int URI_ASSOC_APP_TYPE_ID = 6;
	private static final int URI_VIEW = 7;
	private static final int URI_VIEW_ID = 8;
	
	private static final UriMatcher sURLMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.AppColumns.TABLE_NAME, URI_APP);
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.AppColumns.TABLE_NAME + "/#", URI_APP_ID);
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.TypeColumns.TABLE_NAME, URI_TYPE);
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.TypeColumns.TABLE_NAME + "/#", URI_TYPE_ID);
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.AssocAppTypeColumns.TABLE_NAME, URI_ASSOC_APP_TYPE);
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.AssocAppTypeColumns.TABLE_NAME + "/#", URI_ASSOC_APP_TYPE_ID);
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.AssocViewColumns.VIEW_NAME, URI_VIEW);
		sURLMatcher.addURI(AppTypeInfo.AUTHORITY, AppTypeInfo.AssocViewColumns.VIEW_NAME + "/#", URI_VIEW_ID);
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new AppTypeDbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		// Generate the body of the query
		int match = sURLMatcher.match(uri);
		switch (match) {
		case URI_APP:
			qb.setTables(AppTypeInfo.AppColumns.TABLE_NAME);
			break;
		case URI_APP_ID:
			qb.setTables(AppTypeInfo.AppColumns.TABLE_NAME);
			qb.appendWhere(AppTypeInfo.AppColumns._ID);
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case URI_TYPE:
			qb.setTables(AppTypeInfo.TypeColumns.TABLE_NAME);
			break;
		case URI_TYPE_ID:
			qb.setTables(AppTypeInfo.TypeColumns.TABLE_NAME);
			qb.appendWhere(AppTypeInfo.TypeColumns._ID);
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case URI_ASSOC_APP_TYPE:
			qb.setTables(AppTypeInfo.AssocAppTypeColumns.TABLE_NAME);
			break;
		case URI_ASSOC_APP_TYPE_ID:
			qb.setTables(AppTypeInfo.AssocAppTypeColumns.TABLE_NAME);
			qb.appendWhere(AppTypeInfo.AssocAppTypeColumns._ID);
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case URI_VIEW:
			qb.setTables(AppTypeInfo.AssocViewColumns.VIEW_NAME);
			break;
		case URI_VIEW_ID:
			qb.setTables(AppTypeInfo.AssocViewColumns.VIEW_NAME);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor ret = qb.query(db, projection, selection, selectionArgs, null,
				null, sortOrder);

		if (ret != null) {
			ret.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return ret;
	}

	@Override
	public String getType(Uri uri) {
		int match = sURLMatcher.match(uri);
		switch (match) {
		case URI_APP:
		case URI_TYPE:
		case URI_ASSOC_APP_TYPE:
			return AppTypeInfo.CONTENT_TYPE;
		case URI_APP_ID:
		case URI_TYPE_ID:
		case URI_ASSOC_APP_TYPE_ID:
			return AppTypeInfo.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId;
		Uri rowUri = null;
		
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		
		String tableName = "";
    	switch (sURLMatcher.match(uri)) { 
    	case URI_APP:
    		tableName = AppTypeInfo.AppColumns.TABLE_NAME;
    		if (values.containsKey(AppTypeInfo.AppColumns.PKG_CLASS_NAME) == false) {
    			values.put(AppTypeInfo.AppColumns.PKG_CLASS_NAME, "");
    		}
    		if (values.containsKey(AppTypeInfo.AppColumns.IS_VISIBILITY) == false) {
    			values.put(AppTypeInfo.AppColumns.IS_VISIBILITY, 0);
    		}
    		rowId = db.insert(tableName, null, values);
    		if(rowId != -1){
    			rowUri = ContentUris.withAppendedId(AppTypeInfo.AppColumns.CONTENT_URI, rowId);
    			getContext().getContentResolver().notifyChange(rowUri, null);
    		}
    		break;
    	case URI_TYPE:
    		tableName = AppTypeInfo.TypeColumns.TABLE_NAME;
    		if (values.containsKey(AppTypeInfo.TypeColumns.TYPE_NAME) == false) {
    			values.put(AppTypeInfo.TypeColumns.TYPE_NAME, "");
    		}
    		if (values.containsKey(AppTypeInfo.TypeColumns.IS_USER_DEFINED) == false) {
    			values.put(AppTypeInfo.TypeColumns.IS_USER_DEFINED, 0);
    		}
    		rowId = db.insert(tableName, null, values);
    		if(rowId != -1){
    			rowUri = ContentUris.withAppendedId(AppTypeInfo.TypeColumns.CONTENT_URI, rowId);
    			getContext().getContentResolver().notifyChange(rowUri, null);
    		}
    		break;
    	case URI_ASSOC_APP_TYPE:
    		tableName = AppTypeInfo.AssocAppTypeColumns.TABLE_NAME;
    		if (values.containsKey(AppTypeInfo.AssocAppTypeColumns.ID_APP) == false) {
    			values.put(AppTypeInfo.AssocAppTypeColumns.ID_APP, -1);
    		}
    		if (values.containsKey(AppTypeInfo.AssocAppTypeColumns.ID_TYPE) == false) {
    			values.put(AppTypeInfo.AssocAppTypeColumns.ID_TYPE, -1);
    		}
    		rowId = db.insert(tableName, null, values);
    		if(rowId != -1){
    			rowUri = ContentUris.withAppendedId(AppTypeInfo.AssocAppTypeColumns.CONTENT_URI, rowId);
    			getContext().getContentResolver().notifyChange(rowUri, null);
    		}
    		break;
    	default:
			// Validate the requested uri
			throw new IllegalArgumentException("Unknown URI " + uri);	
    	}

		return rowUri;
	}

	
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		if(DEBUG){
			String strUri = uri.toString();
			Log.d(LOG_TAG, "uri : " + strUri);
			Log.d(LOG_TAG, "where : " + where);
			Log.d(LOG_TAG, "whereArgs: " + whereArgs[0]);
		}
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		long rowId = 0;
		switch (sURLMatcher.match(uri)) {
		case URI_APP:
			count = db.delete(AppTypeInfo.AppColumns.TABLE_NAME, where, whereArgs);
			break;
		case URI_APP_ID:
			String segmentApp = uri.getPathSegments().get(1);
			rowId = Long.parseLong(segmentApp);
			if (TextUtils.isEmpty(where)) {
				where = AppTypeInfo.AppColumns._ID + rowId;
			} else {
				where = AppTypeInfo.AppColumns._ID + rowId + " AND (" + where + ")";
			}
			count = db.delete(AppTypeInfo.AppColumns.TABLE_NAME, where, whereArgs);
			break;
		case URI_TYPE:
			count = db.delete(AppTypeInfo.TypeColumns.TABLE_NAME, where, whereArgs);
			break;
		case URI_TYPE_ID:
			String segmentType = uri.getPathSegments().get(1);
			rowId = Long.parseLong(segmentType);
			if (TextUtils.isEmpty(where)) {
				where = AppTypeInfo.TypeColumns._ID + rowId;
			} else {
				where = AppTypeInfo.TypeColumns._ID + rowId + " AND (" + where + ")";
			}
			count = db.delete(AppTypeInfo.TypeColumns.TABLE_NAME, where, whereArgs);
			break;
		case URI_ASSOC_APP_TYPE:
			count = db.delete(AppTypeInfo.AssocAppTypeColumns.TABLE_NAME, where, whereArgs);
			break;
		case URI_ASSOC_APP_TYPE_ID:
			String segmentAssoc = uri.getPathSegments().get(1);
			rowId = Long.parseLong(segmentAssoc);
			if (TextUtils.isEmpty(where)) {
				where = AppTypeInfo.AssocAppTypeColumns._ID + rowId;
			} else {
				where = AppTypeInfo.AssocAppTypeColumns._ID + rowId + " AND (" + where + ")";
			}
			count = db.delete(AppTypeInfo.AssocAppTypeColumns.TABLE_NAME, where, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Cannot delete from URi: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	
	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		if(DEBUG){
			String strUri = uri.toString();
			Log.d(LOG_TAG, "uri : " + strUri);
			Log.d(LOG_TAG, "where : " + where);
			Log.d(LOG_TAG, "whereArgs: " + whereArgs);
		}
		int count;
		long rowId = 0;
		int match = sURLMatcher.match(uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		switch (match) {
		case URI_APP:
			count = db.update(AppTypeInfo.AppColumns.TABLE_NAME, values, where, whereArgs);
			break;
		case URI_APP_ID:
			String segmentApp = uri.getPathSegments().get(1);
			rowId = Long.parseLong(segmentApp);
			if (TextUtils.isEmpty(where)) {
				where = AppTypeInfo.AppColumns._ID + rowId;
			} else {
				where = AppTypeInfo.AppColumns._ID + rowId + " AND (" + where + ")";
			}
			count = db.update(AppTypeInfo.AppColumns.TABLE_NAME, values, where, whereArgs);
			break;
		case URI_TYPE:
			count = db.update(AppTypeInfo.TypeColumns.TABLE_NAME, values, where, whereArgs);
			break;
		case URI_TYPE_ID:
			String segmentType = uri.getPathSegments().get(1);
			rowId = Long.parseLong(segmentType);
			if (TextUtils.isEmpty(where)) {
				where = AppTypeInfo.TypeColumns._ID + rowId;
			} else {
				where = AppTypeInfo.TypeColumns._ID + rowId + " AND (" + where + ")";
			}
			count = db.update(AppTypeInfo.TypeColumns.TABLE_NAME, values, where, whereArgs);
			break;
		case URI_ASSOC_APP_TYPE:
			count = db.update(AppTypeInfo.AssocAppTypeColumns.TABLE_NAME, values, where, whereArgs);
			break;
		case URI_ASSOC_APP_TYPE_ID:
			String segmentAssoc = uri.getPathSegments().get(1);
			rowId = Long.parseLong(segmentAssoc);
			if (TextUtils.isEmpty(where)) {
				where = AppTypeInfo.AssocAppTypeColumns._ID + rowId;
			} else {
				where = AppTypeInfo.AssocAppTypeColumns._ID + rowId + " AND (" + where + ")";
			}
			count = db.update(AppTypeInfo.AssocAppTypeColumns.TABLE_NAME, values, where, whereArgs);
			break;
		default:
			throw new UnsupportedOperationException("Cannot update URi: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

}
