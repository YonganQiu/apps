package com.android.contacts.list;

import java.util.HashSet;
import java.util.Set;

import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.list.ContactListFilterController.ContactListFilterListener;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.R;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Super class of {@link ContactMultiPickerFragment},
 * {@link PhoneNumberMultiPickerFragment},
 * {@link EmailAddressMultiPickerFragment}
 * 
 * @author yongan.qiu
 */
public abstract class BaseMultiPickerFragment<T extends ContactEntryListAdapter> extends ContactEntryListFragment<T> implements ContactListFilterListener {

    /**
     * Called when "action" menu item clicked.
     */
    public interface OnDoneListener {
        public void onDone();
    }

    private static final String TAG = BaseMultiPickerFragment.class.getSimpleName();

    private static final String KEY_FILTER = "filter";

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    private OnDoneListener mOnDoneListener;

    private Parcelable[] mExcludeUris;

    private String mAccountType;

    private String mAccountName;

    private ContactListFilter mFilter;

    private String mExtraSelection;

    private View mAccountFilterHeader;

    private boolean mFilterSensitive;

    /**
     * Lives as ListView's header and is shown when {@link #mAccountFilterHeader} is set
     * to View.GONE.
     */
    private View mPaddingView;

    /** true if the loader has started at least once. */
    private boolean mLoaderStarted;

    private TextView mSelectedConvCount;
    private boolean mAllSelected;
    private MenuItem mSelectAllOrNone;

