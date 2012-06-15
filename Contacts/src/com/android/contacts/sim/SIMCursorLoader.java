package com.android.contacts.sim;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

public class SIMCursorLoader extends CursorLoader {

	Uri mUri ;
	Uri mSIMUri = Uri.parse("content://icc/adn");
	SimContacts mSimContacts;
	Cursor cursor = null;
	int[] Counts;
	String[] Titles;
	public SIMCursorLoader(Context context) {
		super(context);
	}

	private String getSortOrder(String[] projectionType) {
			return Contacts.SORT_KEY_PRIMARY;
	}
	
	@Override
	public Cursor loadInBackground() {
		String name;
		Cursor mCursor = getContext().getContentResolver().query(mSIMUri, null, null,
				null, null);
		if (mCursor != null) {
			mSimContacts = new SimContacts(mCursor, getSortOrder());
			cursor = mSimContacts.getSimContacts();
			mCursor.close();
        }
		if (cursor != null) {
			cursor.moveToFirst();
		}		
		
		return cursor;
		
	}
	@Override
	public void setUri(Uri uri) {
		super.setUri(uri);
	}
	
	public String [] getTitles(){
		return Titles;
	}
	
	public int [] getCounts(){
		return Counts;
	}
}
