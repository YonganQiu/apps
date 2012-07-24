package com.android.contacts.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.contacts.list.FilteredPhoneNumberAdapter.PhoneQuery;
import com.android.contacts.util.SimpleHanziToPinyin;
import com.android.contacts.util.SimpleHanziToPinyin.Token;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

public class PatternAndResultHelper {
	
	private static final String TAG = PatternAndResultHelper.class.getSimpleName();
	
	private HashMap<Long, PhoneNumberRefInfo> mPhoneNumberRefInfos = new HashMap<Long, PhoneNumberRefInfo>();
	private HashMap<Long, PhoneNumberRefInfo> mOldInfos;
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
	public PatternAndResultHelper() {
	}

	public HashMap<Long, PhoneNumberRefInfo> getCachedInfos() {
		synchronized (mLock) {
			return mPhoneNumberRefInfos;
		}
	}
	
	public HashMap<Long, String> search(HashMap<Long, PhoneNumberRefInfo> infos, String key) {
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
		Log.i(TAG, "search: letters pattern is " + lettersPatternString);
		
		Pattern lettersPattern = Pattern.compile(lettersPatternString);
		Matcher lettersMatcher = lettersPattern.matcher("");
		String group;
		for (PhoneNumberRefInfo info : infos.values()) {
			//Log.i(TAG, "search: handling " + info.mLowerCasePinYinOfDisplayName + ", number " + info.mNumber + ", id " + info.mDataId);
			lettersMatcher.reset(info.mLowerCasePinYinOfDisplayName);
			if (lettersMatcher.find()) {
				group = lettersMatcher.group();
				//Log.i(TAG, "matcher string: " + group);
				result.put(info.mDataId, group);
			} else if (onlyDightsAndPlus(info.mNumber).contains(keyWithoutBlank)) {
				//Log.i(TAG, "matcher string: " + keyWithoutBlank);
				result.put(info.mDataId, keyWithoutBlank);
			}
		}
		
		long end = System.currentTimeMillis();
		Log.i(TAG, "cost time-------" + (end - start) + "ms, search result-------" + result.size());
		return result;
	}
	
    SimpleHanziToPinyin mHanziToPinyin;
    private String toLowCasePinYin(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }
        if (mHanziToPinyin == null) {
            mHanziToPinyin = SimpleHanziToPinyin.getInstance();
        }
        ArrayList<Token> tokens = mHanziToPinyin.get(input);
        StringBuilder output = new StringBuilder();
        if (tokens != null && tokens.size() > 0) {
            for (Token token : tokens) {
                output.append(token.target).append(' ');
            }
        }
        //Log.i(TAG, "toPinYin(): input(" + input + "), output(" + output.toString() + ")");
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

	public void addInCache(PhoneNumberRefInfo info) {
		mPhoneNumberRefInfos.put(info.mDataId, info);
	}

    public void addInCache(String lowerCasePinYinOfDisplayName, String number, long dataId, boolean useCache) {
        Long id = dataId;
        if (useCache && mOldInfos != null && mOldInfos.containsKey(id)) {
            mPhoneNumberRefInfos.put(id, mOldInfos.get(id));
        } else {
            mPhoneNumberRefInfos.put(dataId, new PhoneNumberRefInfo(lowerCasePinYinOfDisplayName, number, dataId));
        }
    }
	
	private void swapCache() {
		synchronized (mLock) {
		    mOldInfos = mPhoneNumberRefInfos;
			mPhoneNumberRefInfos = new HashMap<Long, PhoneNumberRefInfo>();
		}
	}
	
	public void loadPhoneNumberRefInfos(Cursor cursor, boolean withCache) {
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
			addInCache(toLowCasePinYin(cursor.getString(PhoneQuery.PHONE_DISPLAY_NAME)), 
					cursor.getString(PhoneQuery.PHONE_NUMBER), 
					cursor.getLong(PhoneQuery.PHONE_ID), withCache);
		} while (cursor.moveToNext());
	}

	class PhoneNumberRefInfo {
		private String mLowerCasePinYinOfDisplayName;
		private String mNumber;
		private long mDataId;
		public PhoneNumberRefInfo(String lowerCasePinYinOfDisplayName, String number, long dataId) {
			mLowerCasePinYinOfDisplayName = lowerCasePinYinOfDisplayName;
			mNumber = number;
			mDataId = dataId;
		}
	}
}
