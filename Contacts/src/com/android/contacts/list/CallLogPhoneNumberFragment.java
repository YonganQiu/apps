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
import java.util.List;

import com.android.common.io.MoreCloseables;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactTileLoaderFactory;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.activities.ViewPagerVisibilityListener;
import com.android.contacts.calllog.CallLogNotificationsService;
import com.android.contacts.calllog.CallLogQuery;
import com.android.contacts.calllog.SimpleCallLogQueryHandler;
import com.android.contacts.calllog.ClearCallLogDialog;
import com.android.contacts.calllog.ContactInfoHelper;
import com.android.contacts.calllog.SimpleCallLogAdapter;
import com.google.common.annotations.VisibleForTesting;
import com.android.contacts.list.SimplePhoneNumberListAdapter.OnItemActionListener;
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
 * Fragment for Phone UI's favorite screen.
 *
 * This fragment contains three kinds of contacts in one screen: "starred", "frequent", and "all"
 * contacts. To show them at once, this merges results from {@link ContactTileAdapter} and
 * {@link SimplePhoneNumberListAdapter} into one unified list using {@link CallLogPhoneNumberMergedAdapter}.
 * A contact filter header is also inserted between those adapters' results.
 */
public class CallLogPhoneNumberFragment extends Fragment implements ViewPagerVisibilityListener, 
        SimplePhoneNumberListAdapter.OnItemActionListener, 
        SimpleCallLogQueryHandler.Listener, SimpleCallLogAdapter.CallFetcher {
    private static final String TAG = CallLogPhoneNumberFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    /**
     * Used with LoaderManager.
     */
    private static int LOADER_ID_CONTACT_TILE = 1;
    private static int LOADER_ID_ALL_CONTACTS = 2;
    private static int LOADER_ID_RESULT_CONTACTS = 3;

    private static final String KEY_FILTER = "filter";

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    public interface Listener {
        public void onContactSelected(Uri contactUri);
    }

    private ArrayList<Long> mResult;
    private boolean mInSearchMode = false;
    private boolean mFirstEnterSearchMode = false;
    //TODO
    private class ResultContactsLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.d(TAG, "ResultContactsLoaderListener#onCreateLoader");
            CursorLoader loader = new CursorLoader(getActivity(), null, null, null, null, null);
            mAllContactsAdapter.configureLoader(loader, Directory.DEFAULT, mResult);
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
            
            mSimplePhoneNumberCache.loadPhoneNumberRefInfos(data);
            mAllContactsAdapter.changeCursor(data);
            //updateFilterHeaderView();
            //mAccountFilterHeaderContainer.setVisibility(View.VISIBLE);

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.d(TAG, "ResultContactsLoaderListener#onLoaderReset. ");
        }
    }

    private class AllContactsLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.d(TAG, "AllContactsLoaderListener#onCreateLoader");
            CursorLoader loader = new CursorLoader(getActivity(), null, null, null, null, null);
            mAllContactsAdapter.configureLoader(loader, Directory.DEFAULT);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.d(TAG, "AllContactsLoaderListener#onLoadFinished");
            if (data != null) {
            	Log.i(TAG, "cursor.count = " + data.getCount() + data);
            } else {
            	Log.i(TAG, "cursor is null!!!");
            }

            mSimplePhoneNumberCache.loadPhoneNumberRefInfos(data);

            startSearchTask(mQueryString, true);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.d(TAG, "AllContactsLoaderListener#onLoaderReset. ");
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
    class SearchTask extends AsyncTask<String, Void, ArrayList<Long>> {
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(ArrayList<Long> result) {
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
			// TODO Auto-generated method stub
			super.onCancelled();
		}

		@Override
		protected ArrayList<Long> doInBackground(String... params) {
			Log.i(TAG, "doInBackground().");
			String query = params[0];
			Log.i(TAG, "query is " + query);
			if (mSimplePhoneNumberCache == null || TextUtils.isEmpty(query)) {
				Log.i(TAG, "Cache is null or query key is null.");
				return null;
			}
			return mSimplePhoneNumberCache.search(mSimplePhoneNumberCache.getCachedInfos(), query);
		}
    };
    
    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                    CallLogPhoneNumberFragment.this, REQUEST_CODE_ACCOUNT_FILTER);
        }
    }

    private class ContactsPreferenceChangeListener
            implements ContactsPreferences.ChangeListener {
        @Override
        public void onChange() {
            if (loadContactsPreferences()) {
                requestReloadAllContacts();
            }
        }
    }

    private class ScrollListener implements ListView.OnScrollListener {
        private boolean mShouldShowFastScroller;
        @Override
        public void onScroll(AbsListView view,
                int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            // FastScroller should be visible only when the user is seeing "all" contacts section.
            final boolean shouldShow = mAdapter.shouldShowFirstScroller(firstVisibleItem);
            if (shouldShow != mShouldShowFastScroller) {
                mListView.setVerticalScrollBarEnabled(shouldShow);
                mListView.setFastScrollEnabled(shouldShow);
                mListView.setFastScrollAlwaysVisible(shouldShow);
                mShouldShowFastScroller = shouldShow;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }

    private Listener mListener;
    private CallLogPhoneNumberMergedAdapter mAdapter;
    //private CallLogAdapter mCallLogAdapter;
    private SimplePhoneNumberListAdapter mAllContactsAdapter;

    /**
     * true when the loader for {@link SimplePhoneNumberListAdapter} has started already.
     */
    private boolean mAllContactsLoaderStarted;
    
    private boolean mResultContactsLoaderStarted;
    /**
     * true when the loader for {@link SimplePhoneNumberListAdapter} must reload "all" contacts again.
     * It typically happens when {@link ContactsPreferences} has changed its settings
     * (display order and sort order)
     */
    private boolean mAllContactsForceReload;
    
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

    //TODO
    /*private final LoaderManager.LoaderCallbacks<Cursor> mContactTileLoaderListener =
            new ContactTileLoaderListener();*/
    private final LoaderManager.LoaderCallbacks<Cursor> mAllContactsLoaderListener =
            new AllContactsLoaderListener();
    private final LoaderManager.LoaderCallbacks<Cursor> mResultContactsLoaderListener =
            new ResultContactsLoaderListener();
    private final OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();
    private final ContactsPreferenceChangeListener mContactsPreferenceChangeListener =
            new ContactsPreferenceChangeListener();
    private final ScrollListener mScrollListener = new ScrollListener();

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            mFilter = savedState.getParcelable(KEY_FILTER);
        }
        //{
        mCallLogQueryHandler = new SimpleCallLogQueryHandler(getActivity().getContentResolver(), this);
        mSimplePhoneNumberCache = new SimplePhoneNumberCache();
        //}
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
        //mListView.setItemsCanFocus(true);
        //mListView.setOnItemClickListener(this);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);

        initAdapters(getActivity(), inflater);

        mListView.setAdapter(mAdapter);

        mListView.setOnScrollListener(mScrollListener);
        mListView.setFastScrollEnabled(false);
        mListView.setFastScrollAlwaysVisible(false);

        mEmptyView = (TextView) listLayout.findViewById(R.id.empty);
        mEmptyView.setText(getString(R.string.listTotalAllContactsZero));
        mListView.setEmptyView(mEmptyView);

        //updateFilterHeaderView();
        return listLayout;
    }
    
    //{yongan.qiu
    String mQueryString;
	public boolean onQueryTextChange(String newText) {
		if (TextUtils.isEmpty(newText)) {
			exitSearchMode();
		} else {
			ensureSearchMode();
		}
		if (mCallLogQueryHandler != null) {
			mCallLogQueryHandler.setQueryString(newText);
		}
		String oldText = mQueryString;
		mQueryString = newText;
		
		refreshData();
		if (newText != null && oldText != null && newText.startsWith(oldText)) {
			mSearchFromResultNeeded = true;
		} else {
			//mAllContactsAdapter.setQueryString(newText);
			mAllContactsForceReload = true;
		}
		// TODO Auto-generated method stub
		return true;
	}
	//}yongan.qiu

    /**
     * Constructs and initializes {@link #mCallLogAdapter}, {@link #mAllContactsAdapter}, and
     * {@link #mAllContactsAdapter}.
     *
     * TODO: Move all the code here to {@link CallLogPhoneNumberMergedAdapter} if possible.
     * There are two problems: account header (whose content changes depending on filter settings)
     * and OnClickListener (which initiates {@link Activity#startActivityForResult(Intent, int)}).
     * See also issue 5429203, 5269692, and 5432286. If we are able to have a singleton for filter,
     * this work will become easier.
     */
    private void initAdapters(Context context, LayoutInflater inflater) {
        String currentCountryIso = ContactsUtils.getCurrentCountryIso(getActivity());
        mCallLogAdapter = new SimpleCallLogAdapter(getActivity(), this,
                new ContactInfoHelper(getActivity(), currentCountryIso), null);

        // Setup the "all" adapter manually. See also the setup logic in ContactEntryListFragment.
        mAllContactsAdapter = new SimplePhoneNumberListAdapter(context, null);
        mAllContactsAdapter.setPhotoLoader(ContactPhotoManager.getInstance(context));
        
        mAllContactsAdapter.setOnItemActionListener(this);

        if (mFilter != null) {
            mAllContactsAdapter.setContactFilter(mFilter);
        }

        // Create the account filter header but keep it hidden until "all" contacts are loaded.
        mAccountFilterHeaderContainer = new FrameLayout(context, null);
        mAccountFilterHeader = inflater.inflate(R.layout.account_filter_header_for_phone_favorite,
                mListView, false);
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        mAccountFilterHeaderContainer.addView(mAccountFilterHeader);
        mAccountFilterHeaderContainer.setVisibility(View.GONE);

        mAdapter = new CallLogPhoneNumberMergedAdapter(context,
                mCallLogAdapter, mAccountFilterHeaderContainer, mAllContactsAdapter);

    }

    @Override
    public void onStart() {
        //{
        // Start the empty loader now to defer other fragments.  We destroy it when both calllog
        // and the voicemail status are fetched.
        getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
                new EmptyLoader.Callback(getActivity()));
        mEmptyLoaderRunning = true;
        //}
        super.onStart();

        mContactsPrefs.registerChangeListener(mContactsPreferenceChangeListener);

        // If ContactsPreferences has changed, we need to reload "all" contacts with the new
        // settings. If mAllContactsFoarceReload is already true, it should be kept.
        if (loadContactsPreferences()) {
            mAllContactsForceReload = true;
        }

        // Use initLoader() instead of reloadLoader() to refraing unnecessary reload.
        // This method call implicitly assures ContactTileLoaderListener's onLoadFinished() will
        // be called, on which we'll check if "all" contacts should be reloaded again or not.
        //TODO
        //getLoaderManager().initLoader(LOADER_ID_CONTACT_TILE, null, mContactTileLoaderListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContactsPrefs.unregisterChangeListener();
    }

    /**
     * {@inheritDoc}
     *
     * This is only effective for elements provided by {@link #mCallLogAdapter}.
     * {@link #mCallLogAdapter} has its own logic for click events.
     */
    /*@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int callLogAdapterCount = mCallLogAdapter.getCount();
        if (position <= callLogAdapterCount) {
            //TODO
            Log.e(TAG, "onItemClick() event for unexpected position. "
                    + "The position " + position + " is before \"all\" section. Ignored.");
        } else {
            final int localPosition = position - mCallLogAdapter.getCount() - 1;
            if (mListener != null) {
                mListener.onContactSelected(mAllContactsAdapter.getDataUri(localPosition));
            }
        }
    }*/

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

    interface OnSearchModeChangedListener {
        void onEnterSearchMode();
        void onExitSearchMode();
    }
    OnSearchModeChangedListener mOnSearchModeChangedListener;
    public void setOnSearchModeChangedListener(OnSearchModeChangedListener listener) {
        mOnSearchModeChangedListener = listener;
    }
    public void ensureSearchMode() {
        if (mInSearchMode) {
            mFirstEnterSearchMode = false;
            return;
        }
        mInSearchMode = true;
        mFirstEnterSearchMode = true;
        if (mOnSearchModeChangedListener != null) {
            mOnSearchModeChangedListener.onEnterSearchMode();
        }
    }
    public void exitSearchMode() {
        if (!mInSearchMode) {
            return;
        }
        mInSearchMode = false;
        mResult = null;
        if (mOnSearchModeChangedListener != null) {
            mOnSearchModeChangedListener.onExitSearchMode();
        }
    }
    public boolean isInSearchMode() {
        return mInSearchMode;
    }
    
    private boolean loadContactsPreferences() {
        if (mContactsPrefs == null || mAllContactsAdapter == null) {
            return false;
        }

        boolean changed = false;
        if (mAllContactsAdapter.getContactNameDisplayOrder() != mContactsPrefs.getDisplayOrder()) {
            mAllContactsAdapter.setContactNameDisplayOrder(mContactsPrefs.getDisplayOrder());
            changed = true;
        }

        if (mAllContactsAdapter.getSortOrder() != mContactsPrefs.getSortOrder()) {
            mAllContactsAdapter.setSortOrder(mContactsPrefs.getSortOrder());
            changed = true;
        }

        return changed;
    }

    /**
     * Requests to reload "all" contacts. If the section is already loaded, this method will
     * force reloading it now. If the section isn't loaded yet, the actual load may be done later
     * (on {@link #onStart()}.
     */
    private void requestReloadAllContacts() {
        if (DEBUG) {
            Log.d(TAG, "requestReloadAllContacts()"
                    + " mAllContactsAdapter: " + mAllContactsAdapter
                    + ", mAllContactsLoaderStarted: " + mAllContactsLoaderStarted);
        }

        if (mAllContactsAdapter == null || !mAllContactsLoaderStarted) {
            // Remember this request until next load on onStart().
            mAllContactsForceReload = true;
            return;
        }

        if (DEBUG) Log.d(TAG, "Reload \"all\" contacts now.");

        mAllContactsAdapter.onDataReload();
        // Use restartLoader() to make LoaderManager to load the section again.
        getLoaderManager().restartLoader(LOADER_ID_ALL_CONTACTS, null, mAllContactsLoaderListener);
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

        if (mAllContactsAdapter != null) {
            mAllContactsAdapter.setContactFilter(mFilter);
            requestReloadAllContacts();
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

    private SimpleCallLogAdapter mCallLogAdapter;
    private SimpleCallLogQueryHandler mCallLogQueryHandler;

    SimplePhoneNumberCache mSimplePhoneNumberCache;

    private boolean mEmptyLoaderRunning;
    private boolean mCallLogFetched;

    /** Called by the SimpleCallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public void onCallsFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        mCallLogAdapter.setLoading(false);
        mCallLogAdapter.changeCursor(cursor);

        mCallLogFetched = true;
        destroyEmptyLoaderIfAllDataFetched();
        
        //{
        if (DEBUG) Log.d(TAG, "ContactTileLoaderListener#onLoadFinished");

        if (mAllContactsForceReload) {
            mAllContactsAdapter.onDataReload();
            // Use restartLoader() to make LoaderManager to load the section again.
            getLoaderManager().restartLoader(
                    LOADER_ID_ALL_CONTACTS, null, mAllContactsLoaderListener);
        } else if (!mAllContactsLoaderStarted) {
            // Load "all" contacts if not loaded yet.
            getLoaderManager().initLoader(
                    LOADER_ID_ALL_CONTACTS, null, mAllContactsLoaderListener);
        } else if (mSearchFromResultNeeded) {
            startSearchTask(mQueryString, true);
        }
        mAllContactsForceReload = false;
        mSearchFromResultNeeded = false;
        mAllContactsLoaderStarted = true;
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
        mCallLogQueryHandler.fetchAllUnknownCalls();
    }

    public void startCallsQuery() {
        mCallLogAdapter.setLoading(true);
        mCallLogQueryHandler.fetchAllUnknownCalls();
    }

    @VisibleForTesting
    SimpleCallLogAdapter getAdapter() {
        return mCallLogAdapter;
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
			mListener.onContactSelected(mAllContactsAdapter
					.getDataUri(position));
		}
	}

}