package com.android.launcher2;

/**
 * added by zhongheng.zheng 2012.8.14 for app type utils
 * 
 */

import java.util.ArrayList;
import java.util.List;
import com.android.launcher.R;
import com.android.launcher2.AppTypeInfo.AssocViewColumns;
import com.android.launcher2.AppTypeInfo.TypeColumns;

import org.xmlpull.v1.XmlPullParserException;



import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AppTypeUitls {
	private Context mContext;

	public AppTypeUitls(Context context) {
		mContext = context;
	}
	
	
	
	public List<ApplicationAppInfo> getAllHideAppInfo() {
		List<ApplicationAppInfo> appInfoList = new ArrayList<ApplicationAppInfo>();
		
		String strIsVisible = "0";
		Cursor cursor = mContext.getContentResolver().query(
				AppTypeInfo.AppColumns.CONTENT_URI,
				null,
				AppTypeInfo.AppColumns.IS_VISIBILITY + "=?",
				new String[] {strIsVisible}, null);
		if (cursor == null || cursor.getCount() <= 0) {
			return appInfoList;
		}

		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			ApplicationAppInfo appInfo = new ApplicationAppInfo(cursor, mContext);
			appInfoList.add(appInfo);
		}
		cursor.close();

		return appInfoList;
	}

	
	public List<ApplicationTypeInfo> getAllTypeInfo() {
		List<ApplicationTypeInfo> typeInfoList = new ArrayList<ApplicationTypeInfo>();
		Cursor cursor = mContext.getContentResolver().query(
				AppTypeInfo.TypeColumns.CONTENT_URI,
				null, null, null, null);
		if (cursor == null || cursor.getCount() <= 0) {
			return typeInfoList;
		}

		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			ApplicationTypeInfo typeInfo = new ApplicationTypeInfo(cursor,
					mContext);
			typeInfoList.add(typeInfo);
		}
		cursor.close();

		return typeInfoList;
	}

	public List<ApplicationAppInfo> getAppInfoByType(ApplicationTypeInfo typeInfo) {
		int idType = typeInfo.mId;
		return getAppInfoByType(idType);
	}


	//get app info by type , and app is visible
	private List<ApplicationAppInfo> getAppInfoByType(int idType) {
		List<ApplicationAppInfo> appInfoList = new ArrayList<ApplicationAppInfo>();
		String strIdType = String.valueOf(idType);
//		Cursor cursor = mContext.getContentResolver().query(
//				AppTypeInfo.AssocViewColumns.CONTENT_URI,
//				null,
//				AppTypeInfo.AssocAppTypeColumns.ID_TYPE + "=?",
//				new String[] { strIdType }, null);
		String strIsVisible = "1";
		Cursor cursor = mContext.getContentResolver().query(
				AppTypeInfo.AssocViewColumns.CONTENT_URI,
				null,
				AppTypeInfo.AssocAppTypeColumns.ID_TYPE + "=?" + " AND "
						+ AppTypeInfo.AppColumns.IS_VISIBILITY + "=?",
				new String[] { strIdType, strIsVisible }, null);
		if (cursor == null || cursor.getCount() <= 0) {
			return appInfoList;
		}

		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			int idApp = cursor
					.getInt(AppTypeInfo.AssocViewColumns.APPCOLUMN_ID_APP_INDEX);
			String pkgClassName = cursor
					.getString(AppTypeInfo.AssocViewColumns.APPCOLUMN_PKG_CLASS_NAME_INEDX);
			boolean isVisibility = cursor
					.getInt(AppTypeInfo.AssocViewColumns.APPCOLUMN_IS_VISIBILITY_INEDX) == 1;
			ApplicationAppInfo appInfo = new ApplicationAppInfo(idApp,
					pkgClassName, isVisibility);
			appInfoList.add(appInfo);
		}
		cursor.close();

		return appInfoList;
	}
	
	
	public ApplicationTypeInfo addNewType(String typeName) {
		boolean isSameTypeName = isHaveSameTypeName(typeName);
		if(isSameTypeName){
			return null;
		}
		int idType = insertNewTypeToDb(typeName);
		ApplicationTypeInfo typeInfo = new ApplicationTypeInfo();
		typeInfo.mIsUserDefined = true;
		typeInfo.mTypeName = typeName;
		typeInfo.mId = idType;
		return typeInfo;
	}
	
	private int insertNewTypeToDb(String typeName){
		ApplicationTypeInfo typeInfo = new ApplicationTypeInfo();
		typeInfo.mIsUserDefined = true;
		typeInfo.mTypeName = typeName;
		ContentValues values = typeInfo.toContentValues();
		Uri uri = mContext.getContentResolver().insert(AppTypeInfo.TypeColumns.CONTENT_URI, values);	
		String lastPath = uri.getLastPathSegment();
		return Integer.parseInt(lastPath);
	}
	
	private boolean isHaveSameTypeName(String typeName){
		if(null == typeName){
			return false;
		}
		List<ApplicationTypeInfo> listType = getAllTypeInfo();
		int size = listType.size();
		int i = 0;
		for(i=0;i<size;i++){
			if(typeName.equals(listType.get(i).mTypeName)){
				return true;
			}
		}
		return false;

//		Cursor cursor = mContext.getContentResolver().query(
//				AppTypeInfo.TypeColumns.CONTENT_URI,
//				new String[]{AppTypeInfo.TypeColumns.TYPE_NAME},
//				AppTypeInfo.TypeColumns.TYPE_NAME + "=?",
//				new String[] { typeName }, null);
//		if(null == cursor || cursor.getCount() <= 0){
//			return false;
//		}
//		cursor.close();
//		return true;
	}

	
	public boolean renameTypeName(ApplicationTypeInfo typeInfo, String toTypeName){
		int idType = typeInfo.mId;
		return renameTypeName(idType,toTypeName);
	}
	
	private boolean renameTypeName(int idType, String toTypeName){
		boolean isSameTypeName = isHaveSameTypeName(toTypeName);
		if(isSameTypeName){
			return false;
		}
		String strIdType = String.valueOf(idType);
		Cursor cursor = mContext.getContentResolver().query(
				AppTypeInfo.TypeColumns.CONTENT_URI,
				null,
				AppTypeInfo.TypeColumns._ID + "=?",
				new String[] { strIdType }, null);
		if(null == cursor || cursor.getCount() <= 0){
			return false;
		}
		cursor.moveToFirst();
		boolean isUserDefind = cursor.getInt(AppTypeInfo.TypeColumns.IS_USER_DEFINED_INEDX) == 1;
		if(!isUserDefind){
			cursor.close();
			return false;
		}
		cursor.close();
		
		ApplicationTypeInfo typeInfo = new ApplicationTypeInfo();
		typeInfo.mIsUserDefined = true;
		typeInfo.mTypeName = toTypeName;
		typeInfo.mId = idType;
		ContentValues values = typeInfo.toContentValues();
		mContext.getContentResolver().update(AppTypeInfo.TypeColumns.CONTENT_URI, values, AppTypeInfo.TypeColumns._ID + "=?",
				new String[] { strIdType });
		return true;
	}
	
	
	
	public void deleteType(ApplicationTypeInfo typeInfo) {
		int idType = typeInfo.mId;
		deleteType(idType);
	}
	 
	 
	private void deleteType(int idType){
		String strIdType = String.valueOf(idType);
		mContext.getContentResolver().delete(AppTypeInfo.TypeColumns.CONTENT_URI, AppTypeInfo.TypeColumns._ID + "=?",
				new String[] { strIdType });
		mContext.getContentResolver().delete(AppTypeInfo.AssocAppTypeColumns.CONTENT_URI, AppTypeInfo.AssocAppTypeColumns.ID_TYPE + "=?",
				new String[] { strIdType });
	}
	
	
	
	public void editType( List<ApplicationAppInfo> newListApp, ApplicationTypeInfo typeInfo){
		int idType = typeInfo.mId;
		editType(newListApp,idType);
	}
	
	
	private void editType( List<ApplicationAppInfo> newListApp, int idType){
		List<ApplicationAppInfo> oldListApp = getAppInfoByType(idType);
		//get add app and add
		List<ApplicationAppInfo> addApp = getAddApp(oldListApp,newListApp);
		addAppByType(addApp,idType);
		
		//get delete app and delete
		List<ApplicationAppInfo> deleteApp = getDeleteApp(oldListApp,newListApp);
		deleteAppByType(deleteApp,idType);
	}
	
	
