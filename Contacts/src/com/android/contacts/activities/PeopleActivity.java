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

package com.android.contacts.activities;

import com.android.contacts.ContactLoader;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.activities.ActionBarAdapter.TabState;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ContactDetailLayoutController;
import com.android.contacts.detail.ContactDetailUpdatesFragment;
import com.android.contacts.detail.ContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.dialpad.DialerFragment;
import com.android.contacts.dialpad.DialerFragment.OnFragmentReadyListener;
import com.android.contacts.dialpad.DialpadFragment;
import com.android.contacts.dialpad.DialpadFragment.OnDightsChangedListener;
import com.android.contacts.group.GroupBrowseListFragment;
import com.android.contacts.group.GroupBrowseListFragment.OnGroupBrowserActionListener;
import com.android.contacts.group.GroupDetailFragment;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.interactions.ImportExportDialogFragment;
import com.android.contacts.interactions.PhoneNumberInteraction;
import com.android.contacts.list.FilteredResultsFragment;
import com.android.contacts.list.ContactBrowseListFragment;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ContactTileAdapter.DisplayType;
import com.android.contacts.list.AccountFilterActivity;
import com.android.contacts.list.ContactTileFrequentFragment;
import com.android.contacts.list.ContactTileListFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsUnavailableFragment;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.list.DirectoryListLoader;
import com.android.contacts.list.OnContactBrowserActionListener;
import com.android.contacts.list.OnContactsUnavailableActionListener;
import com.android.contacts.list.ProviderStatusLoader;
import com.android.contacts.list.ProviderStatusLoader.ProviderStatusListener;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferenceActivity;
import com.android.contacts.preference.DisplayOptionsPreferenceFragment;
import com.android.contacts.sim.SimHelperService;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.AccountPromptUtils;
import com.android.contacts.util.AccountSelectionUtil;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.InternalsAndAccountsMergeAdapter;
import com.android.contacts.util.InternalsListAdapter;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.util.InternalsListAdapter.InternalListFilter;
import com.android.contacts.util.Constants;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.ProviderStatus;
import android.provider.ContactsContract.QuickContact;
import android.provider.Settings;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays a list to browse contacts. For xlarge screens, this also displays a detail-pane on
 * the right.
 */
