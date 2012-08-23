package com.android.launcher2;

/**
 * added by zhongheng.zheng 2012.8.14 for assoc app type info
 * 
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.android.launcher2.AppTypeInfo.AssocAppTypeColumns;

public class ApplicationAssocAppTypeInfo {
	public int mId;
	public int mIdApp;
	public int mIdType;

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if (mId != 0) {
			values.put(AssocAppTypeColumns._ID, mId);
		}
		values.put(AssocAppTypeColumns.ID_APP, mIdApp);
		values.put(AssocAppTypeColumns.ID_TYPE, mIdType);
		return values;
	}
	
	public ApplicationAssocAppTypeInfo(Cursor c, Context context){
		mId = c.getInt(AssocAppTypeColumns.ID_INDEX);
		mIdApp = c.getInt(AssocAppTypeColumns.ID_APP_INDEX);
		mIdType = c.getInt(AssocAppTypeColumns.ID_TYPE_INEDX);
	}
	
	public ApplicationAssocAppTypeInfo(int idApp, int idType){
		mIdApp = idApp;
		mIdType = idType;
		mId = 0;
	}
	
	public ApplicationAssocAppTypeInfo(){
	}

	@Override
	public String toString() {
		return "ApplicationAssocAppTypeInfo [mId=" + mId + ", mIdApp=" + mIdApp
				+ ", mIdType=" + mIdType + "]";
	}
}
