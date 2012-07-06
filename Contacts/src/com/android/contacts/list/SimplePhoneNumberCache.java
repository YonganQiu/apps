package com.android.contacts.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.contacts.list.SimplePhoneNumberListAdapter.PhoneQuery;

import android.database.Cursor;
import android.media.HanziToPinyin;
import android.media.HanziToPinyin.Token;
import android.text.TextUtils;
import android.util.Log;

public class SimplePhoneNumberCache {
	
	private static final String TAG = SimplePhoneNumberCache.class.getSimpleName();
	
	private ArrayList<PhoneNumberRefInfo> mPhoneNumberRefInfos = new ArrayList<PhoneNumberRefInfo>();
	private HashMap<Long, String> mResultCache = new HashMap<Long, String>();

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
	
	public HashMap<Long, String> search(ArrayList<PhoneNumberRefInfo> infos, String key) {
		HashMap<Long, String> result = mResultCache;
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
		String keyWithoutBlank = removeAll(key, ' ');
		//String dightsPatternString = createDightsPatternFromKey(key);
		Log.i(TAG, "search: letters pattern is " + lettersPatternString);
		//Log.i(TAG, "search: dights pattern is " + dightsPatternString);
		
		Pattern lettersPattern = Pattern.compile(lettersPatternString);
		Matcher lettersMatcher = lettersPattern.matcher("");
		//Pattern dightsPattern = Pattern.compile(dightsPatternString);
		//Matcher dightsMatcher = dightsPattern.matcher("");
		String group;
		for (PhoneNumberRefInfo info : infos) {
			Log.i(TAG, "search: handling " + info.mSortKey + ", number " + info.mNumber + ", id " + info.mDataId);
			lettersMatcher.reset(toPinYin(info.mSortKey));
			if (lettersMatcher.find()) {
				group = lettersMatcher.group();
				Log.i(TAG, "matcher string: " + group);
				result.put(info.mDataId, group);
			} else if (onlyDightsAndPlus(info.mNumber).contains(keyWithoutBlank)) {
				Log.i(TAG, "matcher string: " + keyWithoutBlank);
				result.put(info.mDataId, keyWithoutBlank);
			}
		}
		
		long end = System.currentTimeMillis();
		Log.i(TAG, "cost time-------" + (end - start) + "ms, search result-------" + result.size());
		return result;
	}
	
    HanziToPinyin mHanziToPinyin;
    private String toPinYin(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }
        if (mHanziToPinyin == null) {
            mHanziToPinyin = HanziToPinyin.getInstance();
        }
        ArrayList<Token> tokens = mHanziToPinyin.get(input);
        StringBuilder output = new StringBuilder();
        if (tokens != null && tokens.size() > 0) {
            for (Token token : tokens) {
                output.append(token.target.toLowerCase()).append(' ');
            }
        }
        Log.i(TAG, "toPinYin(): input(" + input + "), output(" + output.toString() + ")");
        return output.toString();
    }

	public String createLetterPatternFromKey(String key) {
		StringBuilder builder = new StringBuilder();
		//remove all blank ' '.
		key = removeAll(key, ' ');
		//keep the first '+'.
		if (key.startsWith("+")) {
			builder.append("\\+");
		}

		char[] s = key.toLowerCase().toCharArray();
		for (int i = 0; i < s.length; i++) {
			char c = s[i];
			if (c >= '0' && c <= '9') {
				if (i == 0) {
					builder.append("\\b");
				} else {
					builder.append("([a-z0-9]*[^a-z0-9]*\\s+)?");
				}
				builder.append('[').append(NUMBER_TO_LETTER[c - '0']).append(']');
			} else if ("*#".indexOf(c) >= 0) {
				if (i == 0) {
					builder.append("\\b");
				} else {
					builder.append("([a-z0-9]*[^a-z0-9]*\\s+)?");
				}
				builder.append("\\").append(c);
			}
		}
		//builder.append("([a-z0-9]*[^a-z0-9]*)?");
		return builder.toString();
	}
	
	public String createDightsPatternFromKey(String key) {
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
	
	public static String removeAll(String from, char charToRemove) {
		int i = 0;
		int j = 0;
		char[] s = new char[from.length()];
		while (i < from.length()) {
			if (from.charAt(i) != charToRemove) {
				s[j++] = from.charAt(i);
			}
			i++;
		}
		return new String(s, 0, j);
	}

	public static String onlyDightsAndPlus(String oriNumber) {
		int i = 0;
		int j = 0;
		char[] s = new char[oriNumber.length()];
		char c;
		while (i < oriNumber.length()) {
			c = oriNumber.charAt(i);
			if (c >= '0' && c <= '9' || c == '+') {
				s[j++] = oriNumber.charAt(i);
			}
			i++;
		}
		return new String(s, 0, j);
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
			addInCache(cursor.getString(PhoneQuery.PHONE_DISPLAY_NAME).toLowerCase(), 
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