public class PeopleActivity extends ContactsActivity
        implements View.OnCreateContextMenuListener, ActionBarAdapter.Listener,
        DialogManager.DialogShowingViewActivity,
        ContactListFilterController.ContactListFilterListener, ProviderStatusListener,
        //{Added by yongan.qiu on 2012.6.21 begin.
        DialerFragment.OnFragmentReadyListener, 
        DialpadFragment.OnDightsChangedListener
        //}Added by yongan.qiu end.
        {

    private static final String TAG = "PeopleActivity";

    // These values needs to start at 2. See {@link ContactEntryListFragment}.
    private static final int SUBACTIVITY_NEW_CONTACT = 2;
    private static final int SUBACTIVITY_EDIT_CONTACT = 3;
    private static final int SUBACTIVITY_NEW_GROUP = 4;
    private static final int SUBACTIVITY_EDIT_GROUP = 5;
    private static final int SUBACTIVITY_ACCOUNT_FILTER = 6;

    //{Added by yongan.qiu on 2012-7-16 begin.
    private static final int REQUEST_CODE_PICK_PHONE = 7;
    private static final int REQUEST_CODE_PICK_EMAIL = 8;
    //}Added by yongan.qiu end.

    private final DialogManager mDialogManager = new DialogManager(this);

    private ContactsIntentResolver mIntentResolver;
    private ContactsRequest mRequest;

    private ActionBarAdapter mActionBarAdapter;

    private ContactDetailFragment mContactDetailFragment;
    private ContactDetailUpdatesFragment mContactDetailUpdatesFragment;

    private ContactLoaderFragment mContactDetailLoaderFragment;
    private final ContactDetailLoaderFragmentListener mContactDetailLoaderFragmentListener =
            new ContactDetailLoaderFragmentListener();

    private GroupDetailFragment mGroupDetailFragment;
    private final GroupDetailFragmentListener mGroupDetailFragmentListener =
            new GroupDetailFragmentListener();

    private ContactTileListFragment.Listener mFavoritesFragmentListener =
            new StrequentContactListFragmentListener();

    private ContactListFilterController mContactListFilterController;

    private ContactsUnavailableFragment mContactsUnavailableFragment;
    private ProviderStatusLoader mProviderStatusLoader;
    private int mProviderStatus = -1;

    private boolean mOptionsMenuContactsAvailable;

    /**
     * Showing a list of Contacts. Also used for showing search results in search mode.
     */
    private DefaultContactBrowseListFragment mAllFragment;
    private ContactTileListFragment mFavoritesFragment;
    private ContactTileFrequentFragment mFrequentFragment;
    private GroupBrowseListFragment mGroupsFragment;
    private DialerFragment mDialerFragment;
    //{Added by yongan.qiu on 2012.6.21 begin.
    private FilteredResultsFragment mFilteredResultsFragment;
    //}Added by yongan.qiu end.

    private View mFavoritesView;
    private View mBrowserView;
    private View mDetailsView;

    private View mAddGroupImageView;

    /** ViewPager for swipe, used only on the phone (i.e. one-pane mode) */
    private ViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    private final TabPagerListener mTabPagerListener = new TabPagerListener();

    //add JiangzhouQ 20120624
    private final TouchEventDeliverListener mTouchEventDeliverListener = new TouchEventDeliverListener();
    //end JiangzhouQ 20120624
    
    private ContactDetailLayoutController mContactDetailLayoutController;

    private final Handler mHandler = new Handler();

    /**
     * True if this activity instance is a re-created one.  i.e. set true after orientation change.
     * This is set in {@link #onCreate} for later use in {@link #onStart}.
     */
    private boolean mIsRecreatedInstance;

    /**
     * If {@link #configureFragments(boolean)} is already called.  Used to avoid calling it twice
     * in {@link #onStart}.
     * (This initialization only needs to be done once in onStart() when the Activity was just
     * created from scratch -- i.e. onCreate() was just called)
     */
    private boolean mFragmentInitialized;

    /**
     * Whether or not the current contact filter is valid or not. We need to do a check on
     * start of the app to verify that the user is not in single contact mode. If so, we should
     * dynamically change the filter, unless the incoming intent specifically requested a contact
     * that should be displayed in that mode.
     */
    private boolean mCurrentFilterIsValid;

    /** Sequential ID assigned to each instance; used for logging */
    private final int mInstanceId;
    private static final AtomicInteger sNextInstanceId = new AtomicInteger();

    //begin: added by yunzhou.song
    private boolean mFilterPhones;
    //end: added by yunzhou.song
    
    public PeopleActivity() {
        mInstanceId = sNextInstanceId.getAndIncrement();
        mIntentResolver = new ContactsIntentResolver(this);
        mProviderStatusLoader = new ProviderStatusLoader(this);
    }

    @Override
    public String toString() {
        // Shown on logcat
        return String.format("%s@%d", getClass().getSimpleName(), mInstanceId);
    }

    public boolean areContactsAvailable() {
        //{Modified by yongan.qiu on 2012-7-11 begin.
        //old:
        /*return mProviderStatus == ProviderStatus.STATUS_NORMAL;*/
        //new:
        return (mProviderStatus == ProviderStatus.STATUS_NORMAL
                || AccountTypeManager.getInstance(this).getInternalsAndAccounts(true).size() > 0);
		//}Modified by yongan.qiu end.
    }

    private boolean areContactWritableAccountsAvailable() {
        return ContactsUtils.areContactWritableAccountsAvailable(this);
    }

    private boolean areGroupWritableAccountsAvailable() {
        return ContactsUtils.areGroupWritableAccountsAvailable(this);
    }

    /**
     * Initialize fragments that are (or may not be) in the layout.
     *
     * For the fragments that are in the layout, we initialize them in
     * {@link #createViewsAndFragments(Bundle)} after inflating the layout.
     *
     * However, there are special fragments which may not be in the layout, so we have to do the
     * initialization here.
     * The target fragments are:
     * - {@link ContactDetailFragment} and {@link ContactDetailUpdatesFragment}:  They may not be
     *   in the layout depending on the configuration.  (i.e. portrait)
     * - {@link ContactsUnavailableFragment}: We always create it at runtime.
     */
    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactDetailFragment) {
            mContactDetailFragment = (ContactDetailFragment) fragment;
        } else if (fragment instanceof ContactDetailUpdatesFragment) {
            mContactDetailUpdatesFragment = (ContactDetailUpdatesFragment) fragment;
        } else if (fragment instanceof ContactsUnavailableFragment) {
            mContactsUnavailableFragment = (ContactsUnavailableFragment)fragment;
            mContactsUnavailableFragment.setProviderStatusLoader(mProviderStatusLoader);
            mContactsUnavailableFragment.setOnContactsUnavailableActionListener(
                    new ContactsUnavailableFragmentListener());
        }
        //{Added by yongan.qiu on 2012.6.26 begin.
        else if (fragment instanceof FilteredResultsFragment) {
          mFilteredResultsFragment = (FilteredResultsFragment) fragment;
          mFilteredResultsFragment.setListener(mCallLogPhoneNumberListener);
          if (mContactListFilterController != null
                  && mContactListFilterController.getFilter() != null) {
              mFilteredResultsFragment.setFilter(mContactListFilterController.getFilter());
              }
        }
        //}Added by yongan.qiu end.
    }

    @Override
    protected void onCreate(Bundle savedState) {
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate start");
        }
        super.onCreate(savedState);

        if (!processIntent(false)) {
            finish();
            return;
        }

        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.checkFilterValidity(false);
        mContactListFilterController.addListener(this);

        mIsRecreatedInstance = (savedState != null);
        createViewsAndFragments(savedState);
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate finish");
        }
        //Begin by gangzhou.qi at 2012-7-16 上午11:17:02
        simStateReceiver mSimStateReceiver = new simStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.SIM_READING_STATE);
        registerReceiver(mSimStateReceiver, filter);
        Log.d("^^", "register the broadcast");
        //Ended by gangzhou.qi at 2012-7-16 上午11:17:02
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (!processIntent(true)) {
            finish();
            return;
        }
        mActionBarAdapter.initialize(null, mRequest);

        mContactListFilterController.checkFilterValidity(false);
        mCurrentFilterIsValid = true;

        // Re-configure fragments.
        configureFragments(true /* from request */);
        invalidateOptionsMenuIfNeeded();
    }

    /**
     * Resolve the intent and initialize {@link #mRequest}, and launch another activity if redirect
     * is needed.
     *
     * @param forNewIntent set true if it's called from {@link #onNewIntent(Intent)}.
     * @return {@code true} if {@link PeopleActivity} should continue running.  {@code false}
     *         if it shouldn't, in which case the caller should finish() itself and shouldn't do
     *         farther initialization.
     */
    private boolean processIntent(boolean forNewIntent) {
        boolean isDialtactsIntent = DialtactsActivity.isDialtactsIntent(getIntent());
        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, this + " processIntent: forNewIntent=" + forNewIntent
                    + " intent=" + getIntent() + " request=" + mRequest);
        }
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            return false;
        }

        if(isDialtactsIntent) {
        	return true;
        }
        
        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            return false;
        }

        if (mRequest.getActionCode() == ContactsRequest.ACTION_VIEW_CONTACT
                && !PhoneCapabilityTester.isUsingTwoPanes(this)) {
            redirect = new Intent(this, ContactDetailActivity.class);
            redirect.setAction(Intent.ACTION_VIEW);
            redirect.setData(mRequest.getContactUri());
            startActivity(redirect);
            return false;
        }
        setTitle(mRequest.getActivityTitle());
        return true;
    }

    private void createViewsAndFragments(Bundle savedState) {
        setContentView(R.layout.people_activity);

        final FragmentManager fragmentManager = getFragmentManager();

        // Hide all tabs (the current tab will later be reshown once a tab is selected)
        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Prepare the fragments which are used both on 1-pane and on 2-pane.
        boolean isUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(this);
        if (isUsingTwoPanes) {
            mFavoritesFragment = getFragment(R.id.favorites_fragment);
            mGroupsFragment = getFragment(R.id.groups_fragment);
            mAllFragment = getFragment(R.id.all_fragment);
            mDialerFragment = getFragment(R.id.dialer_fragment);
            //{Added by yongan.qiu on 2012.6.21 begin.
            mFilteredResultsFragment = getFragment(R.id.filtered_results_fragment);
            //}Added by yongan.qiu end.

        } else {
            mTabPager = getView(R.id.tab_pager);
            mTabPagerAdapter = new TabPagerAdapter();
            mTabPager.setAdapter(mTabPagerAdapter);
            mTabPager.setOnPageChangeListener(mTabPagerListener);
            mTabPager.setOnPageElementScrollListener(mTouchEventDeliverListener);

            final String FAVORITE_TAG = "tab-pager-favorite";
            final String GROUPS_TAG = "tab-pager-groups";
            final String ALL_TAG = "tab-pager-all";
            final String DIALER_TAG = "tab-pager-blank";

            // Create the fragments and add as children of the view pager.
            // The pager adapter will only change the visibility; it'll never create/destroy
            // fragments.
            // However, if it's after screen rotation, the fragments have been re-created by
            // the fragment manager, so first see if there're already the target fragments
            // existing.
            mFavoritesFragment = (ContactTileListFragment)
                    fragmentManager.findFragmentByTag(FAVORITE_TAG);
            mGroupsFragment = (GroupBrowseListFragment)
                    fragmentManager.findFragmentByTag(GROUPS_TAG);
            mAllFragment = (DefaultContactBrowseListFragment)
                    fragmentManager.findFragmentByTag(ALL_TAG);
            mDialerFragment = (DialerFragment)
                    fragmentManager.findFragmentByTag(DIALER_TAG);
            //{Added by yongan.qiu on 2012.6.21 begin.
            mFilteredResultsFragment = (FilteredResultsFragment)
                    fragmentManager.findFragmentById(R.id.filtered_results_fragment);
            //}Added by yongan.qiu end.

            if (mFavoritesFragment == null) {
                mFavoritesFragment = new ContactTileListFragment();
                mGroupsFragment = new GroupBrowseListFragment();
                mAllFragment = new DefaultContactBrowseListFragment();
                mDialerFragment = new DialerFragment();
                //{Added by yongan.qiu on 2012.6.21 begin.
                mDialerFragment.setFragmentReadyListener(this);
                //}Added by yongan.qiu end.

                transaction.add(R.id.tab_pager, mFavoritesFragment, FAVORITE_TAG);
                transaction.add(R.id.tab_pager, mGroupsFragment, GROUPS_TAG);
                transaction.add(R.id.tab_pager, mAllFragment, ALL_TAG);
                transaction.add(R.id.tab_pager, mDialerFragment, DIALER_TAG);
            }
        }

        mFavoritesFragment.setListener(mFavoritesFragmentListener);

        mAllFragment.setOnContactListActionListener(new ContactBrowserActionListener());
        //begin: added by yunzhou.song
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilterPhones = prefs.getBoolean(Constants.PREF_KEY_FILTES_PHONES, false);
        //end: added by yunzhou.song

        mGroupsFragment.setListener(new GroupBrowserActionListener());

        // Hide all fragments for now.  We adjust visibility when we get onSelectedTabChanged()
        // from ActionBarAdapter.
        transaction.hide(mFavoritesFragment);
        transaction.hide(mGroupsFragment);
        transaction.hide(mAllFragment);
        transaction.hide(mDialerFragment);

        if (isUsingTwoPanes) {
            // Prepare 2-pane only fragments/views...

            // Container views for fragments
            mFavoritesView = getView(R.id.favorites_view);
            mDetailsView = getView(R.id.details_view);
            mBrowserView = getView(R.id.browse_view);

            // 2-pane only fragments
            mFrequentFragment = getFragment(R.id.frequent_fragment);
            mFrequentFragment.setListener(mFavoritesFragmentListener);
            mFrequentFragment.setDisplayType(DisplayType.FREQUENT_ONLY);
            mFrequentFragment.enableQuickContact(true);

            mContactDetailLoaderFragment = getFragment(R.id.contact_detail_loader_fragment);
            mContactDetailLoaderFragment.setListener(mContactDetailLoaderFragmentListener);

            mGroupDetailFragment = getFragment(R.id.group_detail_fragment);
            mGroupDetailFragment.setListener(mGroupDetailFragmentListener);
            mGroupDetailFragment.setQuickContact(true);

            if (mContactDetailFragment != null) {
                transaction.hide(mContactDetailFragment);
            }
            transaction.hide(mGroupDetailFragment);

            // Configure contact details
            mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
                    getFragmentManager(), findViewById(R.id.contact_detail_container),
                    new ContactDetailFragmentListener());
        }
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();

        // Setting Properties after fragment is created
        if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
            mFavoritesFragment.enableQuickContact(true);
            mFavoritesFragment.setDisplayType(DisplayType.STARRED_ONLY);
        } else {
            mFavoritesFragment.setDisplayType(DisplayType.STARRED_ONLY);
        }

        // Configure action bar
        mActionBarAdapter = new ActionBarAdapter(this, this, getActionBar(), isUsingTwoPanes);
        mActionBarAdapter.initialize(savedState, mRequest);

        invalidateOptionsMenuIfNeeded();
    }

    @Override
    protected void onStart() {
        if (!mFragmentInitialized) {
            mFragmentInitialized = true;
            /* Configure fragments if we haven't.
             *
             * Note it's a one-shot initialization, so we want to do this in {@link #onCreate}.
             *
             * However, because this method may indirectly touch views in fragments but fragments
             * created in {@link #configureContentView} using a {@link FragmentTransaction} will NOT
             * have views until {@link Activity#onCreate} finishes (they would if they were inflated
             * from a layout), we need to do it here in {@link #onStart()}.
             *
             * (When {@link Fragment#onCreateView} is called is different in the former case and
             * in the latter case, unfortunately.)
             *
             * Also, we skip most of the work in it if the activity is a re-created one.
             * (so the argument.)
             */
            configureFragments(!mIsRecreatedInstance);
        } else if (PhoneCapabilityTester.isUsingTwoPanes(this) && !mCurrentFilterIsValid) {
            // We only want to do the filter check in onStart for wide screen devices where it
            // is often possible to get into single contact mode. Only do this check if
            // the filter hasn't already been set properly (i.e. onCreate or onActivityResult).

            // Since there is only one {@link ContactListFilterController} across multiple
            // activity instances, make sure the filter controller is in sync withthe current
            // contact list fragment filter.
            // TODO: Clean this up. Perhaps change {@link ContactListFilterController} to not be a
            // singleton?
            mContactListFilterController.setContactListFilter(mAllFragment.getFilter(), true);
            mContactListFilterController.checkFilterValidity(true);
            mCurrentFilterIsValid = true;
        }
        super.onStart();
    }

    @Override
    protected void onPause() {
        mOptionsMenuContactsAvailable = false;

        mProviderStatus = -1;
        mProviderStatusLoader.setProviderStatusListener(null);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mProviderStatusLoader.setProviderStatusListener(this);
        showContactsUnavailableFragmentIfNecessary();

        // Re-register the listener, which may have been cleared when onSaveInstanceState was
        // called.  See also: onSaveInstanceState
        mActionBarAdapter.setListener(this);
        if (mTabPager != null) {
            mTabPager.setOnPageChangeListener(mTabPagerListener);
            mTabPager.setOnPageElementScrollListener(mTouchEventDeliverListener);
        }
        // Current tab may have changed since the last onSaveInstanceState().  Make sure
        // the actual contents match the tab.
        updateFragmentsVisibility();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCurrentFilterIsValid = false;
    }

    @Override
    protected void onDestroy() {
        // Some of variables will be null if this Activity redirects Intent.
        // See also onCreate() or other methods called during the Activity's initialization.
        if (mActionBarAdapter != null) {
            mActionBarAdapter.setListener(null);
        }
        if (mContactListFilterController != null) {
            mContactListFilterController.removeListener(this);
        }

        super.onDestroy();
    }

    private void configureFragments(boolean fromRequest) {
        if (fromRequest) {
            ContactListFilter filter = null;
            int actionCode = mRequest.getActionCode();
            boolean searchMode = mRequest.isSearchMode();
            TabState tabToOpen = null;
            switch (actionCode) {
                case ContactsRequest.ACTION_ALL_CONTACTS:
                    filter = ContactListFilter.createFilterWithType(
                            ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS);
                    tabToOpen = TabState.ALL;
                    break;
                case ContactsRequest.ACTION_CONTACTS_WITH_PHONES:
                    filter = ContactListFilter.createFilterWithType(
                            ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY);
                    tabToOpen = TabState.ALL;
                    break;

                case ContactsRequest.ACTION_FREQUENT:
                case ContactsRequest.ACTION_STREQUENT:
                case ContactsRequest.ACTION_STARRED:
                    tabToOpen = TabState.FAVORITES;
                    break;
                case ContactsRequest.ACTION_VIEW_CONTACT:
                    // We redirect this intent to the detail activity on 1-pane, so we don't get
                    // here.  It's only for 2-pane.
                    tabToOpen = TabState.ALL;
                    break;
                case ContactsRequest.ACTION_GROUP:
                    tabToOpen = TabState.GROUPS;
                    break;
                case ContactsRequest.ACTION_DIAL:
                case ContactsRequest.ACTION_VIEW_CALL_LOG:
                    tabToOpen = TabState.DIALER;
                    break;
            }
            if (tabToOpen != null) {
                mActionBarAdapter.setCurrentTab(tabToOpen);
            }

            if (filter != null) {
                mContactListFilterController.setContactListFilter(filter, false);
                searchMode = false;
            }

            if (mRequest.getContactUri() != null) {
                searchMode = false;
            }

            mActionBarAdapter.setSearchMode(searchMode);
            configureDialerFragment();
            configureContactListFragmentForRequest();
        }

        configureContactListFragment();
        configureGroupListFragment();

        invalidateOptionsMenuIfNeeded();
    }

    @Override
    public void onContactListFilterChanged() {
        boolean doInvalidateOptionsMenu = false;

        if (mAllFragment != null && mAllFragment.isAdded()) {
            mAllFragment.setFilter(mContactListFilterController.getFilter());
            doInvalidateOptionsMenu = true;
        }

        //{Added by yongan.qiu on 2012.6.21 begin.
        if (mFilteredResultsFragment != null && mFilteredResultsFragment.isAdded()) {
            mFilteredResultsFragment.setFilter(mContactListFilterController.getFilter());
            doInvalidateOptionsMenu = true;
        }
        //}Added by yongan.qiu end.

        if (doInvalidateOptionsMenu) {
            invalidateOptionsMenuIfNeeded();
        }
    }

    private void setupContactDetailFragment(final Uri contactLookupUri) {
        mContactDetailLoaderFragment.loadUri(contactLookupUri);
        invalidateOptionsMenuIfNeeded();
    }

    private void setupGroupDetailFragment(Uri groupUri) {
        mGroupDetailFragment.loadGroup(groupUri);
        invalidateOptionsMenuIfNeeded();
    }

    /**
     * Handler for action bar actions.
     */
    @Override
    public void onAction(Action action) {
        switch (action) {
            case START_SEARCH_MODE:
                // Tell the fragments that we're in the search mode
                configureFragments(false /* from request */);
                updateFragmentsVisibility();
                invalidateOptionsMenu();
                break;
            case STOP_SEARCH_MODE:
                setQueryTextToFragment("");
                updateFragmentsVisibility();
                invalidateOptionsMenu();
                break;
            case CHANGE_SEARCH_QUERY:
                setQueryTextToFragment(mActionBarAdapter.getQueryString());
                break;
            default:
                throw new IllegalStateException("Unkonwn ActionBarAdapter action: " + action);
        }
    }

    @Override
    public void onSelectedTabChanged() {
        updateFragmentsVisibility();
    }

    /**
     * Updates the fragment/view visibility according to the current mode, such as
     * {@link ActionBarAdapter#isSearchMode()} and {@link ActionBarAdapter#getCurrentTab()}.
     */
    private void updateFragmentsVisibility() {
        TabState tab = mActionBarAdapter.getCurrentTab();

        // We use ViewPager on 1-pane.
        if (!PhoneCapabilityTester.isUsingTwoPanes(this)) {
            if (mActionBarAdapter.isSearchMode()) {
                mTabPagerAdapter.setSearchMode(true);
            } else {
                // No smooth scrolling if quitting from the search mode.
                final boolean wasSearchMode = mTabPagerAdapter.isSearchMode();
                mTabPagerAdapter.setSearchMode(false);
                int tabIndex = tab.ordinal();
                if (mTabPager.getCurrentItem() != tabIndex) {
                    mTabPager.setCurrentItem(tabIndex, !wasSearchMode);
                }
            }
            invalidateOptionsMenu();
            showEmptyStateForTab(tab);
            if (tab == TabState.GROUPS) {
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
            }
            return;
        }

        // for the tablet...

        // If in search mode, we use the all list + contact details to show the result.
        if (mActionBarAdapter.isSearchMode()) {
            tab = TabState.ALL;
        }
        switch (tab) {
            case FAVORITES:
                mFavoritesView.setVisibility(View.VISIBLE);
                mBrowserView.setVisibility(View.GONE);
                mDetailsView.setVisibility(View.GONE);
                break;
            case GROUPS:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.VISIBLE);
                mDetailsView.setVisibility(View.VISIBLE);
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                break;
            case ALL:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.VISIBLE);
                mDetailsView.setVisibility(View.VISIBLE);
                break;
            case DIALER:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.GONE);
                mDetailsView.setVisibility(View.GONE);
                break;
        }
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // Note mContactDetailLoaderFragment is an invisible fragment, but we still have to show/
        // hide it so its options menu will be shown/hidden.
        switch (tab) {
            case FAVORITES:
                showFragment(ft, mFavoritesFragment);
                showFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                hideFragment(ft, mDialerFragment);
                break;
            case ALL:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                showFragment(ft, mAllFragment);
                showFragment(ft, mContactDetailLoaderFragment);
                showFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                hideFragment(ft, mDialerFragment);
                break;
            case GROUPS:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                showFragment(ft, mGroupsFragment);
                showFragment(ft, mGroupDetailFragment);
                hideFragment(ft, mDialerFragment);
                break;
            case DIALER:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                showFragment(ft, mDialerFragment);
                break;
        }
        if (!ft.isEmpty()) {
            ft.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
            // When switching tabs, we need to invalidate options menu, but executing a
            // fragment transaction does it implicitly.  We don't have to call invalidateOptionsMenu
            // manually.
        }
        showEmptyStateForTab(tab);
    }

    private void showEmptyStateForTab(TabState tab) {
        if (mContactsUnavailableFragment != null) {
            switch (tab) {
                case FAVORITES:
                    mContactsUnavailableFragment.setMessageText(
                            R.string.listTotalAllContactsZeroStarred, -1);
                    break;
                case GROUPS:
                    mContactsUnavailableFragment.setMessageText(R.string.noGroups,
                            areGroupWritableAccountsAvailable() ? -1 : R.string.noAccounts);
                    break;
                case ALL:
                    mContactsUnavailableFragment.setMessageText(R.string.noContacts, -1);
                    break;
            }
            FragmentManager fragmentManager = getFragmentManager();
            switch(tab) {
                case FAVORITES:
                case GROUPS:
                case ALL:
                    if(mContactsUnavailableFragment.isHidden()) {
                        FragmentTransaction transaction = fragmentManager.beginTransaction();
                        transaction.show(mContactsUnavailableFragment);
                        transaction.commitAllowingStateLoss();
                        fragmentManager.executePendingTransactions();
                    }
                    break;
                case DIALER:
                    if(!mContactsUnavailableFragment.isHidden()) {
                        FragmentTransaction transaction = fragmentManager.beginTransaction();
                        transaction.hide(mContactsUnavailableFragment);
                        transaction.commitAllowingStateLoss();
                        fragmentManager.executePendingTransactions();
                    }
                    break;
            }
        }
    }

    //added JiangzhouQ 20120627
    private class TouchEventDeliverListener implements ViewPager.OnTouchEventDeliverListener{
    	@Override
    	public boolean isTouchEventDelivered(){
    		if(mDialerFragment.checkCalllogPullOut()){
    			return true;
    		}
    		return false;
    	}
    	
    	@Override
    	public int getLeftDragger(){
    		return mDialerFragment.getLeftDragger();
    	}
    }
    
    //ended JiangzhouQ 20120627
    private class TabPagerListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Make sure not in the search mode, in which case position != TabState.ordinal().
            if (!mTabPagerAdapter.isSearchMode()) {
                TabState selectedTab = TabState.fromInt(position);
                mActionBarAdapter.setCurrentTab(selectedTab, false);
                showEmptyStateForTab(selectedTab);
                if (selectedTab == TabState.GROUPS) {
                    mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                }
                invalidateOptionsMenu();
            }
        }
    }

    /**
     * Adapter for the {@link ViewPager}.  Unlike {@link FragmentPagerAdapter},
     * {@link #instantiateItem} returns existing fragments, and {@link #instantiateItem}/
     * {@link #destroyItem} show/hide fragments instead of attaching/detaching.
     *
     * In search mode, we always show the "all" fragment, and disable the swipe.  We change the
     * number of items to 1 to disable the swipe.
     *
     * TODO figure out a more straight way to disable swipe.
     */
    private class TabPagerAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;

        private boolean mTabPagerAdapterSearchMode;

        private Fragment mCurrentPrimaryItem;

        public TabPagerAdapter() {
            mFragmentManager = getFragmentManager();
        }

        public boolean isSearchMode() {
            return mTabPagerAdapterSearchMode;
        }

        public void setSearchMode(boolean searchMode) {
            if (searchMode == mTabPagerAdapterSearchMode) {
                return;
            }
            mTabPagerAdapterSearchMode = searchMode;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabPagerAdapterSearchMode ? 1 : TabState.values().length;
        }

        /** Gets called when the number of items changes. */
        @Override
        public int getItemPosition(Object object) {
            if (mTabPagerAdapterSearchMode) {
                if (object == mAllFragment) {
                    return 0; // Only 1 page in search mode
                }
            } else {
                if (object == mFavoritesFragment) {
                    return TabState.FAVORITES.ordinal();
                }
                if (object == mAllFragment) {
                    return TabState.ALL.ordinal();
                }
                if (object == mGroupsFragment) {
                    return TabState.GROUPS.ordinal();
                }
                if (object == mDialerFragment) {
                    return TabState.DIALER.ordinal();
                }
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(View container) {
        }

        private Fragment getFragment(int position) {
            if (mTabPagerAdapterSearchMode) {
                if (position == 0) {
                    return mAllFragment;
                }
            } else {
                if (position == TabState.FAVORITES.ordinal()) {
                    return mFavoritesFragment;
                } else if (position == TabState.GROUPS.ordinal()) {
                    return mGroupsFragment;
                } else if (position == TabState.ALL.ordinal()) {
                    return mAllFragment;
                } else if (position == TabState.DIALER.ordinal()) {
                    return mDialerFragment;
                }
            }
            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public Object instantiateItem(View container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            mCurTransaction.show(f);

            // Non primary pages are not visible.
            f.setUserVisibleHint(f == mCurrentPrimaryItem);
            return f;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(View container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(View container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }

    private void setQueryTextToFragment(String query) {
        mAllFragment.setQueryString(query, true);
        mAllFragment.setVisibleScrollbarEnabled(!mAllFragment.isSearchMode());
    }
    
    private void configureDialerFragment() {
        int actionCode = mRequest.getActionCode();
        switch(actionCode) {
            case ContactsRequest.ACTION_DIAL:
                mDialerFragment.setFragmentShow(R.id.dialpad_fragment, 0, 0, true);
                break;
            case ContactsRequest.ACTION_VIEW_CALL_LOG:
                mDialerFragment.setFragmentShow(R.id.dialpad_fragment, 0, 0, false);
                break;
        }
    }

    private void configureContactListFragmentForRequest() {
        Uri contactUri = mRequest.getContactUri();
        if (contactUri != null) {
            // For an incoming request, explicitly require a selection if we are on 2-pane UI,
            // (i.e. even if we view the same selected contact, the contact may no longer be
            // in the list, so we must refresh the list).
            if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
                mAllFragment.setSelectionRequired(true);
            }
            mAllFragment.setSelectedContactUri(contactUri);
        }

        //begin: added by yunzhou.song
        ContactListFilter filter = mContactListFilterController.getFilter();
        if (filter == null
        		|| filter.filterType != ContactListFilter.FILTER_TYPE_CUSTOM) {
        	mFilterPhones = false;
        }
        mAllFragment.setFilterPhones(mFilterPhones);
        mAllFragment.setFilter(filter);
        //end: added by yunzhou.song
        //begin: remarked by yunzhou.song
        //mAllFragment.setFilter(mContactListFilterController.getFilter());
        //end: remarked by yunzhou.song
        //{Added by yongan.qiu on 2012.6.21 begin.
        if (mFilteredResultsFragment != null && mFilteredResultsFragment.isAdded()) {
            mFilteredResultsFragment.setFilter(filter);
        }
        //}Added by yongan.qiu end.
        setQueryTextToFragment(mActionBarAdapter.getQueryString());

        if (mRequest.isDirectorySearchEnabled()) {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DEFAULT);
        } else {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
        }
    }

    private void configureContactListFragment() {
        // Filter may be changed when this Activity is in background.
        //begin: added by yunzhou.song
        ContactListFilter filter = mContactListFilterController.getFilter();
        if (filter == null
        		|| filter.filterType != ContactListFilter.FILTER_TYPE_CUSTOM) {
        	mFilterPhones = false;
        }
        mAllFragment.setFilterPhones(mFilterPhones);
        mAllFragment.setFilter(filter);
        //end: added by yunzhou.song
        //begin: remarked by yunzhou.song
        //mAllFragment.setFilter(mContactListFilterController.getFilter());
        //end: remarked by yunzhou.song
        //{Added by yongan.qiu on 2012.6.21 begin.
        if (mFilteredResultsFragment != null && mFilteredResultsFragment.isAdded()) {
            mFilteredResultsFragment.setFilter(filter);
        }
        //}Added by yongan.qiu end.

        final boolean useTwoPane = PhoneCapabilityTester.isUsingTwoPanes(this);
        mAllFragment.setVerticalScrollbarPosition(
                useTwoPane
                        ? View.SCROLLBAR_POSITION_LEFT
                        : View.SCROLLBAR_POSITION_RIGHT);
        mAllFragment.setSelectionVisible(useTwoPane);
        mAllFragment.setQuickContactEnabled(!useTwoPane);
    }

    private void configureGroupListFragment() {
        final boolean useTwoPane = PhoneCapabilityTester.isUsingTwoPanes(this);
        mGroupsFragment.setVerticalScrollbarPosition(
                useTwoPane
                        ? View.SCROLLBAR_POSITION_LEFT
                        : View.SCROLLBAR_POSITION_RIGHT);
        mGroupsFragment.setSelectionVisible(useTwoPane);
    }

    @Override
    public void onProviderStatusChange() {
        showContactsUnavailableFragmentIfNecessary();
    }

    private void showContactsUnavailableFragmentIfNecessary() {
        int providerStatus = mProviderStatusLoader.getProviderStatus();
        if (providerStatus == mProviderStatus) {
            return;
        }

        mProviderStatus = providerStatus;

        View contactsUnavailableView = findViewById(R.id.contacts_unavailable_view);
        View mainView = findViewById(R.id.main_view);

        //{Modified by yongan.qiu on 2012-7-11 begin.
        //old:
        /*if (mProviderStatus == ProviderStatus.STATUS_NORMAL) {*/
        //new:
        if (areContactsAvailable()) {
        //}Modified by yongan.qiu end.
            contactsUnavailableView.setVisibility(View.GONE);
            if (mainView != null) {
                mainView.setVisibility(View.VISIBLE);
            }
            if (mAllFragment != null) {
                mAllFragment.setEnabled(true);
            }
        } else {
            // If there are no accounts on the device and we should show the "no account" prompt
            // (based on {@link SharedPreferences}), then launch the account setup activity so the
            // user can sign-in or create an account.
            if (!areContactWritableAccountsAvailable() &&
                    AccountPromptUtils.shouldShowAccountPrompt(this)) {
                AccountPromptUtils.launchAccountPrompt(this);
                return;
            }

            // Otherwise, continue setting up the page so that the user can still use the app
            // without an account.
            if (mAllFragment != null) {
                mAllFragment.setEnabled(false);
            }
            if (mContactsUnavailableFragment == null) {
                mContactsUnavailableFragment = new ContactsUnavailableFragment();
                mContactsUnavailableFragment.setProviderStatusLoader(mProviderStatusLoader);
                mContactsUnavailableFragment.setOnContactsUnavailableActionListener(
                        new ContactsUnavailableFragmentListener());
                getFragmentManager().beginTransaction()
                        .replace(R.id.contacts_unavailable_container, mContactsUnavailableFragment)
                        .commitAllowingStateLoss();
            } else {
                mContactsUnavailableFragment.update();
            }
            contactsUnavailableView.setVisibility(View.VISIBLE);
            if (mainView != null) {
                mainView.setVisibility(View.INVISIBLE);
            }

            TabState tab = mActionBarAdapter.getCurrentTab();
            showEmptyStateForTab(tab);
        }

        invalidateOptionsMenuIfNeeded();
    }

    private final class ContactBrowserActionListener implements OnContactBrowserActionListener {

        @Override
        public void onSelectionChange() {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupContactDetailFragment(mAllFragment.getSelectedContactUri());
            }
        }

        @Override
        public void onViewContactAction(Uri contactLookupUri) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupContactDetailFragment(contactLookupUri);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, contactLookupUri);
                // In search mode, the "up" affordance in the contact detail page should return the
                // user to the search results, so finish the activity when that button is selected.
                if (mActionBarAdapter.isSearchMode()) {
                    intent.putExtra(
                            ContactDetailActivity.INTENT_KEY_FINISH_ACTIVITY_ON_UP_SELECTED, true);
                }
                startActivity(intent);
            }
        }

        @Override
        public void onCreateNewContactAction() {
            Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivity(intent);
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            startActivityForResult(intent, SUBACTIVITY_EDIT_CONTACT);
        }

        @Override
        public void onAddToFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 1);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onRemoveFromFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 0);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onCallContactAction(Uri contactUri) {
            PhoneNumberInteraction.startInteractionForPhoneCall(PeopleActivity.this, contactUri);
        }

        @Override
        public void onSmsContactAction(Uri contactUri) {
            PhoneNumberInteraction.startInteractionForTextMessage(PeopleActivity.this, contactUri);
        }

        @Override
        public void onDeleteContactAction(Uri contactUri) {
            ContactDeletionInteraction.start(PeopleActivity.this, contactUri, false);
        }

        @Override
        public void onFinishAction() {
            onBackPressed();
        }

        @Override
        public void onInvalidSelection() {
            ContactListFilter filter;
            ContactListFilter currentFilter = mAllFragment.getFilter();
            if (currentFilter != null
                    && currentFilter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
                filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS);
                mAllFragment.setFilter(filter);
            } else {
                filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_SINGLE_CONTACT);
                mAllFragment.setFilter(filter, false);
            }
            mContactListFilterController.setContactListFilter(filter, true);
        }
    }

    private class ContactDetailLoaderFragmentListener implements ContactLoaderFragmentListener {
        @Override
        public void onContactNotFound() {
            // Nothing needs to be done here
        }

        @Override
        public void onDetailsLoaded(final ContactLoader.Result result) {
            if (result == null) {
                // Nothing is loaded. Show empty state.
                mContactDetailLayoutController.showEmptyState();
                return;
            }
            // Since {@link FragmentTransaction}s cannot be done in the onLoadFinished() of the
            // {@link LoaderCallbacks}, then post this {@link Runnable} to the {@link Handler}
            // on the main thread to execute later.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // If the activity is destroyed (or will be destroyed soon), don't update the UI
                    if (isFinishing()) {
                        return;
                    }
                    mContactDetailLayoutController.setContactData(result);
                }
            });
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            startActivityForResult(intent, SUBACTIVITY_EDIT_CONTACT);
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
            ContactDeletionInteraction.start(PeopleActivity.this, contactUri, false);
        }
    }

    public class ContactDetailFragmentListener implements ContactDetailFragment.Listener {
        @Override
        public void onItemClicked(Intent intent) {
            if (intent == null) {
                return;
            }
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found for intent: " + intent);
            }
        }

        @Override
        public void onCreateRawContactRequested(ArrayList<ContentValues> values,
                AccountWithDataSet account) {
            Toast.makeText(PeopleActivity.this, R.string.toast_making_personal_copy,
                    Toast.LENGTH_LONG).show();
            Intent serviceIntent = ContactSaveService.createNewRawContactIntent(
                    PeopleActivity.this, values, account,
                    PeopleActivity.class, Intent.ACTION_VIEW);
            startService(serviceIntent);
        }
    }

    private class ContactsUnavailableFragmentListener
            implements OnContactsUnavailableActionListener {

        @Override
        public void onCreateNewContactAction() {
            startActivity(new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
        }

        @Override
        public void onAddAccountAction() {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Settings.EXTRA_AUTHORITIES,
                    new String[] { ContactsContract.AUTHORITY });
            startActivity(intent);
        }

        @Override
        public void onImportContactsFromFileAction() {
            ImportExportDialogFragment.show(getFragmentManager());
        }

        @Override
        public void onFreeInternalStorageAction() {
            startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        }
    }

    private final class StrequentContactListFragmentListener
            implements ContactTileListFragment.Listener {
        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                QuickContact.showQuickContact(PeopleActivity.this, targetRect, contactUri, 0, null);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, contactUri));
            }
        }
    }

    private final class GroupBrowserActionListener implements OnGroupBrowserActionListener {

        @Override
        public void onViewGroupAction(Uri groupUri) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupGroupDetailFragment(groupUri);
            } else {
                Intent intent = new Intent(PeopleActivity.this, GroupDetailActivity.class);
                intent.setData(groupUri);
                startActivity(intent);
            }
        }
    }

    private class GroupDetailFragmentListener implements GroupDetailFragment.Listener {
        @Override
        public void onGroupSizeUpdated(String size) {
            // Nothing needs to be done here because the size will be displayed in the detail
            // fragment
        }

        @Override
        public void onGroupTitleUpdated(String title) {
            // Nothing needs to be done here because the title will be displayed in the detail
            // fragment
        }

        @Override
        public void onAccountTypeUpdated(String accountTypeString, String dataSet) {
            // Nothing needs to be done here because the group source will be displayed in the
            // detail fragment
        }

        @Override
        public void onEditRequested(Uri groupUri) {
            final Intent intent = new Intent(PeopleActivity.this, GroupEditorActivity.class);
            intent.setData(groupUri);
            intent.setAction(Intent.ACTION_EDIT);
            startActivityForResult(intent, SUBACTIVITY_EDIT_GROUP);
        }

        @Override
        public void onContactSelected(Uri contactUri) {
            // Nothing needs to be done here because either quickcontact will be displayed
            // or activity will take care of selection
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!areContactsAvailable()) {
            // If contacts aren't available, hide all menu items.
            return false;
        }
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);

        // On narrow screens we specify a NEW group button in the {@link ActionBar}, so that
        // it can be in the overflow menu. On wide screens, we use a custom view because we need
        // its location for anchoring the account-selector popup.
        final MenuItem addGroup = menu.findItem(R.id.menu_custom_add_group);
        if (addGroup != null) {
            mAddGroupImageView = getLayoutInflater().inflate(
                    R.layout.add_group_menu_item, null, false);
            mAddGroupImageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewGroupWithAccountDisambiguation();
                }
            });
            addGroup.setActionView(mAddGroupImageView);
        }
        return true;
    }

    private void invalidateOptionsMenuIfNeeded() {
        if (isOptionsMenuChanged()) {
            invalidateOptionsMenu();
        }
    }

    public boolean isOptionsMenuChanged() {
        if (mOptionsMenuContactsAvailable != areContactsAvailable()) {
            return true;
        }

        if (mAllFragment != null && mAllFragment.isOptionsMenuChanged()) {
            return true;
        }

        if (mContactDetailLoaderFragment != null &&
                mContactDetailLoaderFragment.isOptionsMenuChanged()) {
            return true;
        }

        if (mGroupDetailFragment != null && mGroupDetailFragment.isOptionsMenuChanged()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuContactsAvailable = areContactsAvailable();
        if (!mOptionsMenuContactsAvailable) {
            return false;
        }

        final MenuItem addContactMenu = menu.findItem(R.id.menu_add_contact);
        //{Added by yongan.qiu on 2012-7-16 begin.
        final MenuItem messageMenu = menu.findItem(R.id.menu_msg);
        final MenuItem emailMenu = menu.findItem(R.id.menu_email);
        //}Added by yongan.qiu end.
        final MenuItem contactsFilterMenu = menu.findItem(R.id.menu_contacts_filter);
        final MenuItem dialpadMenu = menu.findItem(R.id.menu_dialpad);
        final MenuItem callTypeMenu = menu.findItem(R.id.menu_call_type);
        final MenuItem callSettingsMenu = menu.findItem(R.id.menu_call_settings);

        MenuItem addGroupMenu = menu.findItem(R.id.menu_add_group);
        if (addGroupMenu == null) {
            addGroupMenu = menu.findItem(R.id.menu_custom_add_group);
        }

        final boolean isSearchMode = mActionBarAdapter.isSearchMode();
        if (isSearchMode) {
            addContactMenu.setVisible(false);
            addGroupMenu.setVisible(false);
            //{Added by yongan.qiu on 2012-7-16 begin.
            messageMenu.setVisible(false);
            emailMenu.setVisible(false);
            //}Added by yongan.qiu end.
            contactsFilterMenu.setVisible(false);
            dialpadMenu.setVisible(false);
            callTypeMenu.setVisible(false);
            callSettingsMenu.setVisible(false);
            final boolean showMiscOptions = !isSearchMode;
            makeMenuItemVisible(menu, R.id.menu_search, showMiscOptions);
            makeMenuItemVisible(menu, R.id.menu_import_export, showMiscOptions);
            makeMenuItemVisible(menu, R.id.menu_accounts, showMiscOptions);
            makeMenuItemVisible(menu, R.id.menu_display_settings,
                    showMiscOptions && !ContactsPreferenceActivity.isEmpty(this));
            makeMenuItemVisible(menu, R.id.menu_sim_contacts, showMiscOptions);
        } else {
            switch (mActionBarAdapter.getCurrentTab()) {
                case FAVORITES:
                    addContactMenu.setVisible(false);
                    addGroupMenu.setVisible(false);
                    //{Added by yongan.qiu on 2012-7-16 begin.
                    messageMenu.setVisible(false);
                    emailMenu.setVisible(false);
                    //}Added by yongan.qiu end.
                    contactsFilterMenu.setVisible(false);
                    dialpadMenu.setVisible(false);
                    callTypeMenu.setVisible(false);
                    callSettingsMenu.setVisible(false);
                    makeMenuItemVisible(menu, R.id.menu_search, true);
                    makeMenuItemVisible(menu, R.id.menu_import_export, false);
                    makeMenuItemVisible(menu, R.id.menu_accounts, false);
                    makeMenuItemVisible(menu, R.id.menu_display_settings, false);
                    makeMenuItemVisible(menu, R.id.menu_sim_contacts, false);
                  //<!-Added by gangzhou.qi at 2012-7-27
                    mTabPager.mSlipMenuRelativeLayoutTurnOn = true;
                    Log.d(TAG, "mTabPager.mSlipMenuRelativeLayoutTurnOn = true;");
                    //Added by gangzhou.qi at 2012-7-27 -!>
                    break;
                case ALL:
                    addContactMenu.setVisible(true);
                    addGroupMenu.setVisible(false);
                    //{Added by yongan.qiu on 2012-7-16 begin.
                    messageMenu.setVisible(true);
                    emailMenu.setVisible(true);
                    //}Added by yongan.qiu end.
                    contactsFilterMenu.setVisible(true);
                    dialpadMenu.setVisible(false);
                    callTypeMenu.setVisible(false);
                    callSettingsMenu.setVisible(false);
                    makeMenuItemVisible(menu, R.id.menu_search, true);
                    makeMenuItemVisible(menu, R.id.menu_import_export, true);
                    makeMenuItemVisible(menu, R.id.menu_accounts, true);
                    makeMenuItemVisible(menu, R.id.menu_display_settings, !ContactsPreferenceActivity.isEmpty(this));
                    makeMenuItemVisible(menu, R.id.menu_sim_contacts, true);
                  //<!-Added by gangzhou.qi at 2012-7-27
                    mTabPager.mSlipMenuRelativeLayoutTurnOn = true;
                    Log.d(TAG, "mTabPager.mSlipMenuRelativeLayoutTurnOn = true;");
                    //Added by gangzhou.qi at 2012-7-27 -!>
                    break;
                case GROUPS:
                    // Do not display the "new group" button if no accounts are available
                    if (areGroupWritableAccountsAvailable()) {
                        addGroupMenu.setVisible(true);
                    } else {
                        addGroupMenu.setVisible(false);
                    }
                    addContactMenu.setVisible(false);
                    //{Added by yongan.qiu on 2012-7-16 begin.
                    messageMenu.setVisible(false);
                    emailMenu.setVisible(false);
                    //}Added by yongan.qiu end.
                    contactsFilterMenu.setVisible(false);
                    dialpadMenu.setVisible(false);
                    callTypeMenu.setVisible(false);
                    callSettingsMenu.setVisible(false);
                    makeMenuItemVisible(menu, R.id.menu_search, true);
                    makeMenuItemVisible(menu, R.id.menu_import_export, false);
                    makeMenuItemVisible(menu, R.id.menu_accounts, true);
                    makeMenuItemVisible(menu, R.id.menu_display_settings, false);
                    makeMenuItemVisible(menu, R.id.menu_sim_contacts, false);
                  //<!-Added by gangzhou.qi at 2012-7-27
                    mTabPager.mSlipMenuRelativeLayoutTurnOn = true;
                    Log.d(TAG, "mTabPager.mSlipMenuRelativeLayoutTurnOn = true;");
                    //Added by gangzhou.qi at 2012-7-27 -!>
                    break;
                case DIALER:
                    addContactMenu.setVisible(false);
                    addGroupMenu.setVisible(false);
                    //{Added by yongan.qiu on 2012-7-16 begin.
                    messageMenu.setVisible(false);
                    emailMenu.setVisible(false);
                    //}Added by yongan.qiu end.
                    contactsFilterMenu.setVisible(false);
                    dialpadMenu.setVisible(false);
                    callTypeMenu.setVisible(false);
                    callSettingsMenu.setVisible(true);
                    callSettingsMenu.setIntent(DialtactsActivity.getCallSettingsIntent());
                    makeMenuItemVisible(menu, R.id.menu_search, false);
                    makeMenuItemVisible(menu, R.id.menu_import_export, false);
                    makeMenuItemVisible(menu, R.id.menu_accounts, false);
                    makeMenuItemVisible(menu, R.id.menu_display_settings, false);
                    makeMenuItemVisible(menu, R.id.menu_sim_contacts, false);
                  //<!-Added by gangzhou.qi at 2012-7-27
                    mTabPager.mSlipMenuRelativeLayoutTurnOn = false;
                    Log.d(TAG, "mTabPager.mSlipMenuRelativeLayoutTurnOn = false;");
                    //Added by gangzhou.qi at 2012-7-27 -!>
                    break;
            }
        }

        return true;
    }

    private void makeMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item =menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                // The home icon on the action bar is pressed
                if (mActionBarAdapter.isUpShowing()) {
                    // "UP" icon press -- should be treated as "back".
                    onBackPressed();
                }
                return true;
            }
            case R.id.menu_display_settings: {
                final Intent intent = new Intent(this, ContactsPreferenceActivity.class);
                // as there is only one section right now, make sure it is selected
                // on small screens, this also hides the section selector
                // Due to b/5045558, this code unfortunately only works properly on phones
                boolean settingsAreMultiPane = getResources().getBoolean(
                        com.android.internal.R.bool.preferences_prefer_dual_pane);
                if (!settingsAreMultiPane) {
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                            DisplayOptionsPreferenceFragment.class.getName());
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                            R.string.preference_displayOptions);
                }
                startActivity(intent);
                return true;
            }
            case R.id.menu_contacts_filter: {
                AccountFilterUtil.startAccountFilterActivityForResult(this,
                        SUBACTIVITY_ACCOUNT_FILTER);
                return true;
            }
            case R.id.menu_search: {
                onSearchRequested();
                return true;
            }
            case R.id.menu_add_contact: {
                final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                // On 2-pane UI, we can let the editor activity finish itself and return
                // to this activity to display the new contact.
                if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    intent.putExtra(
                        ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
                    startActivityForResult(intent, SUBACTIVITY_NEW_CONTACT);
                } else {
                    // Otherwise, on 1-pane UI, we need the editor to launch the view contact
                    // intent itself.
                    startActivity(intent);
                }
                return true;
            }
            case R.id.menu_add_group: {
                createNewGroupWithAccountDisambiguation();
                return true;
            }
            case R.id.menu_import_export: {
                ImportExportDialogFragment.show(getFragmentManager());
                return true;
            }
            case R.id.menu_accounts: {
                final Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
                intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {
                    ContactsContract.AUTHORITY
                });
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                return true;
            }
            case R.id.menu_sim_contacts: {
            	Intent intent = new Intent(this,SimHelperService.class);
            	intent.setAction(SimHelperService.ACTION_PREPARE);
            	startService(intent);
            	return true;
//            	if(checkSimState() == 5 ){
//            		 Intent intent = new Intent("android.intent.action.SIMPICK");
////           		 intent.setType("vnd.android.cursor.dir/phone");
//           		 intent.setType("vnd.android.cursor.dir/phone_v2");
////           		 intent.setType("vnd.android.cursor.dir/person");
////           		 intent.setType("vnd.android.cursor.dir/contact");
//           		 intent.putExtra("multiple_choice", true);
//           		 startActivityForResult(intent, 1);
//           		 return true;
//            	}
            }
            case R.id.menu_dialpad: {
            	mDialerFragment.setFragmentShow(R.id.dialpad_fragment,
    					R.animator.fragment_slide_down_enter,
    					R.animator.fragment_slide_down_exit,
    					!mDialerFragment.isFragmentShow(R.id.dialpad_fragment));
            	return true;
            }
            case R.id.menu_call_type:{
                if(mDialerFragment.isFragmentShow(R.id.dialpad_fragment)) {
                    mDialerFragment.setFragmentShow(R.id.dialpad_fragment,
                        R.animator.fragment_slide_down_enter,
                        R.animator.fragment_slide_down_exit,
                        !mDialerFragment.isFragmentShow(R.id.dialpad_fragment));
                }
                mDialerFragment.onAnimationFragment();
                return true;
            }
            //{Added by yongan.qiu on 2012-7-16 begin.
            case R.id.menu_msg: {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                intent.putExtra(Constants.EXTRA_MULTIPLE_CHOICE, true);
                intent.putExtra(Constants.EXTRA_CONTACT_LIST_FILTER, mContactListFilterController.getFilter());
                startActivityForResult(intent, REQUEST_CODE_PICK_PHONE);
                return true;
            }
            case R.id.menu_email: {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
                intent.putExtra(Constants.EXTRA_MULTIPLE_CHOICE, true);
                intent.putExtra(Constants.EXTRA_CONTACT_LIST_FILTER, mContactListFilterController.getFilter());
                startActivityForResult(intent, REQUEST_CODE_PICK_EMAIL);
                return true;
            }
            //}Added by yongan.qiu end.
        }
        return false;
    }

    private int checkSimState(){
    	TelephonyManager telephonyManager = new TelephonyManager(this);
		int SIM_STATE = telephonyManager.getSimState();
		if (SIM_STATE != 5) {
			AlertDialog dlg = new AlertDialog.Builder(this)
					.setTitle(getResources().getString(android.R.string.dialog_alert_title))
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(getString(R.string.sim_state_wrong))
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).create();
			dlg.show();
			dlg.setCancelable(true);
			dlg.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
		}
		return SIM_STATE;
    }
    
    private void createNewGroupWithAccountDisambiguation() {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(this).getAccounts(true);
        //{Modified by yongan.qiu on 2012-7-6 begin.
        //This modification is redundant because in our device, mAddGroupImageView is always null.
        //old:
        /*if (accounts.size() <= 1 || mAddGroupImageView == null) {
            // No account to choose or no control to anchor the popup-menu to
            // ==> just go straight to the editor which will disambig if necessary
            final Intent intent = new Intent(this, GroupEditorActivity.class);
            intent.setAction(Intent.ACTION_INSERT);
            startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
            return;
        }

        final ListPopupWindow popup = new ListPopupWindow(this, null);
        popup.setWidth(getResources().getDimensionPixelSize(R.dimen.account_selector_popup_width));
        popup.setAnchorView(mAddGroupImageView);
        // Create a list adapter with all writeable accounts (assume that the writeable accounts all
        // allow group creation).
        final AccountsListAdapter adapter = new AccountsListAdapter(this,
                AccountListFilter.ACCOUNTS_GROUP_WRITABLE);
        popup.setAdapter(adapter);
        popup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popup.dismiss();
                AccountWithDataSet account = adapter.getItem(position);
                final Intent intent = new Intent(PeopleActivity.this, GroupEditorActivity.class);
                intent.setAction(Intent.ACTION_INSERT);
                intent.putExtra(Intents.Insert.ACCOUNT, account);
                intent.putExtra(Intents.Insert.DATA_SET, account.dataSet);
                startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
            }
        });*/
        //new:
        if (mAddGroupImageView == null) {
            final Intent intent = new Intent(this, GroupEditorActivity.class);
            intent.setAction(Intent.ACTION_INSERT);
            startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
            return;
        }
        final ListPopupWindow popup = new ListPopupWindow(this, null);
        popup.setWidth(getResources().getDimensionPixelSize(R.dimen.account_selector_popup_width));
        popup.setAnchorView(mAddGroupImageView);
        final InternalsListAdapter internalListAdapter = new InternalsListAdapter(this, InternalListFilter.INTERNALS_GROUP_WRITABLE);
        AccountsListAdapter accountListAdapter = null;
        if (accounts.size() > 0) {
            accountListAdapter = new AccountsListAdapter(this,
                    AccountListFilter.ACCOUNTS_GROUP_WRITABLE);
        }
        final InternalsAndAccountsMergeAdapter mergeAdapter = 
                new InternalsAndAccountsMergeAdapter(internalListAdapter, accountListAdapter);
        popup.setAdapter(mergeAdapter);
        popup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popup.dismiss();
                AccountWithDataSet account = mergeAdapter.getItem(position);
                final Intent intent = new Intent(PeopleActivity.this, GroupEditorActivity.class);
                intent.setAction(Intent.ACTION_INSERT);
                intent.putExtra(Intents.Insert.ACCOUNT, account);
                intent.putExtra(Intents.Insert.DATA_SET, account.dataSet);
                startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
            }
        });
        //}Modified by yongan.qiu end.
        popup.setModal(true);
        popup.show();
    }

    @Override
    public boolean onSearchRequested() { // Search key pressed.
        mActionBarAdapter.setSearchMode(true);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SUBACTIVITY_ACCOUNT_FILTER: {
            	//begin: added by yunzhou.song
            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            	boolean filterPhones = prefs.getBoolean(Constants.PREF_KEY_FILTES_PHONES, false);
            	ContactListFilter filter = null;
            	if(data != null) {
            		filter = (ContactListFilter)
                            data.getParcelableExtra(AccountFilterActivity.KEY_EXTRA_CONTACT_LIST_FILTER);
            	}
            	if(filter != null
            			&& filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
            		if(filterPhones != mFilterPhones) {
	                	mAllFragment.setFilterPhones(filterPhones);
	                	mAllFragment.reloadData();
            		}
            		mFilterPhones = filterPhones;
            	} else {
            		if(mFilterPhones != false) {
            			mFilterPhones = false;
            			mAllFragment.setFilterPhones(false);
            			mAllFragment.reloadData();
            		}
            	}
            	//end: added by yunzhou.song
                AccountFilterUtil.handleAccountFilterResult(
                        mContactListFilterController, resultCode, data);
                break;
            }

            case SUBACTIVITY_NEW_CONTACT:
            case SUBACTIVITY_EDIT_CONTACT: {
                if (resultCode == RESULT_OK && PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    mRequest.setActionCode(ContactsRequest.ACTION_VIEW_CONTACT);
                    mAllFragment.setSelectionRequired(true);
                    mAllFragment.reloadDataAndSetSelectedUri(data.getData());
                    // Suppress IME if in search mode
                    if (mActionBarAdapter != null) {
                        mActionBarAdapter.clearFocusOnSearchView();
                    }
                    // No need to change the contact filter
                    mCurrentFilterIsValid = true;
                }
                break;
            }

            case SUBACTIVITY_NEW_GROUP:
            case SUBACTIVITY_EDIT_GROUP: {
                if (resultCode == RESULT_OK && PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    mRequest.setActionCode(ContactsRequest.ACTION_GROUP);
                    mGroupsFragment.setSelectedUri(data.getData());
                }
                break;
            }
            //{Added by yongan.qiu on 2012-7-16 begin.
            case REQUEST_CODE_PICK_PHONE: {
                if (resultCode == RESULT_OK && data != null) {
                    Parcelable[] uris = data.getParcelableArrayExtra(Intents.EXTRA_PHONE_URIS);
                    wrapFromPhoneUris(uris);
                }
                break;
            }
            case REQUEST_CODE_PICK_EMAIL: {
                if (resultCode == RESULT_OK && data != null) {
                    Parcelable[] uris = data.getParcelableArrayExtra(Constants.EXTRA_EMAIL_URIS);
                    String[] addresses = getAddressesFromEmailUris(uris);
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    Uri emailAddress = Uri.fromParts("mailto", "", null);
                    emailIntent.setData(emailAddress);
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, addresses);
                    try {
                        startActivity(emailIntent);
                    } catch (ActivityNotFoundException e) {
                        // TODO no activity to send email.
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            //}Added by yongan.qiu end.

            // TODO: Using the new startActivityWithResultFromFragment API this should not be needed
            // anymore
            case ContactEntryListFragment.ACTIVITY_REQUEST_CODE_PICKER:
                if (resultCode == RESULT_OK) {
                    mAllFragment.onPickerResult(data);
                }

// TODO fix or remove multipicker code
//                else if (resultCode == RESULT_CANCELED && mMode == MODE_PICK_MULTIPLE_PHONES) {
//                    // Finish the activity if the sub activity was canceled as back key is used
//                    // to confirm user selection in MODE_PICK_MULTIPLE_PHONES.
//                    finish();
//                }
//                break;
        }
    }

    //{Added by yongan.qiu on 2012-7-16 begin.
    private static final String[] PHONE_PROJECTION = {
        Phone._ID,
        Phone.DISPLAY_NAME,
        Phone.NUMBER
    };
    private static final int PHONE_ID = 0;
    private static final int PHONE_DISPLAY_NAME = 1;
    private static final int PHONE_NUMBER = 2;

    private static final String[] EMAIL_PROJECTION = {
        Email._ID,
        Email.DISPLAY_NAME,
        Email.ADDRESS
    };
    private static final int EMAIL_ID = 0;
    private static final int EMAIL_DISPLAY_NAME = 1;
    private static final int EMAIL_ADDRESS = 2;

    private Uri[] wrapFromPhoneUris(Parcelable[] uris) {
        if (uris == null || uris.length < 1) {
            return null;
        }
        StringBuilder idSet = new StringBuilder();
        boolean needComma = false;
        for (Parcelable uri : uris) {
            ((Uri) uri).getLastPathSegment();
            if (needComma) {
                idSet.append(',');
            } else {
                needComma = true;
            }
            idSet.append(((Uri) uri).getLastPathSegment());
        }
        final String where = Phone._ID + " IN (" + idSet.toString() + ")";
        Cursor cursor = getContentResolver().query(Phone.CONTENT_URI, PHONE_PROJECTION, where, null, null);
        if (cursor == null || cursor.getCount() <= 0) {
            return null;
        }
        
        Uri[] newUris = new Uri[cursor.getCount()];
        try {
            while(cursor.moveToNext()) {
                Log.i(TAG, "id = " + cursor.getLong(PHONE_ID)
                        + "dispaly_name = " + cursor.getString(PHONE_DISPLAY_NAME)
                        + "number = " + cursor.getString(PHONE_NUMBER));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return newUris;
    }

    private String[] getAddressesFromEmailUris(Parcelable[] uris) {
        if (uris == null || uris.length < 1) {
            return null;
        }
        StringBuilder idSet = new StringBuilder();
        boolean needComma = false;
        for (Parcelable uri : uris) {
            ((Uri) uri).getLastPathSegment();
            if (needComma) {
                idSet.append(',');
            } else {
                needComma = true;
            }
            idSet.append(((Uri) uri).getLastPathSegment());
        }
        final String where = Email._ID + " IN (" + idSet.toString() + ")";
        Cursor cursor = getContentResolver().query(Email.CONTENT_URI, PHONE_PROJECTION, where, null, null);
        if (cursor == null || cursor.getCount() <= 0) {
            return null;
        }
        
        String[] addresses = new String[cursor.getCount()];
        int i = 0;
        try {
            while(cursor.moveToNext()) {
                Log.i(TAG, "id = " + cursor.getLong(EMAIL_ID)
                        + "dispaly_name = " + cursor.getString(EMAIL_DISPLAY_NAME)
                        + "number = " + cursor.getString(EMAIL_ADDRESS));
                addresses[i++] = cursor.getString(EMAIL_ADDRESS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return addresses;
    }
    //}Added by yongan.qiu end.

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO move to the fragment
        switch (keyCode) {
//            case KeyEvent.KEYCODE_CALL: {
//                if (callSelection()) {
//                    return true;
//                }
//                break;
//            }

            case KeyEvent.KEYCODE_DEL: {
                if (deleteSelection()) {
                    return true;
                }
                break;
            }
            default: {
                // Bring up the search UI if the user starts typing
                final int unicodeChar = event.getUnicodeChar();
                if (unicodeChar != 0 && !Character.isWhitespace(unicodeChar)) {
                    String query = new String(new int[]{ unicodeChar }, 0, 1);
                    if (!mActionBarAdapter.isSearchMode()) {
                        mActionBarAdapter.setQueryString(query);
                        mActionBarAdapter.setSearchMode(true);
                        return true;
                    }
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mActionBarAdapter.isSearchMode()) {
            mActionBarAdapter.setSearchMode(false);
        } else {
            super.onBackPressed();
        }
    }

    private boolean deleteSelection() {
        // TODO move to the fragment
//        if (mActionCode == ContactsRequest.ACTION_DEFAULT) {
//            final int position = mListView.getSelectedItemPosition();
//            if (position != ListView.INVALID_POSITION) {
//                Uri contactUri = getContactUri(position);
//                if (contactUri != null) {
//                    doContactDelete(contactUri);
//                    return true;
//                }
//            }
//        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mActionBarAdapter.onSaveInstanceState(outState);
        if (mContactDetailLayoutController != null) {
            mContactDetailLayoutController.onSaveInstanceState(outState);
        }

        // Clear the listener to make sure we don't get callbacks after onSaveInstanceState,
        // in order to avoid doing fragment transactions after it.
        // TODO Figure out a better way to deal with the issue.
        mActionBarAdapter.setListener(null);
        if (mTabPager != null) {
            mTabPager.setOnPageChangeListener(null);
        }
    }

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    // Visible for testing
    public ContactBrowseListFragment getListFragment() {
        return mAllFragment;
    }

    // Visible for testing
    public ContactDetailFragment getDetailFragment() {
        return mContactDetailFragment;
    }

    //{Added by yongan.qiu on 2012.6.21 begin.
	public static final String CALL_ORIGIN_DIALTACTS = "com.android.contacts.activities.PeopleActivity";
	private FilteredResultsFragment.Listener mCallLogPhoneNumberListener = new FilteredResultsFragment.Listener() {
		@Override
		public void onContactSelected(Uri contactUri) {
			PhoneNumberInteraction.startInteractionForPhoneCall(
					PeopleActivity.this, contactUri, CALL_ORIGIN_DIALTACTS);
		}
	};

	@Override
	public void onFragmentReady() {
		// TODO Auto-generated method stub
		mFilteredResultsFragment = (FilteredResultsFragment) 
				getFragmentManager().findFragmentById(R.id.filtered_results_fragment);
		DialpadFragment dialpadFragment = (DialpadFragment) getFragmentManager().findFragmentById(R.id.dialpad_fragment);
		Log.i(TAG, "mCallLogPhoneNumberFragment " + mFilteredResultsFragment + ", dialpadFragment " + dialpadFragment
				+ ", dialpadFragment isAdded() " + dialpadFragment.isAdded());
		Log.i(TAG, "mDialerFragment " + mDialerFragment);
		if (mFilteredResultsFragment != null
				&& mFilteredResultsFragment.isAdded()) {
			if (mContactListFilterController == null) {
				mContactListFilterController = ContactListFilterController.getInstance(this);
			}
			mFilteredResultsFragment.setFilter(mContactListFilterController
					.getFilter());
		}
		if (dialpadFragment != null && dialpadFragment.isAdded()) {
			dialpadFragment.setOnDightsChangedListener(this);
		}
		if (mDialerFragment != null) {
			FragmentManager fragmentManager = getFragmentManager();
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			mDialerFragment.showCallLogFragment(transaction);
		}
	}
	
	@Override
	public void onDightsChanged(CharSequence input) {
		Log.i(TAG, "onDightsChanged(). " + input + "mDialerFragment = " + mDialerFragment);
		if (mDialerFragment == null) {
			return;
		}
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (TextUtils.isEmpty(input)) {
			mDialerFragment.showCallLogFragment(transaction);
		} else {
			mDialerFragment.showFilteredResultsFragment(transaction);
		}
		fragmentManager.executePendingTransactions();

		mFilteredResultsFragment.onQueryTextChange(input.toString());
	}
	//}Added by yongan.qiu end.
	
	//Begin by gangzhou.qi at 2012-7-16 上午11:06:38
	private class simStateReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			if(Constants.SIM_READING_STATE.equals(intent.getAction())){
				if(mAllFragment != null){
					mAllFragment.setSIMStateView();
					Log.d("^^", "peopleactivity onreceive the broadcastreceiver");
				}
			}
		}
		
	}
	//Ended by gangzhou.qi at 2012-7-16 上午11:06:38
}
