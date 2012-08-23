package com.android.launcher2;

/**
 * added by zhongheng.zheng 2012.8.14 for app type db helper
 * 
 */

import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.android.launcher.R;
import com.android.launcher2.AppTypeInfo.AppColumns;
import com.android.launcher2.AppTypeInfo.AssocAppTypeColumns;
import com.android.launcher2.AppTypeInfo.TypeColumns;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class AppTypeDbHelper extends SQLiteOpenHelper {

	private static final String LOG_TAG = "AppTypeDbHelper";
	private static final boolean DEBUG = true;

	private static final String DATABASE_NAME = "apptype.db";
	private static String XML_APP_TYPE = "app_type";
	private static String XML_ATTR_TYPE_ID = "type_id";

	private static final int DATABASE_VERSION = 1;

	private Context mContext;

	public AppTypeDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		if (DEBUG) {
			Log.d(LOG_TAG, "AppTypeDbHelper onCreate() begin");
		}

		// app table
		db.execSQL("CREATE TABLE " + AppTypeInfo.AppColumns.TABLE_NAME + " ("
				+ AppTypeInfo.AppColumns._ID + " INTEGER PRIMARY KEY,"
				+ AppTypeInfo.AppColumns.PKG_CLASS_NAME + " TEXT, "
				+ AppTypeInfo.AppColumns.IS_VISIBILITY + " INTEGER" + ");");

		// type table
		db.execSQL("CREATE TABLE " + AppTypeInfo.TypeColumns.TABLE_NAME + " ("
				+ AppTypeInfo.TypeColumns._ID + " INTEGER PRIMARY KEY,"
				+ AppTypeInfo.TypeColumns.TYPE_NAME + " TEXT, "
				+ AppTypeInfo.TypeColumns.IS_USER_DEFINED + " INTEGER" + ");");

		// association app and type table
		db.execSQL("CREATE TABLE " + AppTypeInfo.AssocAppTypeColumns.TABLE_NAME
				+ " (" + AppTypeInfo.AssocAppTypeColumns._ID
				+ " INTEGER PRIMARY KEY,"
				+ AppTypeInfo.AssocAppTypeColumns.ID_APP + " INTEGER, "
				+ AppTypeInfo.AssocAppTypeColumns.ID_TYPE + " INTEGER" + ");");

		// view from 3 tables
		String viewSelect = "SELECT " + AppColumns.TABLE_NAME + "."
				+ AppColumns._ID + " AS " + "AppColumns_ID" + ","
				+ AppColumns.PKG_CLASS_NAME + "," + AppColumns.IS_VISIBILITY
				+ "," + TypeColumns.TABLE_NAME + "." + TypeColumns._ID + " AS "
				+ "TypeColumns_ID" + "," + TypeColumns.TYPE_NAME + ","
				+ TypeColumns.IS_USER_DEFINED + ","
				+ AssocAppTypeColumns.TABLE_NAME + "."
				+ AssocAppTypeColumns._ID + " AS " + "AssocAppTypeColumns_ID"
				+ "," + AssocAppTypeColumns.ID_APP + ","
				+ AssocAppTypeColumns.ID_TYPE + " FROM "
				+ AppColumns.TABLE_NAME + "," + TypeColumns.TABLE_NAME + ","
				+ AssocAppTypeColumns.TABLE_NAME + " WHERE "
				+ AssocAppTypeColumns.ID_APP + " = " + AppColumns.TABLE_NAME
				+ "." + AppColumns._ID + " AND " + AssocAppTypeColumns.ID_TYPE
				+ " = " + TypeColumns.TABLE_NAME + "." + TypeColumns._ID;

		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("CREATE VIEW ");
		strBuilder.append(AppTypeInfo.AssocViewColumns.VIEW_NAME);
		strBuilder.append(" AS ");
		strBuilder.append(viewSelect);
		if (DEBUG) {
			Log.d(LOG_TAG, strBuilder.toString());
		}
		db.execSQL("CREATE VIEW " + AppTypeInfo.AssocViewColumns.VIEW_NAME
				+ " AS " + viewSelect);

		loadDefaultTypeNameDb(db);

		if (DEBUG) {
			Log.d(LOG_TAG, "DatabaseHelper onCreate() end");
		}
	}

	private void loadDefaultTypeNameDb(SQLiteDatabase db) {
		ApplicationTypeInfo typeInfo = new ApplicationTypeInfo();
		List<String> listTypeName = getTypeNameIdConfig();
		int size = listTypeName.size();
		ContentValues values = new ContentValues();
		for (int i = 0; i < size; i++) {
			values.clear();
			values.put(TypeColumns.TYPE_NAME, listTypeName.get(i));
			values.put(TypeColumns.IS_USER_DEFINED, false);
			db.insert(AppTypeInfo.TypeColumns.TABLE_NAME, null, values);
		}

	}

	private List<String> getTypeNameIdConfig() {
		List<String> data = new ArrayList<String>();
		try {
			XmlResourceParser xrp = mContext.getResources().getXml(
					R.xml.default_app_type);
			while (xrp.next() != XmlResourceParser.START_TAG) {
				continue;
			}
			xrp.next();
			while (xrp.getEventType() != XmlResourceParser.END_TAG) {
				while (xrp.getEventType() != XmlResourceParser.START_TAG) {
					if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
						xrp.close();
						return data;
					}
					xrp.next();
				}
				if (xrp.getName().equals(XML_APP_TYPE)) {
					String typeId = xrp.getAttributeValue(null,
							XML_ATTR_TYPE_ID);
					data.add(typeId);
				}
				while (xrp.getEventType() != XmlResourceParser.END_TAG) {
					xrp.next();
				}
				xrp.next();
			}
			xrp.close();
		} catch (XmlPullParserException e) {
			Log.d(LOG_TAG, "Ill-formatted default_app_type.xml file.");
		} catch (java.io.IOException e) {
			Log.d(LOG_TAG, "Unable to read timezones.xml file.");
		}
		return data;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + AppTypeInfo.AppColumns.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + AppTypeInfo.TypeColumns.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS "
				+ AppTypeInfo.AssocAppTypeColumns.TABLE_NAME);
		db.execSQL("DROP VIEW IF EXISTS "
				+ AppTypeInfo.AssocViewColumns.VIEW_NAME);
		onCreate(db);
	}

}
