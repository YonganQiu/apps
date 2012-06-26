package com.android.contacts.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.contacts.list.SimplePhoneNumberListAdapter.PhoneQuery;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

public class SimplePhoneNumberCache {
	
	private static final String TAG = SimplePhoneNumberCache.class.getSimpleName();
	
	private ArrayList<PhoneNumberRefInfo> mPhoneNumberRefInfos = 
			new ArrayList<PhoneNumberRefInfo>();
	private ArrayList<Long> mResultCache = 
			new ArrayList<Long>();

	private Object mLock = new Object();
	
	private static final String[] NUMBER_TO_LETTER= {
		"0",
		"1",
		"2abc",
		"3def",
		"4ghi",
		"5jkl",
		"6mno",
		"7pqrs",
		"8tuv",
		"9wxyz"
	};
	public SimplePhoneNumberCache() {
	}

	public ArrayList<PhoneNumberRefInfo> getCachedInfos() {
		synchronized (mLock) {
			return mPhoneNumberRefInfos;
		}
	}
	
	public ArrayList<Long> search(ArrayList<PhoneNumberRefInfo> infos, String key) {
		ArrayList<Long> result = mResultCache;
		result.clear();
		
		if (TextUtils.isEmpty(key)) {
			Log.w(TAG, "key is null.");
			return result;
		}
		
		if (infos == null || infos.size() <= 0) {
			Log.w(TAG, "cache infos is null or 0 size.");
			return result;
		}

		key = key.toLowerCase();
		
		long start = System.currentTimeMillis();
		
		//setup pattern.
		String lettersPatternString = createLetterPatternFromKey(key);
		String dightsPatternString = createDightsPatternFromKey(key);
		Log.i(TAG, "search: letters pattern is " + lettersPatternString);
		Log.i(TAG, "search: dights pattern is " + dightsPatternString);
		
		Pattern lettersPattern = Pattern.compile(lettersPatternString);
		Matcher lettersMatcher = lettersPattern.matcher("");
		Pattern dightsPattern = Pattern.compile(dightsPatternString);
		Matcher dightsMatcher = dightsPattern.matcher("");
		boolean found;
		for (PhoneNumberRefInfo info : infos) {
			// TODO
			if (info.mDataId == 105) continue;
			
			Log.i(TAG, "search: handling " + info.mSortKey + ", number " + info.mNumber + ", id " + info.mDataId);
			lettersMatcher.reset(info.mSortKey);
			found = lettersMatcher.find();
			if (!found) {
				found = dightsMatcher.reset(info.mNumber).find();
			}
			if (found) {
				result.add(info.mDataId);
			}
		}
		
		long end = System.currentTimeMillis();
		Log.i(TAG, "cost time-------" + (end - start) + "ms, search result-------" + result.size());
		return result;
	}
	
	private String createLetterPatternFromKey(String key) {
		StringBuilder builder = new StringBuilder(".*");
		char[] s = key.toLowerCase().toCharArray();
		for (char c : s) {
			if (c >= '0' && c <= '9') { //just number is legal.
				builder.append('[').append(NUMBER_TO_LETTER[c - '0']).append("].*");
			}
		}
		//builder.append(".*");
		return builder.toString();
	}
	
	private String createDightsPatternFromKey(String key) {
		StringBuilder builder = new StringBuilder(".*");
		char[] s = key.toLowerCase().toCharArray();
		for (char c : s) {
			if (c >= '0' && c <= '9') { //just number is legal.
				builder.append(c - '0').append("[\\- ]*");
			}
		}
		builder.append(".*");
		return builder.toString();
	}
	
/*	public void reset() {
		if (mPhoneNumberRefInfos.size() > 0) {
			mPhoneNumberRefInfos.clear();
		}
	}
*/	
	public void addInCache(PhoneNumberRefInfo info) {
		mPhoneNumberRefInfos.add(info);
	}
	
	public void addInCache(String sortKey, String number, long dataId) {
		//Log.i(TAG, "sortkey " + sortKey + ", number " + number + ", id " + contactId);
		mPhoneNumberRefInfos.add(new PhoneNumberRefInfo(sortKey, number, dataId));
	}
	
	private void swapCache() {
		synchronized (mLock) {
			mPhoneNumberRefInfos = new ArrayList<PhoneNumberRefInfo>();
		}
	}
	
	public void loadPhoneNumberRefInfos(Cursor cursor) {
		//reset();
		swapCache();
		
		if (cursor == null) {
			Log.e(TAG, "Cursor can not be null!");
			return;
		}
		if (!cursor.moveToFirst()) {
			Log.w(TAG, "Cursor is empty!");
			return;
		}
		do {
			addInCache(cursor.getString(PhoneQuery.PHONE_SORT_KEY).toLowerCase(), 
					cursor.getString(PhoneQuery.PHONE_NUMBER), 
					cursor.getLong(PhoneQuery.PHONE_ID));
		} while (cursor.moveToNext());
	}
	
/*	public void sort() {
		PhoneNumberRefInfo[] infos = new PhoneNumberRefInfo[mPhoneNumberRefInfos.size()];
		mPhoneNumberRefInfos.toArray(infos);
		Arrays.sort(infos, new SortComparator());
	}

	class SortComparator implements Comparator<PhoneNumberRefInfo> {
		@Override
		public int compare(PhoneNumberRefInfo lhs, PhoneNumberRefInfo rhs) {
			return lhs.mSortKey.compareToIgnoreCase(rhs.mSortKey);
		}
	}
*/	
	class PhoneNumberRefInfo {
		private String mSortKey;
		private String mNumber;
		private long mDataId;
		public PhoneNumberRefInfo(String sortKey, String number, long dataId) {
			mSortKey = sortKey;
			mNumber = number;
			mDataId = dataId;
		}
	}
}
