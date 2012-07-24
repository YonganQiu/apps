/*
 * Copyright (C) 2011 Google Inc.
 * Licensed to The Android Open Source Project.
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

import com.android.contacts.R;
import com.android.contacts.calllog.FilteredCallLogAdapter;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

/**
 * An adapter that combines items from {@link ContactTileAdapter} and
 * {@link FilteredPhoneNumberAdapter} into a single list. In between those two results,
 * an account filter header will be inserted.
 */
public class FilteredResultsAdapter extends BaseAdapter {

    private class CustomDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }
    }

    private final FilteredCallLogAdapter mCallLogAdapter;
    private final FilteredPhoneNumberAdapter mPhoneNumberAdapter;
    private final View mAccountFilterHeaderContainer;

    private final int mItemPaddingLeft;
    private final int mItemPaddingRight;

    // Make frequent header consistent with account filter header.
    private final int mFrequentHeaderPaddingTop;

    private final DataSetObserver mObserver;

    public FilteredResultsAdapter(Context context,
            FilteredCallLogAdapter contactTileAdapter,
            View accountFilterHeaderContainer,
            FilteredPhoneNumberAdapter MyPhoneNumberListAdapter) {
        Resources resources = context.getResources();
        mItemPaddingLeft = resources.getDimensionPixelSize(R.dimen.detail_item_side_margin);
        mItemPaddingRight = resources.getDimensionPixelSize(R.dimen.list_visible_scrollbar_padding);
        mFrequentHeaderPaddingTop = resources.getDimensionPixelSize(
                R.dimen.contact_browser_list_top_margin);
        mCallLogAdapter = contactTileAdapter;
        mPhoneNumberAdapter = MyPhoneNumberListAdapter;

        mAccountFilterHeaderContainer = accountFilterHeaderContainer;

        mObserver = new CustomDataSetObserver();
        mCallLogAdapter.registerDataSetObserver(mObserver);
        mPhoneNumberAdapter.registerDataSetObserver(mObserver);
    }

    @Override
    public int getCount() {
        final int callLogAdapterCount = mCallLogAdapter.getCount();
        final int phoneNumberAdapterCount = mPhoneNumberAdapter.getCount();
        if (mPhoneNumberAdapter.isLoading()) {
            // Hide "all" contacts during its being loaded.
            return callLogAdapterCount + 1;
        } else {
            return callLogAdapterCount + phoneNumberAdapterCount + 1;
        }
    }

    @Override
    public Object getItem(int position) {
        final int callLogAdapterCount = mCallLogAdapter.getCount();
        final int phoneNumberAdapterCount = mPhoneNumberAdapter.getCount();
        if (position < callLogAdapterCount) {
            return mCallLogAdapter.getItem(position);
        } else if (position == callLogAdapterCount) {
            return mAccountFilterHeaderContainer;
        } else {
            final int localPosition = position - callLogAdapterCount - 1;
            return mPhoneNumberAdapter.getItem(localPosition);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return (mCallLogAdapter.getViewTypeCount()
                + mPhoneNumberAdapter.getViewTypeCount()
                + 1);
    }

    @Override
    public int getItemViewType(int position) {
        final int contactTileAdapterCount = mCallLogAdapter.getCount();
        if (position < contactTileAdapterCount) {
            return mCallLogAdapter.getItemViewType(position);
        } else if (position == contactTileAdapterCount) {
            return mCallLogAdapter.getViewTypeCount()
                    + mPhoneNumberAdapter.getViewTypeCount();
        } else {
            final int localPosition = position - contactTileAdapterCount - 1;
            final int type = mPhoneNumberAdapter.getItemViewType(localPosition);
            // IGNORE_ITEM_VIEW_TYPE must be handled differently.
            return (type < 0) ? type : type + mCallLogAdapter.getViewTypeCount();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int contactTileAdapterCount = mCallLogAdapter.getCount();
        // Obtain a View relevant for that position, and adjust its horizontal padding. Each
        // View has different implementation, so we use different way to control those padding.
        if (position < contactTileAdapterCount) {
            return mCallLogAdapter.getView(position, convertView, parent);
        } else if (position == contactTileAdapterCount) {
            mAccountFilterHeaderContainer.setPadding(mItemPaddingLeft,
                    mAccountFilterHeaderContainer.getPaddingTop(),
                    mItemPaddingRight,
                    mAccountFilterHeaderContainer.getPaddingBottom());
            return mAccountFilterHeaderContainer;
        } else {
            final int localPosition = position - contactTileAdapterCount - 1;
            final View itemView = mPhoneNumberAdapter.getView(localPosition, convertView, null);
            // TODO
            //itemView.setPadding(mItemPaddingLeft, itemView.getPaddingTop(),
            //        mItemPaddingRight, itemView.getPaddingBottom());
            //itemView.setSelectionBoundsHorizontalMargin(mItemPaddingLeft, mItemPaddingRight);
            return itemView;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return (mCallLogAdapter.areAllItemsEnabled()
                && mAccountFilterHeaderContainer.isEnabled()
                && mPhoneNumberAdapter.areAllItemsEnabled());
    }

    @Override
    public boolean isEnabled(int position) {
        final int contactTileAdapterCount = mCallLogAdapter.getCount();
        if (position < contactTileAdapterCount) {
            return mCallLogAdapter.isEnabled(position);
        } else if (position == contactTileAdapterCount) {
            // This will be handled by View's onClick event instead of ListView's onItemClick event.
            return false;
        } else {
            final int localPosition = position - contactTileAdapterCount - 1;
            return mPhoneNumberAdapter.isEnabled(localPosition);
        }
    }

    public boolean shouldShowFirstScroller(int firstVisibleItem) {
        return true;
    }
}