package com.android.launcher2;

/**
 * added by zhongheng.zheng 2012.8.14 for app type info
 * 
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.android.launcher.R;
import com.android.launcher2.AppTypeInfo.AppColumns;
import com.android.launcher2.AppTypeInfo.TypeColumns;

public class ApplicationTypeInfo {
	private static final String TAG = "ApplicationTypeInfo";
	public int mId;
	public String mTypeName;
	public boolean mIsUserDefined;

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if (mId != 0) {
			values.put(AppColumns._ID, mId);
		}
		values.put(TypeColumns.TYPE_NAME, mTypeName);
		values.put(TypeColumns.IS_USER_DEFINED, mIsUserDefined);
		return values;
	}
	
	public ApplicationTypeInfo(Cursor c, Context context){
		mId = c.getInt(TypeColumns.ID_INDEX);
		mIsUserDefined = c.getInt(TypeColumns.IS_USER_DEFINED_INEDX) == 1;
		String strName = c.getString(TypeColumns.TYPE_NAME_INDEX);
		if(mIsUserDefined){
			mTypeName = strName;
		}else{
			int nameResourceId = getStringResourceId(context,strName);
			mTypeName = context.getString(nameResourceId);
		}
		
	}
	
	public ApplicationTypeInfo(){
	}

	@Override
	public String toString() {
		return "ApplicationTypeInfo [mId=" + mId + ", mTypeName=" + mTypeName
				+ ", mIsUserDefined=" + mIsUserDefined + "]";
	}
	
	
	private int getStringResourceId(Context context, String resoureName) {
		int resourceId = -1;
		try {
			resourceId = R.string.class.getField(resoureName).getInt(null);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (SecurityException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (IllegalAccessException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (NoSuchFieldException e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		} catch (Exception e) {
			Log.e(TAG, "get string resource id is failed. resoureName:"
					+ resoureName, e);
			return resourceId;
		}
		return resourceId;
	}
	
	
}
