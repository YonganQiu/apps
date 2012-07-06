/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Color;
import android.media.HanziToPinyin;
import android.media.HanziToPinyin.Token;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.ContactCounts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.editor.AggregationSuggestionEngine.RawContact;
import com.android.contacts.format.PrefixHighlighter;
import com.android.contacts.list.SimplePhoneNumberListAdapter.OnItemActionListener;

import com.android.contacts.R;
/**
 * A cursor adapter for the {@link Phone#CONTENT_TYPE} content type.
 */
public class SimplePhoneNumberListAdapter extends CursorAdapter{
    private static final String TAG = SimplePhoneNumberListAdapter.class.getSimpleName();

    protected static class PhoneQuery {
        private static final String[] PROJECTION_PRIMARY = new String[] {
            Phone._ID,                          // 0
            Phone.TYPE,                         // 1
            Phone.LABEL,                        // 2
            Phone.NUMBER,                       // 3
            Phone.CONTACT_ID,                   // 4
            Phone.LOOKUP_KEY,                   // 5
            Phone.PHOTO_ID,                     // 6
            Phone.DISPLAY_NAME_PRIMARY,         // 7
            //{yongan.qiu
            Phone.SORT_KEY_PRIMARY,             // 8
            //}yongan.qiu
        };

        private static final String[] PROJECTION_ALTERNATIVE = new String[] {
            Phone._ID,                          // 0
            Phone.TYPE,                         // 1
            Phone.LABEL,                        // 2
            Phone.NUMBER,                       // 3
            Phone.CONTACT_ID,                   // 4
            Phone.LOOKUP_KEY,                   // 5
            Phone.PHOTO_ID,                     // 6
            Phone.DISPLAY_NAME_ALTERNATIVE,     // 7
            //{yongan.qiu
            Phone.SORT_KEY_ALTERNATIVE,         // 8
            //}yongan.qiu
        };

        public static final int PHONE_ID           = 0;
        public static final int PHONE_TYPE         = 1;
        public static final int PHONE_LABEL        = 2;
        public static final int PHONE_NUMBER       = 3;
        public static final int PHONE_CONTACT_ID   = 4;
        public static final int PHONE_LOOKUP_KEY   = 5;
        public static final int PHONE_PHOTO_ID     = 6;
        public static final int PHONE_DISPLAY_NAME = 7;
        //{yongan.qiu
        public static final int PHONE_SORT_KEY     = 8;
        //}yongan.qiu

    }

    private final CharSequence mUnknownNameText;

    public SimplePhoneNumberListAdapter(Context context, Cursor cursor) {
    	super(context, cursor);
        //begin: added by yunzhou.song
        mFragment = null;
        //end: added by yunzhou.song
        mUnknownNameText = context.getText(android.R.string.unknownName);
    }
    
    //begin: added by yunzhou.song
    private final PhoneNumberPickerFragment mFragment;
    public SimplePhoneNumberListAdapter(Context context, Cursor cursor, PhoneNumberPickerFragment fragment) {
    	super(context, cursor);
        mFragment = fragment;
        mUnknownNameText = context.getText(android.R.string.unknownName);
    }
    //end: added by yunzhou.song

    protected CharSequence getUnknownNameText() {
        return mUnknownNameText;
    }

    //{yongan.qiu
    String mQueryString;
    public String getQueryString() {
    	return mQueryString;
    }
    public void setQueryString(String queryString) {
    	mQueryString = queryString;
    }
    private int mDisplayOrder;
    public int getContactNameDisplayOrder() {
    	return mDisplayOrder;
    }
    public void setContactNameDisplayOrder(int displayOrder) {
    	mDisplayOrder = displayOrder;
    }
    private int mSortOrder;
    public int getSortOrder() {
    	return mSortOrder;
    }
    public void setSortOrder(int sortOrder) {
        mSortOrder = sortOrder;
    }