//	public void editType(List<ApplicationAppInfo> oldListApp, List<ApplicationAppInfo> newListApp, int idType){
//		//get add app and add
//		List<ApplicationAppInfo> addApp = getAddApp(oldListApp,newListApp);
//		addAppByType(addApp,idType);
//		
//		//get delete app and delete
//		List<ApplicationAppInfo> deleteApp = getDeleteApp(oldListApp,newListApp);
//		deleteAppByType(deleteApp,idType);
//	}
	
	private void deleteAppByType(List<ApplicationAppInfo> deleteApp, int idType) {
		int size = deleteApp.size();
		String strIdType = String.valueOf(idType);
		String strIdApp;
		int idApp;
		for (int i = 0; i < size; i++) {
			idApp = deleteApp.get(i).mId;
			strIdApp = String.valueOf(idApp);
			mContext.getContentResolver().delete(
					AppTypeInfo.AssocAppTypeColumns.CONTENT_URI,
					AppTypeInfo.AssocAppTypeColumns.ID_TYPE + "=?" + " AND "
							+ AppTypeInfo.AssocAppTypeColumns.ID_APP + "=?",
					new String[] { strIdType, strIdApp });
		}
	}
	
	private void addAppByType(List<ApplicationAppInfo> addApp,int idType){
		int size = addApp.size();
		int i = 0;
		boolean inAppTable;
		ApplicationAppInfo app;
		int idApp;
		for(i=0;i<size;i++){
			app = addApp.get(i);
			inAppTable = isInAppTable(app);
			if(!inAppTable){
				idApp = insertNewAppToDb(app);
				insertNewAssocTpDb(idApp,idType);
			}else{
				idApp = app.mId;
				insertNewAssocTpDb(idApp,idType);
			}
		}
	}
	
	private int insertNewAppToDb(ApplicationAppInfo appInfo) {
		appInfo.mId = 0;
		ContentValues values = appInfo.toContentValues();
		Uri uri = mContext.getContentResolver().insert(
				AppTypeInfo.AppColumns.CONTENT_URI, values);
		String lastPath = uri.getLastPathSegment();
		return Integer.parseInt(lastPath);
	}
	
	private int insertNewAssocTpDb(int idApp,int idType){
		ApplicationAssocAppTypeInfo assoc = new ApplicationAssocAppTypeInfo(idApp,idType);
		ContentValues values = assoc.toContentValues();
		Uri uri = mContext.getContentResolver().insert(
				AppTypeInfo.AssocAppTypeColumns.CONTENT_URI, values);
		String lastPath = uri.getLastPathSegment();
		return Integer.parseInt(lastPath);
	}
	
	private boolean isInAppTable(ApplicationAppInfo app){
		String pkgClass = app.mPkgClassName;
		Cursor cursor = mContext.getContentResolver().query(
				AppTypeInfo.AppColumns.CONTENT_URI,
				null,
				AppTypeInfo.AppColumns.PKG_CLASS_NAME + "=?",
				new String[] { pkgClass}, null);
		if (cursor == null || cursor.getCount() <= 0) {
			return false;
		}
		return true;
	}
	
	
	
	
	private List<ApplicationAppInfo> getAddApp(List<ApplicationAppInfo> oldListApp, List<ApplicationAppInfo> newListApp){
		List<ApplicationAppInfo> appInfoList = new ArrayList<ApplicationAppInfo>();
		int i = 0;
		int j = 0;
		int oldSize = oldListApp.size();
		int newSize = newListApp.size();
		ApplicationAppInfo newApp;
		for(i=0;i<newSize;i++){
			newApp = newListApp.get(i);
			for(j=0;j<oldSize;j++){
				if(newApp.mId == oldListApp.get(j).mId){
					break;
				}
			}
			if(j>=oldSize){
				appInfoList.add(newApp);
			}
		}
		
		return appInfoList;
	}
	
	private List<ApplicationAppInfo> getDeleteApp(List<ApplicationAppInfo> oldListApp, List<ApplicationAppInfo> newListApp){
		List<ApplicationAppInfo> appInfoList = new ArrayList<ApplicationAppInfo>();
		int i = 0;
		int j = 0;
		int oldSize = oldListApp.size();
		int newSize = newListApp.size();
		ApplicationAppInfo oldApp;
		for(i=0;i<oldSize;i++){
			oldApp = oldListApp.get(i);
			for(j=0;j<newSize;j++){
				if(oldApp.mId == newListApp.get(j).mId){
					break;
				}
			}
			if(j>=oldSize){
				appInfoList.add(oldApp);
			}
		}
		
		return appInfoList;
	}
	
	
	
	public void editHide(List<ApplicationAppInfo> newListApp) {
		List<ApplicationAppInfo> oldListApp = getAllHideAppInfo();
		// get add app and add
		List<ApplicationAppInfo> addApp = getAddApp(oldListApp, newListApp);
		addAppHide(addApp);

		// get delete app and edit hide to visible
		List<ApplicationAppInfo> deleteApp = getDeleteApp(oldListApp,
				newListApp);
		editAppHideToVisible(deleteApp);
	}
	
