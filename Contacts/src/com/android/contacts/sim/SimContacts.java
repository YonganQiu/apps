package com.android.contacts.sim;

import com.android.contacts.sim.HanziToPinyin.Token;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Contacts.People;
import android.provider.ContactsContract.ContactCounts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SimContacts {
	
	private int[] mCounts;
	private String[] mTitles;
	private List<SimContact> mList;
	
	public SimContacts(Cursor cursor, String sortOrder) {
		mList = new ArrayList<SimContact>();
		if (cursor != null) {
//			String id, name, number, accountType, sortKey, sortKeyAlt;
			String id, type, label, number, contactId, lookUp, photoId, name, sortKey, sortKeyAlt;
			cursor.moveToPosition(-1);
			while (cursor.moveToNext()) {
				id = cursor.getString(cursor.getColumnIndex(People._ID));
				type = null;
				label = null;
				number = cursor.getString(cursor.getColumnIndex(People.NUMBER));
				contactId = null;
				lookUp = null;
				photoId = null;
				name = cursor.getString(cursor.getColumnIndex(People.NAME));
				sortKey = getSortKey(name);
				sortKeyAlt = sortKey;
				mList.add(new SimContact(id, type, label, number, contactId, lookUp, photoId, name , sortKey, sortKeyAlt));
			}
			cursor.close();
		}

		if(!mList.isEmpty()) {
			Collections.sort(mList, new SortSimContacts(sortOrder));
			setTitlesAndCounts(sortOrder);
		}
	}
	
	public SimContacts(Context context, String sortOrder) {
		Cursor cursor = context
				.getContentResolver()
				.query(Uri
						.parse("content://icc/adn")
						.buildUpon()
						.appendQueryParameter(
								ContactCounts.ADDRESS_BOOK_INDEX_EXTRAS, "true")
						.build(), null, null, null, null);

		mList = new ArrayList<SimContact>();
		if (cursor != null) {
			String id, type, label, number, contactId, lookUp, photoId, name, sortKey, sortKeyAlt;
			while (cursor.moveToNext()) {
				id = cursor.getString(cursor.getColumnIndex(People._ID));
				type = "sim";
				label = "";
				number = cursor.getString(cursor.getColumnIndex(People.NUMBER));
				contactId = "";
				lookUp = "";
				photoId = "";
				name = cursor.getString(cursor.getColumnIndex(People.NAME));
				sortKey = getSortKey(name);
				sortKeyAlt = sortKey;

				mList.add(new SimContact(id, type, label, number, contactId, lookUp, photoId, name , sortKey, sortKeyAlt));
			}
			cursor.close();
		}

		if(!mList.isEmpty()) {
			Collections.sort(mList, new SortSimContacts(sortOrder));
			setTitlesAndCounts(sortOrder);
		}
	}
	
	public Cursor getSimContacts() {
//		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id",
//				"name", "number", "account_type", "sort_key", "sort_key_alt"},
//				mList.size());
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id",
				"data2", "data3", "data1", "contact_id", "lookup" , "photo_id" , "display_name"},
				mList.size());

		SimContact simContact;
		int size = mList.size();
		for (int i = 0; i < size; i++) {
			simContact = mList.get(i);
			matrixCursor.addRow(new String[] { simContact.mId,
					simContact.mType, simContact.mLabel,
					simContact.mNumber, simContact.mContactId,
					simContact.mLookUp ,simContact.mPhotoId , simContact.mName});
			
		}

		return matrixCursor;
	}
	
	public int[] getCounts() {
		return mCounts;
	}

	public String[] getTitles() {
		return mTitles;
	}

	private void setTitlesAndCounts(String sortOrder) {
		String key = null;
		Integer value;
		SimContact simContact;
		int size = mList.size();
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		boolean isPrimary = Contacts.SORT_KEY_PRIMARY.equals(sortOrder);
		
		for (int i = 0; i < size; i++) {
			simContact = mList.get(i);

			if (!TextUtils.isEmpty(isPrimary ? simContact.mSortKey
					: simContact.mSortKeyAlt)) {
				key = isPrimary ? simContact.mSortKey.substring(0, 1)
						: simContact.mSortKeyAlt.substring(0, 1);
			}
			
			if(key != null && key.matches("[a-zA-Z]{1}")) {
				key = key.toUpperCase();
			} else {
				key = "#";
			}

			value = map.get(key);
			value = value == null ? 1 : value.intValue() + 1;

			map.put(key, value);
		}

		mTitles = new String[map.size()];
		map.keySet().toArray(mTitles);
		Arrays.sort(mTitles);

		mCounts = new int[mTitles.length];
		for (int i = 0; i < mTitles.length; i++) {
			mCounts[i] = map.get(mTitles[i]).intValue();
		}
		map.clear();
	}

	private String getSortKey(String name) {
		ArrayList<Token> tokens = HanziToPinyin.getInstance().get(name);
		if (tokens != null && tokens.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (Token token : tokens) {
				if (Token.PINYIN == token.type) {
					if (sb.length() > 0) {
						sb.append(' ');
					}
					sb.append(token.target);
					sb.append(' ');
					sb.append(token.source);
				} else {
					if (sb.length() > 0) {
						sb.append(' ');
					}
					sb.append(token.source);
				}
			}
			return sb.toString().trim();
		}
		return name.trim();
	}

    
	public static class SimContact {
		public final String mId;
		public final String mType;
		public final String mLabel;
		public final String mNumber;
		public final String mContactId;
		public final String mLookUp;
		public final String mPhotoId;
		public final String mName;
		public final String mSortKey;
		public final String mSortKeyAlt;
		
		public SimContact(String id, String type, String label, String number,
				String contactId, String lookUp, String photoId, String name,
				String sortKey, String sortKeyAlt) {
			mId = id;
			mType = type;
			mLabel = label;
			mNumber = number;
			mContactId = contactId;
			mLookUp = lookUp;
			mPhotoId = photoId;
			mName = name;
			mSortKey = sortKey;
			mSortKeyAlt = sortKeyAlt;
		}
	}

	private class SortSimContacts implements Comparator<SimContact> {

		private String mSortOrder;

		public SortSimContacts(String sortOrder) {
			mSortOrder = sortOrder;
		}

		public int compare(SimContact contact1, SimContact contact2) {
			int result = 0;
			if (contact1 != null && contact2 != null) {
				String str1, str2;
				if (Contacts.SORT_KEY_PRIMARY.equals(mSortOrder)) {
					str1 = contact1.mSortKey;
					str2 = contact2.mSortKey;
				} else {
					str1 = contact1.mSortKeyAlt;
					str2 = contact2.mSortKeyAlt;
				}
				if (str1 == null) {
					str1 = "";
				}
				if (str2 == null) {
					str2 = "";
				}
				result = str1.compareToIgnoreCase(str2);
				if (str1.length() > 0 && str2.length() > 0) {
					boolean result1 = str1.substring(0, 1).matches("[a-zA-Z]");
					boolean result2 = str2.substring(0, 1).matches("[a-zA-Z]");
					if (result1 != result2) {
						if (result > 0) {
							if (!result1 && result2) {
								result = -result;
							}
						} else if (result < 0) {
							if (result1 && !result2) {
								result = -result;
							}
						}
					}
				}
			}
			return result;
		}
	}
}