    private ContactPhotoManager mPhotoManager;
    public ContactPhotoManager getPhotoLoader() {
    	return mPhotoManager;
    }
    public void setPhotoLoader(ContactPhotoManager photoManager) {
    	mPhotoManager = photoManager;
    }
    public void changeCursor(Cursor c) {
    	mLoading = false;
    	super.changeCursor(c);
    }
    private boolean mLoading;
    public boolean isLoading() {
    	return mLoading;
    }
    private ContactListFilter mFilter;
    public void setContactFilter(ContactListFilter filter) {
    	mFilter = filter;
    }
    public ContactListFilter getContactFilter() {
    	return mFilter;
    }
    public void onDataReload() {
    	mLoading = true;
    }
    private long mDirectoryId;
    private boolean mDarkTheme = false;
    
    private HashMap<Long, String> mId2Match;
    
    public void setId2Match(HashMap<Long, String> map) {
        mId2Match = map;
        Log.i(TAG, "mId2Match " + mId2Match);
    }
    //}yongan.qiu
    
    public void configureLoader(CursorLoader loader, long directoryId) {
        configureLoader(loader, directoryId, null);
    }
    public void configureLoader(CursorLoader loader, long directoryId, Set<Long> ids) {
        Uri uri;
        
        //{yongan.qiu
        mDirectoryId = directoryId;
        //}yongan.qiu

        if (directoryId != Directory.DEFAULT) {
            Log.w(TAG, "MyPhoneNumberListAdapter is not ready for non-default directory ID ("
                    + "directoryId: " + directoryId + ")");
        }

        /*String query = getQueryString();
        Log.i("<><><><>", "" + query);
        if (!TextUtils.isEmpty(query)) {
            Builder builder = Phone.CONTENT_FILTER_URI.buildUpon();
            builder.appendPath(query);      // Builder will encode the query

            builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                    String.valueOf(directoryId));
            uri = builder.build();
        } else*/ {
            uri = Phone.CONTENT_URI.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT))
                    .build();
            if (ids == null) {
                configureSelection(loader, directoryId, getContactFilter());
            } else {
                configureSelectionWithContactIds(loader, directoryId, getContactFilter(), ids);
        	}
        }

        // Remove duplicates when it is possible.
        uri = uri.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
                .build();
        loader.setUri(uri);

        // TODO a projection that includes the search snippet
        if (getContactNameDisplayOrder() == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY) {
            loader.setProjection(PhoneQuery.PROJECTION_PRIMARY);
        } else {
            loader.setProjection(PhoneQuery.PROJECTION_ALTERNATIVE);
        }
        
        if (getSortOrder() == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
            loader.setSortOrder(Phone.SORT_KEY_PRIMARY);
        } else {
            loader.setSortOrder(Phone.SORT_KEY_ALTERNATIVE);
        }
    }

    private void configureSelectionWithContactIds(
            CursorLoader loader, long directoryId, ContactListFilter filter, Set<Long> ids) {
        if (ids == null) throw new IllegalArgumentException("Contact ids should not be null.");
        if (filter == null || directoryId != Directory.DEFAULT) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        final List<String> selectionArgs = new ArrayList<String>();

        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_CUSTOM: {
                selection.append(Contacts.IN_VISIBLE_GROUP + "=1");
                selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                break;
            }
            case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                selection.append("(");

                selection.append(RawContacts.ACCOUNT_TYPE + "=?"
                        + " AND " + RawContacts.ACCOUNT_NAME + "=?");
                selectionArgs.add(filter.accountType);
                selectionArgs.add(filter.accountName);
                if (filter.dataSet != null) {
                    selection.append(" AND " + RawContacts.DATA_SET + "=?");
                    selectionArgs.add(filter.dataSet);
                } else {
                    selection.append(" AND " + RawContacts.DATA_SET + " IS NULL");
                }
                selection.append(")");
                break;
            }
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS:
            case ContactListFilter.FILTER_TYPE_DEFAULT:
                break; // No selection needed.
            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                break; // This adapter is always "phone only", so no selection needed either.
            default:
                Log.w(TAG, "Unsupported filter type came " +
                        "(type: " + filter.filterType + ", toString: " + filter + ")" +
                        " showing all contacts.");
                // No selection.
                break;
        }

        if (filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM 
        		|| filter.filterType == ContactListFilter.FILTER_TYPE_ACCOUNT) {
            selection.append(" AND ");
        }
        selection.append(Phone._ID + " IN (");
        for (Long id : ids) {
            selection.append(id).append(',');
        }
        if (ids.size() > 0) {
            selection.delete(selection.length() - 1, selection.length());
        }
        selection.append(")");

        Log.i(TAG, "selection is : " + selection);
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }
    
    private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter) {
        if (filter == null || directoryId != Directory.DEFAULT) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        final List<String> selectionArgs = new ArrayList<String>();

        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_CUSTOM: {
                selection.append(Contacts.IN_VISIBLE_GROUP + "=1");
                selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                break;
            }
            case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                selection.append("(");

                selection.append(RawContacts.ACCOUNT_TYPE + "=?"
                        + " AND " + RawContacts.ACCOUNT_NAME + "=?");
                selectionArgs.add(filter.accountType);
                selectionArgs.add(filter.accountName);
                if (filter.dataSet != null) {
                    selection.append(" AND " + RawContacts.DATA_SET + "=?");
                    selectionArgs.add(filter.dataSet);
                } else {
                    selection.append(" AND " + RawContacts.DATA_SET + " IS NULL");
                }
                selection.append(")");
                break;
            }
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS:
            case ContactListFilter.FILTER_TYPE_DEFAULT:
                break; // No selection needed.
            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                break; // This adapter is always "phone only", so no selection needed either.
            default:
                Log.w(TAG, "Unsupported filter type came " +
                        "(type: " + filter.filterType + ", toString: " + filter + ")" +
                        " showing all contacts.");
                // No selection.
                break;
        }
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }

    public Uri getDataUri(int position) {
        Cursor cursor = ((Cursor)getItem(position));
        if (cursor != null) {
            long id = cursor.getLong(PhoneQuery.PHONE_ID);
            return ContentUris.withAppendedId(Data.CONTENT_URI, id);
        } else {
            Log.w(TAG, "Cursor was null in getDataUri() call. Returning null instead.");
            return null;
        }
    }

    OnClickListener mActionListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (mOnItemActionListener != null) {
				int position = (Integer) v.getTag();
				mOnItemActionListener.onItemAction(position);
			}
		}
	};
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.simple_phone_number_list_item, parent, false);
        view.findViewById(R.id.phone_number_item).setOnClickListener(mActionListener);
        return view;
    }

    @Override
    public void bindView(View itemView, Context context, Cursor cursor) {
        String match = null;
        if (mId2Match != null) {
            match = mId2Match.get(cursor.getLong(PhoneQuery.PHONE_ID));
        }
        Log.i(TAG, "match " + match);
        itemView.findViewById(R.id.phone_number_item).setTag(new Integer(cursor.getPosition()));
        boolean nameMatched = bindName(itemView, cursor, match);
        bindQuickContact(itemView, cursor, PhoneQuery.PHONE_PHOTO_ID,
                PhoneQuery.PHONE_CONTACT_ID, PhoneQuery.PHONE_LOOKUP_KEY);
        bindLabel(itemView, context, cursor);
        bindNumber(itemView, cursor, nameMatched ? null : match);
    }

    protected void bindQuickContact(final View view,
            Cursor cursor, int photoIdColumn, int contactIdColumn, int lookUpKeyColumn) {
        long photoId = 0;
        if (!cursor.isNull(photoIdColumn)) {
            photoId = cursor.getLong(photoIdColumn);
        }

        QuickContactBadge quickContact = (QuickContactBadge) view.findViewById(R.id.quick_contact_badge);
        quickContact.assignContactUri(
                getContactUri(cursor, contactIdColumn, lookUpKeyColumn));
        getPhotoLoader().loadPhoto(quickContact, photoId, false, mDarkTheme);
    }

    protected Uri getContactUri(Cursor cursor,
            int contactIdColumn, int lookUpKeyColumn) {
        long contactId = cursor.getLong(contactIdColumn);
        String lookupKey = cursor.getString(lookUpKeyColumn);
        Uri uri = Contacts.getLookupUri(contactId, lookupKey);
        if (mDirectoryId != Directory.DEFAULT) {
            uri = uri.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(mDirectoryId)).build();
        }
        return uri;
    }

    protected void bindLabel(View view, Context context, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(PhoneQuery.PHONE_TYPE)) {
            final int type = cursor.getInt(PhoneQuery.PHONE_TYPE);
            final String customLabel = cursor.getString(PhoneQuery.PHONE_LABEL);

            // TODO cache
            label = Phone.getTypeLabel(context.getResources(), type, customLabel);
        }
        ((TextView) view.findViewById(R.id.label)).setText(label);
    }
    private PrefixHighlighter mPrefixHighligher;
    protected boolean bindName(final View view, Cursor cursor, String match) {
        TextView nameField = (TextView) view.findViewById(R.id.name);
        String name = cursor.getString(PhoneQuery.PHONE_DISPLAY_NAME);
        Log.i(TAG, "name = " + name + " ,match = " + match);
        if (TextUtils.isEmpty(name)) {
            name = mUnknownNameText.toString();
        } else if (!TextUtils.isEmpty(match)) {
            int index, first = -1, last = -1;
            //name = toPinYin(name);
            char[] s = name.toCharArray();
            int l = s.length;
            char c = s[0];
            
            int i = 0;
            index = match.indexOf(c);
            if (index >= 0) {
                first = last = i;
                match = match.substring(index + 1);
            } else {
                for (i = 1; i < l && !TextUtils.isEmpty(match); i++) {
                    c = s[i];
                    index = match.indexOf(c);
                    if (index >= 0) {
                        if (first < 0) {
                            first = i;
                        }
                        last = i;
                        match = match.substring(index + 1);
                    } else {
                        break;
                    }
                }
                
                if (first >= 0 && last >= 0) {
                    //match success
                    if (mPrefixHighligher == null) {
                        mPrefixHighligher = new PrefixHighlighter(Color.BLUE);
                    }
                    Log.i(TAG, "bindName: first " + first + ", last " + last);
                    nameField.setText(mPrefixHighligher.apply(name, first, last + 1));
                    return true;
                }
            }
            Log.i(TAG, "bindName: first " + first + ", last " + last);
        }
        nameField.setText(name);
        return false;
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
                output.append(token.target);
            }
        }
        return output.toString();
    }
    protected boolean bindNumber(final View view, Cursor cursor, String match) {
        TextView numberField = (TextView) view.findViewById(R.id.number);
        String number = cursor.getString(PhoneQuery.PHONE_NUMBER);
        Log.i(TAG, "number = " + number + " ,match = " + match);
        if (TextUtils.isEmpty(number)) {
            number = mUnknownNameText.toString();
        } else if (!TextUtils.isEmpty(match)) {
            int index, first = -1, last = -1;
            char[] s = number.toCharArray();
            int oriLength = s.length;
            char[] shortNumber = new char[oriLength];
            int[] indexes = new int[oriLength];
            char c;
            int i, j = 0;
            for (i = 0; i < oriLength; i++) {
                c = s[i];
                if ((c >= '0' && c <= '9') || c == '+') {
                    shortNumber[j] = c;
                    indexes[j++] = i;
                }
            }
            String shortNumberString = new String(shortNumber, 0, j);
            Log.i(TAG, "bindNumber: shortNumber = " + shortNumberString);
            first = shortNumberString.indexOf(match);
            if (first >= 0) {
                last = first + match.length() - 1;
            }
            if (first >= 0 && last >= 0) {
                Log.i(TAG, "bindNumber 1: first " + first + ", last " + last);
                first = indexes[first];
                last = indexes[last];
                //match success
                if (mPrefixHighligher == null) {
                    mPrefixHighligher = new PrefixHighlighter(Color.BLUE);
                }
                Log.i(TAG, "bindNumber 2: first " + first + ", last " + last);
                numberField.setText(mPrefixHighligher.apply(number, first, last + 1));
                return true;
            }
            Log.i(TAG, "bindNumber: first " + first + ", last " + last);
        }
        numberField.setText(number);
        return false;
    }

    protected void bindPhoto(final View view, Cursor cursor) {
        long photoId = 0;
        if (!cursor.isNull(PhoneQuery.PHONE_PHOTO_ID)) {
            photoId = cursor.getLong(PhoneQuery.PHONE_PHOTO_ID);
        }

        ImageView photo = (ImageView) view.findViewById(R.id.quick_contact_badge);
        getPhotoLoader().loadPhoto(photo, photoId, false, false);
    }

    OnItemActionListener mOnItemActionListener;

    interface OnItemActionListener {
        void onItemAction(int position);
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        mOnItemActionListener = listener;
    }

}
