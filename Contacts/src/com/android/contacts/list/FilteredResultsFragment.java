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
package com.android.contacts.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.android.common.io.MoreCloseables;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactTileLoaderFactory;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.activities.ViewPagerVisibilityListener;
import com.android.contacts.calllog.CallLogNotificationsService;
import com.android.contacts.calllog.CallLogQuery;
import com.android.contacts.calllog.UnknownNumberCallLogQueryHandler;
import com.android.contacts.calllog.ClearCallLogDialog;
import com.android.contacts.calllog.ContactInfoHelper;
import com.android.contacts.calllog.FilteredCallLogAdapter;
import com.google.common.annotations.VisibleForTesting;
import com.android.contacts.list.FilteredPhoneNumberAdapter.OnItemActionListener;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.EmptyLoader;
import com.android.contacts.voicemail.VoicemailStatusHelper;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;
import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.internal.telephony.CallerInfo;

import android.app.Activity;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Directory;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * 
 * @author yongan.qiu
 *
 */
public class FilteredResultsFragment extends Fragment implements ViewPagerVisibilityListener, 
        FilteredPhoneNumberAdapter.OnItemActionListener, 
        UnknownNumberCallLogQueryHandler.Listener, FilteredCallLogAdapter.CallFetcher {
    private static final String TAG = FilteredResultsFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    /**
     * Used with LoaderManager.
     */
    private static int LOADER_ID_PHONE_NUMBER = 1;
    private static int LOADER_ID_RESULT_CONTACTS = 2;

    private static final String KEY_FILTER = "filter";

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    public interface Listener {
        public void onContactSelected(Uri contactUri);
    }

    private HashMap<Long, String> mResult;

    //TODO
    private class ResultContactsLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.d(TAG, "ResultContactsLoaderListener#onCreateLoader");
            CursorLoader loader = new CursorLoader(getActivity(), null, null, null, null, null);
            mPhoneNumberAdapter.configureLoader(loader, Directory.DEFAULT, (mResult == null) ? null : mResult.keySet());
            mPhoneNumberAdapter.setId2Match(mResult);
            mResult = null;
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.d(TAG, "ResultContactsLoaderListener#onLoadFinished");
            if (data != null) {
            	Log.i(TAG, "cursor.count = " + data.getCount() + data);
            } else {
            	Log.i(TAG, "cursor is null!!!");
            }
            
            mPatternAndResultHelper.loadPhoneNumberRefInfos(data, true);
            mPhoneNumberAdapter.changeCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.d(TAG, "ResultContactsLoaderListener#onLoaderReset. ");
        }
    }

    private class PhoneNumberLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.d(TAG, "PhoneNumberLoaderListener#onCreateLoader");
            CursorLoader loader = new CursorLoader(getActivity(), null, null, null, null, null);
            mPhoneNumberAdapter.configureLoader(loader, Directory.DEFAULT);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.d(TAG, "PhoneNumberLoaderListener#onLoadFinished");
            if (data != null) {
            	Log.i(TAG, "cursor.count = " + data.getCount() + data);
            } else {
            	Log.i(TAG, "cursor is null!!!");
            }

            mPatternAndResultHelper.loadPhoneNumberRefInfos(data, false);

            startSearchTask(mQueryString, true);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.d(TAG, "PhoneNumberLoaderListener#onLoaderReset. ");
        }
    }
    
    SearchTask mSearchTask;
    private void startSearchTask(String query, boolean mayInterruptIfRunning) {
        Log.i(TAG, "startSearchTask(). query = " + query);
        if (mSearchTask != null) {
            mSearchTask.cancel(mayInterruptIfRunning);
        }
        mSearchTask = new SearchTask();
        mSearchTask.execute(mQueryString);
    }
    class SearchTask extends AsyncTask<String, Void, HashMap<Long, String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(HashMap<Long, String> result) {
            Log.i(TAG, "onPostExecute().");
            mResult = result;
            //start result query.
            if (mResultContactsLoaderStarted) {
                getLoaderManager().restartLoader(
                        LOADER_ID_RESULT_CONTACTS, null, mResultContactsLoaderListener);
            } else {
                getLoaderManager().initLoader(
                        LOADER_ID_RESULT_CONTACTS, null, mResultContactsLoaderListener);
            }
            mResultContactsLoaderStarted = true;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected HashMap<Long, String> doInBackground(String... params) {
			Log.i(TAG, "doInBackground().");
			String query = params[0];
			Log.i(TAG, "query is " + query);
			if (mPatternAndResultHelper == null || TextUtils.isEmpty(query)) {
				Log.i(TAG, "Cache is null or query key is null.");
				return null;
			}
			return mPatternAndResultHelper.search(mPatternAndResultHelper.getCachedInfos(), query);
		}
    };
    
    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                    FilteredResultsFragment.this, REQUEST_CODE_ACCOUNT_FILTER);
        }
    }

    private class ContactsPreferenceChangeListener
            implements ContactsPreferences.ChangeListener {
        @Override
        public void onChange() {
            if (loadContactsPreferences()) {
                requestReloadPhoneNumber();
            }
        }
    }

    private Listener mListener;
    private FilteredResultsAdapter mAdapter;
    private FilteredPhoneNumberAdapter mPhoneNumberAdapter;

    /**
     * true when the loader for {@link FilteredPhoneNumberAdapter} has started already.
     */
    private boolean mPhoneNumberLoaderStarted;
    
    private boolean mResultContactsLoaderStarted;
    /**
     * true when the loader for {@link FilteredPhoneNumberAdapter} must reload "all" contacts again.
     * It typically happens when {@link ContactsPreferences} has changed its settings
     * (display order and sort order)
     */
    private boolean mPhoneNumberForceReload;
    
    private boolean mSearchFromResultNeeded;

    private ContactsPreferences mContactsPrefs;
    private ContactListFilter mFilter;

    private TextView mEmptyView;
    private ListView mListView;
    /**
     * Layout containing {@link #mAccountFilterHeader}. Used to limit area being "pressed".
     */
    private FrameLayout mAccountFilterHeaderContainer;
    private View mAccountFilterHeader;

    private final LoaderManager.LoaderCallbacks<Cursor> mPhoneNumberLoaderListener =
            new PhoneNumberLoaderListener();
    private final LoaderManager.LoaderCallbacks<Cursor> mResultContactsLoaderListener =
            new ResultContactsLoaderListener();
    private final OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();
    private final ContactsPreferenceChangeListener mContactsPreferenceChangeListener =
            new ContactsPreferenceChangeListener();

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            mFilter = savedState.getParcelable(KEY_FILTER);
        }
        mCallLogQueryHandler = new UnknownNumberCallLogQueryHandler(getActivity().getContentResolver(), this);
        mPatternAndResultHelper = new PatternAndResultHelper();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILTER, mFilter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContactsPrefs = new ContactsPreferences(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View listLayout = inflater.inflate(R.layout.call_log_phone_number_fragment, container, false);

        mListView = (ListView) listLayout.findViewById(R.id.list);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);

        initAdapters(getActivity(), inflater);

        mListView.setAdapter(mAdapter);

        mListView.setVerticalScrollBarEnabled(true);
        mListView.setFastScrollEnabled(true);
        mListView.setFastScrollAlwaysVisible(false);

        mEmptyView = (TextView) listLayout.findViewById(R.id.empty);
        mEmptyView.setText(getString(R.string.listTotalAllContactsZero));
        mListView.setEmptyView(mEmptyView);

        return listLayout;
    }

    private String mQueryString;

    public boolean onQueryTextChange(String newText) {
        if (mCallLogQueryHandler != null) {
            mCallLogQueryHandler.setQueryString(newText);
        }
        String oldText = mQueryString;
        mQueryString = newText;

        refreshData();
        if (newText != null && oldText != null && newText.startsWith(oldText)) {
            mSearchFromResultNeeded = true;
        } else {
            mPhoneNumberForceReload = true;
        }
        return true;
    }

    /**
     * Constructs and initializes {@link #mCallLogAdapter}, {@link #mPhoneNumberAdapter}, and
     * {@link #mPhoneNumberAdapter}.
     *
     * TODO: Move all the code here to {@link FilteredResultsAdapter} if possible.
     * There are two problems: account header (whose content changes depending on filter settings)
     * and OnClickListener (which initiates {@link Activity#startActivityForResult(Intent, int)}).
     * See also issue 5429203, 5269692, and 5432286. If we are able to have a singleton for filter,
     * this work will become easier.
     */
    private void initAdapters(Context context, LayoutInflater inflater) {
        String currentCountryIso = ContactsUtils.getCurrentCountryIso(getActivity());
        mCallLogAdapter = new FilteredCallLogAdapter(getActivity(), this,
                new ContactInfoHelper(getActivity(), currentCountryIso), null);

        // Setup the "all" adapter manually. See also the setup logic in ContactEntryListFragment.
        mPhoneNumberAdapter = new FilteredPhoneNumberAdapter(context, null);
        mPhoneNumberAdapter.setPhotoLoader(ContactPhotoManager.getInstance(context));
        
        mPhoneNumberAdapter.setOnItemActionListener(this);

        if (mFilter != null) {
            mPhoneNumberAdapter.setContactFilter(mFilter);
        }

        // Create the account filter header but keep it hidden until "all" contacts are loaded.
        mAccountFilterHeaderContainer = new FrameLayout(context, null);
        mAccountFilterHeader = inflater.inflate(R.layout.account_filter_header_for_phone_favorite,
                mListView, false);
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        mAccountFilterHeaderContainer.addView(mAccountFilterHeader);
        mAccountFilterHeaderContainer.setVisibility(View.GONE);

        mAdapter = new FilteredResultsAdapter(context,
                mCallLogAdapter, mAccountFilterHeaderContainer, mPhoneNumberAdapter);

    }

    @Override
    public void onStart() {
        // Start the empty loader now to defer other fragments.  We destroy it when both calllog
        // and the voicemail status are fetched.
        getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
                new EmptyLoader.Callback(getActivity()));
        mEmptyLoaderRunning = true;

        super.onStart();

        mContactsPrefs.registerChangeListener(mContactsPreferenceChangeListener);

        // If ContactsPreferences has changed, we need to reload "all" contacts with the new
        // settings. If mPhoneNumberFoarceReload is already true, it should be kept.
        if (loadContactsPreferences()) {
            mPhoneNumberForceReload = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mContactsPrefs.unregisterChangeListener();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(
                        ContactListFilterController.getInstance(getActivity()), resultCode, data);
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    private boolean loadContactsPreferences() {
        if (mContactsPrefs == null || mPhoneNumberAdapter == null) {
            return false;
        }

        boolean changed = false;
        if (mPhoneNumberAdapter.getContactNameDisplayOrder() != mContactsPrefs.getDisplayOrder()) {
            mPhoneNumberAdapter.setContactNameDisplayOrder(mContactsPrefs.getDisplayOrder());
            changed = true;
        }

        if (mPhoneNumberAdapter.getSortOrder() != mContactsPrefs.getSortOrder()) {
            mPhoneNumberAdapter.setSortOrder(mContactsPrefs.getSortOrder());
            changed = true;
        }

        return changed;
    }

    /**
     * Requests to reload "all" contacts. If the section is already loaded, this method will
     * force reloading it now. If the section isn't loaded yet, the actual load may be done later
     * (on {@link #onStart()}.
     */
    private void requestReloadPhoneNumber() {
        if (DEBUG) {
            Log.d(TAG, "requestReloadPhoneNumber()"
                    + " mPhoneNumberAdapter: " + mPhoneNumberAdapter
                    + ", mPhoneNumberLoaderStarted: " + mPhoneNumberLoaderStarted);
        }

        if (mPhoneNumberAdapter == null || !mPhoneNumberLoaderStarted) {
            // Remember this request until next load on onStart().
            mPhoneNumberForceReload = true;
            return;
        }

        if (DEBUG) Log.d(TAG, "Reload \"all\" contacts now.");

        mPhoneNumberAdapter.onDataReload();
        // Use restartLoader() to make LoaderManager to load the section again.
        getLoaderManager().restartLoader(LOADER_ID_PHONE_NUMBER, null, mPhoneNumberLoaderListener);
    }

    public ContactListFilter getFilter() {
        return mFilter;
    }

    public void setFilter(ContactListFilter filter) {
        if ((mFilter == null && filter == null) || (mFilter != null && mFilter.equals(filter))) {
            return;
        }

        if (DEBUG) {
            Log.d(TAG, "setFilter(). old filter (" + mFilter
                    + ") will be replaced with new filter (" + filter + ")");
        }

        mFilter = filter;

        if (mPhoneNumberAdapter != null) {
            mPhoneNumberAdapter.setContactFilter(mFilter);
            requestReloadPhoneNumber();
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }
    
    //----------------------from CallLogFragment.java begin---------------------
    /**
     * ID of the empty loader to defer other fragments.
     */
    private static final int EMPTY_LOADER_ID = 0;

    private FilteredCallLogAdapter mCallLogAdapter;
    private UnknownNumberCallLogQueryHandler mCallLogQueryHandler;

    PatternAndResultHelper mPatternAndResultHelper;

    private boolean mEmptyLoaderRunning;
    private boolean mCallLogFetched;

    /** Called by the UnknownNumberCallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public void onCallsFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        mCallLogAdapter.setLoading(false);
        mCallLogAdapter.setMatch(mCallLogQueryHandler.getQueryString());
        mCallLogAdapter.changeCursor(cursor);

        mCallLogFetched = true;
        destroyEmptyLoaderIfAllDataFetched();

        if (DEBUG) Log.d(TAG, "UnknownNumberCallLogQueryHandler#onCallsFetched");

        if (!mPhoneNumberLoaderStarted) {
            // Load "all" contacts if not loaded yet.
            getLoaderManager().initLoader(
                    LOADER_ID_PHONE_NUMBER, null, mPhoneNumberLoaderListener);
        } else if (mPhoneNumberForceReload) {
            mPhoneNumberAdapter.onDataReload();
            // Use restartLoader() to make LoaderManager to load the section again.
            getLoaderManager().restartLoader(
                    LOADER_ID_PHONE_NUMBER, null, mPhoneNumberLoaderListener);
        } else if (mSearchFromResultNeeded) {
            startSearchTask(mQueryString, true);
        }
        mPhoneNumberForceReload = false;
        mSearchFromResultNeeded = false;
        mPhoneNumberLoaderStarted = true;
    }

    private void destroyEmptyLoaderIfAllDataFetched() {
        if (mCallLogFetched && mEmptyLoaderRunning) {
            mEmptyLoaderRunning = false;
            getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCallLogAdapter.changeCursor(null);
    }

    @Override
    public void fetchCalls() {
        startCallsQuery();
    }

    public void startCallsQuery() {
        mCallLogAdapter.setLoading(true);
        mCallLogQueryHandler.fetchAllUnknownCalls();
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (visible && isResumed()) {
            refreshData();
        }
    }

    /** Requests updates to the data to be shown. */
    private void refreshData() {
        startCallsQuery();
    }

	@Override
	public void onItemAction(int position) {
		// TODO Auto-generated method stub
		if (mListener != null) {
			mListener.onContactSelected(mPhoneNumberAdapter
					.getDataUri(position));
		}
	}

}