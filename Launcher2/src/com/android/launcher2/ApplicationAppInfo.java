package com.android.launcher2;

/**
 * added by zhongheng.zheng 2012.8.14 for app info
 * 
 */

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.android.launcher2.AppTypeInfo.AppColumns;

public class ApplicationAppInfo {
	public int mId;
	public String mPkgClassName;
	public boolean mIsVisibility;
	public ComponentName mComponentName;

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		if (mId != 0) {
			values.put(AppColumns._ID, mId);
		}
		values.put(AppColumns.PKG_CLASS_NAME, mPkgClassName);
		values.put(AppColumns.IS_VISIBILITY, mIsVisibility);
		return values;
	}
	
	public ApplicationAppInfo(Cursor c, Context context){
		mId = c.getInt(AppColumns.ID_INDEX);
		mPkgClassName = c.getString(AppColumns.PKG_CLASS_NAME_INDEX);
		mIsVisibility = c.getInt(AppColumns.IS_VISIBILITY_INEDX) == 1;
		mComponentName = toComponentName(mPkgClassName);
	}
	
	public ApplicationAppInfo(){
	}
	
	public ApplicationAppInfo(String pkgClassName, boolean isVisibility){
		mPkgClassName = pkgClassName;
		mIsVisibility = isVisibility;
		mComponentName = toComponentName(mPkgClassName);
	}
	
	public ApplicationAppInfo(int id, String pkgClassName, boolean isVisibility){
		mId = id;
		mPkgClassName = pkgClassName;
		mIsVisibility = isVisibility;
		mComponentName = toComponentName(mPkgClassName);
	}


	
	@Override
	public String toString() {
		return "ApplicationAppInfo [mId=" + mId + ", mPkgClassName="
				+ mPkgClassName + ", mIsVisibility=" + mIsVisibility
				+ ", mComponentName=" + mComponentName + "]";
	}

	
	
	public ComponentName toComponentName(String pkgClassName) {
		int length = pkgClassName.length();
		int sep = pkgClassName.indexOf('/');
		if (sep < 0 || (sep + 1) >= length) {
			return null;
		}
		String pkg = pkgClassName.substring(1, sep);
		String cls = pkgClassName.substring(sep + 1, length-1);
		if (cls.length() > 0 && cls.charAt(0) == '.') {
			cls = pkg + cls;
		}
		return new ComponentName(pkg, cls);
	}

	public String componentNameToShortString(ComponentName componentName) {
		return componentName.toShortString();
	}
}
