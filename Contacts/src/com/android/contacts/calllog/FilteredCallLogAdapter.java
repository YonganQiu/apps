/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.calllog;

import com.android.common.widget.GroupingListAdapter;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.PhoneCallDetails;
import com.android.contacts.PhoneCallDetailsHelper;
import com.android.contacts.PhoneCallDetailsViews;
import com.android.contacts.R;
import com.android.contacts.calllog.UnknownNumberCallLogQueryHandler.UnknownNumberCallLogQuery;
import com.android.contacts.format.PrefixHighlighter;
import com.android.contacts.util.ExpirableCache;
import com.android.contacts.util.UriUtils;
import com.google.common.annotations.VisibleForTesting;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import java.util.LinkedList;

import libcore.util.Objects;

/**
 * 
 * @author yongan.qiu
 *
 */
public class FilteredCallLogAdapter extends CursorAdapter {
    /** Interface used to initiate a refresh of the content. */
    public interface CallFetcher {
        public void fetchCalls();
    }

    private final Context mContext;
    private final CallFetcher mCallFetcher;

    private boolean mLoading = true;

    /** Helper to set up contact photos. */
    private final ContactPhotoManager mContactPhotoManager;

    /** Helper to parse and process phone numbers. */
    private PhoneNumberHelper mPhoneNumberHelper;

    private PrefixHighlighter mPrefixHighligher;

    /** Listener for the secondary action in the list, either call or play. */
    private final View.OnClickListener mActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            IntentProvider intentProvider = (IntentProvider) view.getTag();
            if (intentProvider != null) {
                mContext.startActivity(intentProvider.getIntent(mContext));
            }
        }
    };

    public FilteredCallLogAdapter(Context context, CallFetcher callFetcher,
            ContactInfoHelper contactInfoHelper, Cursor cursor) {
        super(context, cursor);

        mContext = context;
        mCallFetcher = callFetcher;

        mContactPhotoManager = ContactPhotoManager.getInstance(mContext);

        Resources resources = mContext.getResources();
        mPhoneNumberHelper = new PhoneNumberHelper(resources);
    }

    /**
     * Requery on background thread when {@link Cursor} changes.
     */
    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        mCallFetcher.fetchCalls();
    }

    public void setLoading(boolean loading) {
        mLoading = loading;
    }

    private String mMatch;
    public void setMatch(String match) {
        mMatch = match;
    }

    @Override
    public boolean isEmpty() {
        if (mLoading) {
            // We don't want the empty state to show when loading.
            return false;
        } else {
            return super.isEmpty();
        }
    }

    @Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.simple_call_log_list_item, parent, false);
        view.findViewById(R.id.primary_action_view).setOnClickListener(mActionListener);
        return view;
    }

    @Override
	public void bindView(View view, Context context, Cursor cursor) {
        bindView(view, cursor);
    }

    /**
     * Binds the views in the entry to the data in the call log.
     *
     * @param view the view corresponding to this entry
     * @param c the cursor pointing to the entry in the call log
     */
    private void bindView(View view, Cursor c) {
        QuickContactBadge badgeView = (QuickContactBadge) view.findViewById(R.id.quick_contact_photo);
        View primaryView = view.findViewById(R.id.primary_action_view);
        TextView nameView = (TextView) view.findViewById(R.id.name);
        TextView numberView = (TextView) view.findViewById(R.id.number);

        final String number = c.getString(UnknownNumberCallLogQuery.NUMBER);
        final String name = new String(number);
        int first = -1, last = -1;
        boolean nameSet = false;
        
        // Store away the voicemail information so we can play it directly.
        if (!TextUtils.isEmpty(number)) {
            // Store away the number so we can call it directly if you click on the call icon.
            primaryView.setTag(
                    IntentProvider.getReturnCallIntentProvider(number));
            if (!TextUtils.isEmpty(mMatch)) {
                first = name.indexOf(mMatch);
                if (first >= 0) {
                    last = first + mMatch.length() - 1;
                    if (mPrefixHighligher == null) {
                        mPrefixHighligher = new PrefixHighlighter(Color.BLUE);
                    }
                    nameView.setText(mPrefixHighligher.apply(name, first, last + 1));
                    nameSet = true;
                }
            }
        } else {
            // No action enabled.
            primaryView.setTag(null);
        }

        if (!mPhoneNumberHelper.canPlaceCallsTo(number)) {
            // If this is a number that cannot be dialed, there is no point in looking up a contact
            // for it.
            // TODO
        }

        if (!nameSet) {
            nameView.setText(name);
        }
        numberView.setText(number);
        setPhoto(badgeView, 0, null);
    }

    private void setPhoto(QuickContactBadge view, long photoId, Uri contactUri) {
        view.assignContactUri(contactUri);
        mContactPhotoManager.loadPhoto(view, photoId, false, false);
    }

}