//	public void editHide(List<ApplicationAppInfo> oldListApp, List<ApplicationAppInfo> newListApp){
//		//get add app and add
//		List<ApplicationAppInfo> addApp = getAddApp(oldListApp,newListApp);
//		addAppHide(addApp);
//		
//		//get delete app and edit hide to visible
//		List<ApplicationAppInfo> deleteApp = getDeleteApp(oldListApp,newListApp);
//		editAppHideToVisible(deleteApp);
//	}
	
	private void addAppHide(List<ApplicationAppInfo> addApp){
		int size = addApp.size();
		int i = 0;
		boolean inAppTable;
		ApplicationAppInfo app;
		int idApp;
		for(i=0;i<size;i++){
			app = addApp.get(i);
			inAppTable = isInAppTable(app);
			app.mIsVisibility = false;
			if(!inAppTable){
				idApp = insertNewAppToDb(app);
			}else{
				idApp = app.mId;
				ContentValues values = app.toContentValues();
				String strIdApp = String.valueOf(idApp);
				mContext.getContentResolver().update(AppTypeInfo.AppColumns.CONTENT_URI, values, AppTypeInfo.AppColumns._ID + "=?",
						new String[] { strIdApp });
			}
		}
	}
	
	private void editAppHideToVisible(List<ApplicationAppInfo> toApp) {
		int size = toApp.size();
		ApplicationAppInfo app;
		String strIdApp;
		int idApp;
		for (int i = 0; i < size; i++) {
			app = toApp.get(i);
			app.mIsVisibility = true;
			strIdApp = String.valueOf(app.mId);
			ContentValues values = app.toContentValues();
			mContext.getContentResolver().update(AppTypeInfo.AppColumns.CONTENT_URI, values, AppTypeInfo.AppColumns._ID + "=?",
					new String[] { strIdApp });
		}
	}
	
	public void deleteDbForPackageRemoved(ComponentName component){
		ApplicationAppInfo appInfo = new ApplicationAppInfo();
		appInfo.mComponentName = component;
		appInfo.mPkgClassName = appInfo.componentNameToShortString(component);
		deleteDbForPackageRemoved(appInfo);
	}
	
	private void deleteDbForPackageRemoved(ApplicationAppInfo appInfo){
		String pkgClass = appInfo.mPkgClassName;
		Cursor cursor = mContext.getContentResolver().query(
				AppTypeInfo.AppColumns.CONTENT_URI,
				null,
				AppTypeInfo.AppColumns.PKG_CLASS_NAME + "=?",
				new String[] { pkgClass}, null);
		if (cursor == null || cursor.getCount() <= 0) {
			return;
		}
		cursor.moveToFirst();
		int idApp = cursor.getInt(AppTypeInfo.TypeColumns.ID_INDEX);
		if(idApp <= 0){
			cursor.close();
			return;
		}
		cursor.close();
		String strIdApp = String.valueOf(idApp);
		mContext.getContentResolver().delete(AppTypeInfo.AppColumns.CONTENT_URI, AppTypeInfo.AppColumns._ID + "=?",
				new String[] { strIdApp });
		mContext.getContentResolver().delete(AppTypeInfo.AssocAppTypeColumns.CONTENT_URI, AppTypeInfo.AssocAppTypeColumns.ID_APP + "=?",
				new String[] { strIdApp });
	}
	
	
	/***********************test***********************************/