    private int mActionTitle;
    private int mActionIcon;

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                    BaseMultiPickerFragment.this, REQUEST_CODE_ACCOUNT_FILTER);
        }
    }
    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    public void setExcludeUris(Parcelable[] excludeUris) {
        mExcludeUris = excludeUris;
    }

    public Parcelable[] getExcludeUris() {
        return mExcludeUris;
    }

    /** 
     * If set account and filter in the same time,
     * only filter works.
     */
    public void setAccount(String accountType, String accountName) {
        mAccountType = accountType;
        mAccountName = accountName;
    }

    private boolean hasAccount() {
        return (mAccountType != null && mAccountName != null);
    }

    public void setFilter(ContactListFilter filter) {
        if ((mFilter == null && filter == null) ||
                (mFilter != null && mFilter.equals(filter))) {
            return;
        }
        mFilter = filter;

        //default sensitive while set
        setFilterSensitive(true);

        if (mLoaderStarted) {
            reloadData();
        }
        updateFilterHeaderView();
    }

    public ContactListFilter getFilter() {
        return mFilter;
    }

    public void setExtraSelection(String selection) {
        mExtraSelection = selection;
    }

    public BaseMultiPickerFragment(OnDoneListener listener, int actionTitle, int actionIcon) {
        if (listener == null) {
            Log.w(TAG, "a listener should be specified!");
        } else {
            mOnDoneListener = listener;
        }
        mActionTitle = actionTitle;
        mActionIcon = actionIcon;

        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setQuickContactEnabled(false);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_CONTACT_SHORTCUT);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            View customView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(
                    R.layout.multi_action_selected_count, null);
            mSelectedConvCount = (TextView) customView
                    .findViewById(R.id.selected_count);

            actionBar.setDisplayShowCustomEnabled(true);
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.RIGHT);
            actionBar.setCustomView(customView, lp);
        }
        setHasOptionsMenu(true);
        ContactListFilterController.getInstance(getActivity()).addListener(this);
    }

    private void clearSelection() {
        ListView listView = getListView();
        Set<Uri> selectedUriSet = getSelectedUriSet();

        listView.clearChoices();
        selectedUriSet.clear();
        listView.requestLayout();
        mAllSelected = false;
        mSelectedConvCount.setText(Integer.toString(listView.getCheckedItemCount()));

        if (mSelectAllOrNone != null) {
            mSelectAllOrNone.setIcon(R.drawable.ic_menu_select_all_holo_dark);
            mSelectAllOrNone.setTitle(R.string.select_all);
        }
    }

    private void selectAll() {
        ListView listView = getListView();
        Set<Uri> selectedUriSet = getSelectedUriSet();

        SparseBooleanArray a = listView.getCheckedItemPositions();
        int itemCount = listView.getAdapter().getCount();
        int headerViewsCount = listView.getHeaderViewsCount();
        for (int i = headerViewsCount; i < itemCount; i++) {
            if (!a.get(i)) {
                listView.setItemChecked(i, true);
                selectedUriSet.add(getUri(i - headerViewsCount));
            }
        }

        if (listView.getCheckedItemCount() > 0) {
            mAllSelected = true;
            mSelectAllOrNone.setIcon(R.drawable.ic_menu_select_none_holo_dark);
            mSelectAllOrNone.setTitle(R.string.select_none);
        }
        mSelectedConvCount.setText(Integer.toString(listView.getCheckedItemCount()));
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        View paddingView = inflater.inflate(R.layout.contact_detail_list_padding, null, false);
        mPaddingView = paddingView.findViewById(R.id.contact_detail_list_padding);
        getListView().addHeaderView(paddingView);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        clearSelection();

        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        updateFilterHeaderView();

        setVisibleScrollbarEnabled(true);
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }

        mFilter = savedState.getParcelable(KEY_FILTER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILTER, mFilter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.multi_action, menu);

        mSelectAllOrNone = menu.findItem(R.id.select_all_or_none);

        MenuItem action = menu.findItem(R.id.action);
        action.setTitle(mActionTitle);
        action.setIcon(mActionIcon);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_all_or_none: {
                ListView listView = getListView();
                Set<Uri> selectedUriSet = getSelectedUriSet();

                if (mAllSelected) {
                    //clear all
                    clearSelection();
                } else {
                    //select all
                    selectAll();
                }
                return true;
 
            }
            case R.id.action: {
                if (mOnDoneListener != null) {
                    mOnDoneListener.onDone();
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Search mode is not allowed here. In case setting this fragment to
     * search mode, throw a IllegalArgumentException.
     */
    @Override
    protected final void setSearchMode(boolean flag) {
        if (flag) {
            throw new IllegalArgumentException("Search mode is not allowed here.");
        }
    }

    @Override
    protected void startLoading() {
        mLoaderStarted = true;
        super.startLoading();
    }

    private void updateFilterHeaderView() {
        final ContactListFilter filter = getFilter();
        if (mAccountFilterHeader == null || filter == null) {
            return;
        }

        final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPhone(
                mAccountFilterHeader, filter, false, false);
        if (shouldShowHeader) {
            mPaddingView.setVisibility(View.GONE);
            mAccountFilterHeader.setVisibility(View.VISIBLE);
        } else {
            mPaddingView.setVisibility(View.VISIBLE);
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(
                        ContactListFilterController.getInstance(getActivity()), resultCode, data);
                clearSelection();
                reloadData();
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    /**
     * Do not override this method on sub class of {@link BaseMultiPickerFragment}.
     */
    @Override
    protected final T createListAdapter() {
        T adapter = instanceAdapter();

        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);

        if (hasAccount()) {
            adapter.setFilter(ContactListFilter.createAccountFilter(
                    mAccountType, mAccountName, null, null));
        }
        adapter.setExtraSelection(mExtraSelection);

        onCreateListAdapter(adapter);
        return adapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        if (getAdapter() != null && mFilter != null) {
            getAdapter().setFilter(mFilter);
        }
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    /**
     * Whether allow to automatically adjust filter when filter changed.
     * @param flag true if allowed, false otherwise
     */
    public void setFilterSensitive(boolean flag) {
        mFilterSensitive = flag;
    }

    public boolean isFilterSensitive() {
        return mFilterSensitive;
    }

    @Override
    public void onContactListFilterChanged() {
        if (!isFilterSensitive()) {
            return;
        }
        setFilter(ContactListFilterController.getInstance(getActivity()).getFilter());
    }

    @Override
    protected void onItemClick(int position, long id) {
        ListView listView = getListView();
        
        mSelectedConvCount.setText(Integer.toString(listView.getCheckedItemCount()));

        Uri uri = getUri(position);
        if (uri != null) {
            if(listView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
                int adjPosition = position + listView.getHeaderViewsCount();
                boolean isChecked = listView.isItemChecked(adjPosition);
                
                Set<Uri> selectedUriSet = getSelectedUriSet();
                if(isChecked) {
                    selectedUriSet.add(uri);
                } else {
                    selectedUriSet.remove(uri);
                }
                
                View buttonsLayout = getActivity().findViewById(R.id.layout_bottom);
                if(buttonsLayout != null) {
                    Button okBtn = (Button)buttonsLayout.findViewById(R.id.btn_ok);
                    if(okBtn != null) {
                        okBtn.setEnabled(!selectedUriSet.isEmpty());
                    }
                }
            } else {
                Log.e(TAG, "This fragment should not appear when not in multiple choice mode!");
            }
        } else {
            Log.w(TAG, "Item at " + position + " was clicked before adapter is ready. Ignoring");
        }

        boolean oldState = mAllSelected;
        mAllSelected = (listView.getCheckedItemCount() == getAdapter().getCount());
        if (mSelectAllOrNone != null && oldState != mAllSelected) {
            if (mAllSelected) {
                mSelectAllOrNone.setIcon(R.drawable.ic_menu_select_none_holo_dark);
                mSelectAllOrNone.setTitle(R.string.select_none);
            } else {
                mSelectAllOrNone.setIcon(R.drawable.ic_menu_select_all_holo_dark);
                mSelectAllOrNone.setTitle(R.string.select_all);

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ContactListFilterController.getInstance(getActivity()).removeListener(this);
    }

    /**
     * Do some extra initialization on the adapter just created.
     * @param adapter the adapter just created
     */
    protected void onCreateListAdapter(T adapter) {
    }

    protected abstract T instanceAdapter();

    protected abstract Uri getUri(int position);
}
