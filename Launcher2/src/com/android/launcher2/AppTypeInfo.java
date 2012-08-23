package com.android.launcher2;

/**
 * added by zhongheng.zheng 2012.8.14 for app type info
 * 
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class AppTypeInfo {

	public static final String AUTHORITY = "com.android.launcher.apptype";
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.apptype";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.apptype";

	public static class AppColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/app");
		public static final String TABLE_NAME = "app";
		public static final String PKG_CLASS_NAME = "pkg_class_name";
		public static final String IS_VISIBILITY = "is_visibility";
		
		public static final int ID_INDEX = 0;
		public static final int PKG_CLASS_NAME_INDEX = 1;
		public static final int IS_VISIBILITY_INEDX = 2;
	}

	public static class TypeColumns implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/type");
		public static final String TABLE_NAME = "type";
		public static final String TYPE_NAME = "type_name";
		public static final String IS_USER_DEFINED = "is_user_defined";
		
		public static final int ID_INDEX = 0;
		public static final int TYPE_NAME_INDEX = 1;
		public static final int IS_USER_DEFINED_INEDX = 2;
	}

	public static class AssocAppTypeColumns implements BaseColumns {

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/assocapptype");
		public static final String TABLE_NAME = "assocapptype";
		public static final String ID_APP = "id_app";
		public static final String ID_TYPE = "id_type";
		
		public static final int ID_INDEX = 0;
		public static final int ID_APP_INDEX = 1;
		public static final int ID_TYPE_INEDX = 2;
	}
	
	public static class AssocViewColumns  {

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/assocview");
		public static final String VIEW_NAME = "assocview";

		public static final int APPCOLUMN_ID_APP_INDEX = 0;
		public static final int APPCOLUMN_PKG_CLASS_NAME_INEDX = 1;
		public static final int APPCOLUMN_IS_VISIBILITY_INEDX = 2;
		public static final int TYPECOLUMN_ID_INDEX = 3;
		public static final int TYPECOLUMN_TYPENAME_INEDX = 4;
		public static final int TYPECOLUMN_IS_USER_DEFINED_INEDX = 5;
		public static final int ASSOCAPPTYPECOLUMN_ID_INDEX = 6;
		public static final int ASSOCAPPTYPECOLUMN_ID_APP_INEDX = 7;
		public static final int ASSOCAPPTYPECOLUMN_ID_TYPE_INEDX = 8;
		
	}

}