/*	private void testInsertType(Context context){
		ApplicationTypeInfo typeInfo = new ApplicationTypeInfo();
		typeInfo.mIsUserDefined = true;
		typeInfo.mTypeName = "12345";
		ContentValues values = typeInfo.toContentValues();
		Uri uri = context.getContentResolver().insert(AppTypeInfo.TypeColumns.CONTENT_URI, values);
		String lastPath = uri.getLastPathSegment();
	}
	
	private void testInsertApp(Context context){
		ApplicationAppInfo appInfo = new ApplicationAppInfo();
		String str[] = {"aaa","bbb","ccc","ddd"};
		ContentValues values;
		for(int i=0;i<4;i++){
			appInfo.mIsVisibility = true;
			appInfo.mPkgClassName = str[i];
			appInfo.mId = 0;
			values = appInfo.toContentValues();
			context.getContentResolver().insert(AppTypeInfo.AppColumns.CONTENT_URI, values);
		}
	}
	
	private void testInsertAssocAppType(Context context){
		ApplicationAssocAppTypeInfo assocAppTypeInfo = new ApplicationAssocAppTypeInfo();
		assocAppTypeInfo.mIdApp = 1;
		assocAppTypeInfo.mIdType = 2;
		ContentValues values = assocAppTypeInfo.toContentValues();
		Uri uri = context.getContentResolver().insert(AppTypeInfo.AssocAppTypeColumns.CONTENT_URI, values);
		String lastPath = uri.getLastPathSegment();
	}
	
	private void testInsert(Context context){
//		testInsertType(context);
		testInsertApp(context);
//		testInsertAssocAppType(context);
	}
	
	
	public void test(Context context){
		testInsert(context);
//		testDelete(context);
//		testUpdate(context);
	}
	
	private void testdeleteType(Context context){
		int idType = 1;
		String strIdType = String.valueOf(idType);
		context.getContentResolver().delete(AppTypeInfo.TypeColumns.CONTENT_URI, AppTypeInfo.TypeColumns._ID + "=?",
				new String[] { strIdType });///////////////////////??????????
		context.getContentResolver().delete(AppTypeInfo.AssocAppTypeColumns.CONTENT_URI, AppTypeInfo.AssocAppTypeColumns.ID_TYPE + "=?",
				new String[] { strIdType });///////////////////////??????????
	}
	

	private static void testdeleteApp(Context context){
		int idApp = 1;
		String strIdApp = String.valueOf(idApp);
		context.getContentResolver().delete(AppTypeInfo.AppColumns.CONTENT_URI, AppTypeInfo.AppColumns._ID + "=?",
				new String[] { strIdApp });///////////////////////??????????
		context.getContentResolver().delete(AppTypeInfo.AssocAppTypeColumns.CONTENT_URI, AppTypeInfo.AssocAppTypeColumns.ID_APP + "=?",
				new String[] { strIdApp });///////////////////////??????????
	}
	
	private void testDelete(Context context){
		testdeleteType(context);
		testdeleteApp(context);
	}
	
	private void testUpdate(Context context){
		testUpdateApp(context);
		testUpdateType(context);
	}
	
	private void testUpdateApp(Context context){
		ApplicationAppInfo appInfo = new ApplicationAppInfo();
		appInfo.mPkgClassName = "upApp";
		appInfo.mIsVisibility = false;
		int idApp = 1;
		appInfo.mId = idApp;
		ContentValues values = appInfo.toContentValues();
		String strIdApp = String.valueOf(idApp);
		context.getContentResolver().update(AppTypeInfo.AppColumns.CONTENT_URI, values, AppTypeInfo.AppColumns._ID + "=?",
				new String[] { strIdApp });
	}


	private void testUpdateType(Context context){
		ApplicationTypeInfo typeInfo = new ApplicationTypeInfo();
		typeInfo.mIsUserDefined = false;
		typeInfo.mTypeName = "uptype";
		int idType = 2;
		typeInfo.mId = idType;
		ContentValues values = typeInfo.toContentValues();
		String strIdType = String.valueOf(idType);
		context.getContentResolver().update(AppTypeInfo.TypeColumns.CONTENT_URI, values, AppTypeInfo.TypeColumns._ID + "=?",
				new String[] { strIdType });
	}
	*/
	/***********************test***********************************/
}
