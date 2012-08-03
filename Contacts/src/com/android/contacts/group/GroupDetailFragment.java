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
 * limitations under the License
 */

package com.android.contacts.group;

import java.util.HashSet;
import java.util.Set;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.GroupDetailActivity;
import com.android.contacts.interactions.GroupDeletionDialogFragment;
import com.android.contacts.list.ContactTileAdapter;
import com.android.contacts.list.ContactTileAdapter.DisplayType;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.util.Constants;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
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
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Displays the details of a group and shows a list of actions possible for the group.
 */
public class GroupDetailFragment extends Fragment implements OnScrollListener {

    public static interface Listener {
        /**
         * The group title has been loaded
         */
        public void onGroupTitleUpdated(String title);

        /**
         * The number of group members has been determined
         */
        public void onGroupSizeUpdated(String size);

        /**
         * The account type and dataset have been determined.
         */
        public void onAccountTypeUpdated(String accountTypeString, String dataSet);

        /**
         * User decided to go to Edit-Mode
         */
        public void onEditRequested(Uri groupUri);

        /**
         * Contact is selected and should launch details page
         */
        public void onContactSelected(Uri contactUri);
    }

    private static final String TAG = "GroupDetailFragment";

    private static final int LOADER_METADATA = 0;
    private static final int LOADER_MEMBERS = 1;

    //{Added by yongan.qiu on 2012-7-16 begin.
    private static final int REQUEST_CODE_PICK_CONTACT = 1;
    private static final int REQUEST_CODE_PICK_PHONE = 2;
    private static final int REQUEST_CODE_PICK_EMAIL = 3;
    //}Added by yongan.qiu end.

    private Context mContext;

    private View mRootView;
    private ViewGroup mGroupSourceViewContainer;
    private View mGroupSourceView;
    private TextView mGroupTitle;
    private TextView mGroupSize;
    private ListView mMemberListView;
    private View mEmptyView;

    private Listener mListener;

    private ContactTileAdapter mAdapter;
    private ContactPhotoManager mPhotoManager;
    private AccountTypeManager mAccountTypeManager;

    private Uri mGroupUri;
    private long mGroupId;
    private String mGroupName;
    private String mAccountTypeString;
    private String mDataSet;
    private boolean mIsReadOnly;

    private boolean mShowGroupActionInActionBar;
    private boolean mOptionsMenuGroupDeletable;
    private boolean mOptionsMenuGroupPresent;
    private boolean mCloseActivityAfterDelete;
    
    //begin: added by yunzhou.song
//    private Set<Uri> mContactUriSet = new HashSet<Uri>();
//    
//    public Uri[] getContactUris() {
//    	Uri[] uris = null;
//    	if(!mContactUriSet.isEmpty()) {
//    		uris = new Uri[mContactUriSet.size()];
//    		mContactUriSet.toArray(uris);
//    	}
//    	return uris;
//    }
    //end: added by yunzhou.song
    
	public GroupDetailFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);

        Resources res = getResources();
        int columnCount = res.getInteger(R.integer.contact_tile_column_count);

        mAdapter = new ContactTileAdapter(activity, mContactTileListener, columnCount,
                DisplayType.GROUP_MEMBERS);

        configurePhotoLoader();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        mRootView = inflater.inflate(R.layout.group_detail_fragment, container, false);
        mGroupTitle = (TextView) mRootView.findViewById(R.id.group_title);
        mGroupSize = (TextView) mRootView.findViewById(R.id.group_size);
        mGroupSourceViewContainer = (ViewGroup) mRootView.findViewById(
                R.id.group_source_view_container);
        mEmptyView = mRootView.findViewById(android.R.id.empty);
        mMemberListView = (ListView) mRootView.findViewById(android.R.id.list);
        mMemberListView.setAdapter(mAdapter);

        return mRootView;
    }

    public void loadGroup(Uri groupUri) {
        mGroupUri= groupUri;
        startGroupMetadataLoader();
    }

    public void setQuickContact(boolean enableQuickContact) {
        mAdapter.enableQuickContact(enableQuickContact);
    }

    private void configurePhotoLoader() {
        if (mContext != null) {
            if (mPhotoManager == null) {
                mPhotoManager = ContactPhotoManager.getInstance(mContext);
            }
            if (mMemberListView != null) {
                mMemberListView.setOnScrollListener(this);
            }
            if (mAdapter != null) {
                mAdapter.setPhotoLoader(mPhotoManager);
            }
        }
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    public void setShowGroupSourceInActionBar(boolean show) {
        mShowGroupActionInActionBar = show;
    }

    /**
     * Start the loader to retrieve the metadata for this group.
     */
    private void startGroupMetadataLoader() {
        getLoaderManager().restartLoader(LOADER_METADATA, null, mGroupMetadataLoaderListener);
    }

    /**
     * Start the loader to retrieve the list of group members.
     */
    private void startGroupMembersLoader() {
        getLoaderManager().restartLoader(LOADER_MEMBERS, null, mGroupMemberListLoaderListener);
    }

    private final ContactTileAdapter.Listener mContactTileListener =
            new ContactTileAdapter.Listener() {

        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            mListener.onContactSelected(contactUri);
        }
    };

    /**
     * The listener for the group metadata loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetadataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new GroupMetaDataLoader(mContext, mGroupUri);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            data.moveToPosition(-1);
            if (data.moveToNext()) {
                boolean deleted = data.getInt(GroupMetaDataLoader.DELETED) == 1;
                if (!deleted) {
                    bindGroupMetaData(data);

                    // Retrieve the list of members
                    startGroupMembersLoader();
                    return;
                }
            }
            updateSize(-1);
            updateTitle(null);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener for the group members list loader
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return GroupMemberLoader.constructLoaderForGroupDetailQuery(mContext, mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        	//begin: added by yunzhou.song
//        	if(data != null) {
//        		long contactId;
//        		while(data.moveToNext()) {
//	        		contactId = data.getLong(data.getColumnIndex(Data.CONTACT_ID));
//	        		mContactUriSet.add(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId));
//        		}
//        		data.moveToPosition(-1);
//        	}
        	//end: added by yunzhou.song
            updateSize(data.getCount());
            mAdapter.setContactCursor(data);
            mMemberListView.setEmptyView(mEmptyView);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    private void bindGroupMetaData(Cursor cursor) {
        cursor.moveToPosition(-1);
        if (cursor.moveToNext()) {
            mAccountTypeString = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            mDataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
            mGroupId = cursor.getLong(GroupMetaDataLoader.GROUP_ID);
            mGroupName = cursor.getString(GroupMetaDataLoader.TITLE);
            mIsReadOnly = cursor.getInt(GroupMetaDataLoader.IS_READ_ONLY) == 1;
            updateTitle(mGroupName);
            // Must call invalidate so that the option menu will get updated
            getActivity().invalidateOptionsMenu ();

            final String accountTypeString = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            final String dataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
            updateAccountType(accountTypeString, dataSet);
        }
    }

    private void updateTitle(String title) {
        if (mGroupTitle != null) {
            mGroupTitle.setText(title);
        } else {
            mListener.onGroupTitleUpdated(title);
        }
    }

    /**
     * Display the count of the number of group members.
     * @param size of the group (can be -1 if no size could be determined)
     */
    private void updateSize(int size) {
        String groupSizeString;
        if (size == -1) {
            groupSizeString = null;
        } else {
            String groupSizeTemplateString = getResources().getQuantityString(
                    R.plurals.num_contacts_in_group, size);
            AccountType accountType = mAccountTypeManager.getAccountType(mAccountTypeString,
                    mDataSet);
            groupSizeString = String.format(groupSizeTemplateString, size,
                    accountType.getDisplayLabel(mContext));
        }

        if (mGroupSize != null) {
            mGroupSize.setText(groupSizeString);
        } else {
            mListener.onGroupSizeUpdated(groupSizeString);
        }
    }

    /**
     * Once the account type, group source action, and group source URI have been determined
     * (based on the result from the {@link Loader}), then we can display this to the user in 1 of
     * 2 ways depending on screen size and orientation: either as a button in the action bar or as
     * a button in a static header on the page.
     */
    private void updateAccountType(final String accountTypeString, final String dataSet) {

        // If the group action should be shown in the action bar, then pass the data to the
        // listener who will take care of setting up the view and click listener. There is nothing
        // else to be done by this {@link Fragment}.
        if (mShowGroupActionInActionBar) {
            mListener.onAccountTypeUpdated(accountTypeString, dataSet);
            return;
        }

        final AccountTypeManager manager = AccountTypeManager.getInstance(getActivity());
        final AccountType accountType =
                manager.getAccountType(accountTypeString, dataSet);

        // Otherwise, if the {@link Fragment} needs to create and setup the button, then first
        // verify that there is a valid action.
        if (!TextUtils.isEmpty(accountType.getViewGroupActivity())) {
            if (mGroupSourceView == null) {
                mGroupSourceView = GroupDetailDisplayUtils.getNewGroupSourceView(mContext);
                // Figure out how to add the view to the fragment.
                // If there is a static header with a container for the group source view, insert
                // the view there.
                if (mGroupSourceViewContainer != null) {
                    mGroupSourceViewContainer.addView(mGroupSourceView);
                }
            }

            // Rebind the data since this action can change if the loader returns updated data
            mGroupSourceView.setVisibility(View.VISIBLE);
            GroupDetailDisplayUtils.bindGroupSourceView(mContext, mGroupSourceView,
                    accountTypeString, dataSet);
            mGroupSourceView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI, mGroupId);
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setClassName(accountType.resPackageName,
                            accountType.getViewGroupActivity());
                    startActivity(intent);
                }
            });
        } else if (mGroupSourceView != null) {
            mGroupSourceView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mPhotoManager.pause();
        } else {
            mPhotoManager.resume();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.view_group, menu);
    }

    public boolean isOptionsMenuChanged() {
        return mOptionsMenuGroupDeletable != isGroupDeletable() &&
                mOptionsMenuGroupPresent != isGroupPresent();
    }

    public boolean isGroupDeletable() {
        return mGroupUri != null && !mIsReadOnly;
    }

    public boolean isGroupPresent() {
        return mGroupUri != null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //{Modified by yongan.qiu on 2012-8-2 begin.
        //For sometimes options menu is prepared before fragment becomeing visible,
        //in this case we will miss the "edit" and "delete" items.
        //old:
        /*mOptionsMenuGroupDeletable = isGroupDeletable() && isVisible();
        mOptionsMenuGroupPresent = isGroupPresent() && isVisible();*/
        //new:
        mOptionsMenuGroupDeletable = isGroupDeletable();
        mOptionsMenuGroupPresent = isGroupPresent();
        //}Modified by yongan.qiu end.

        final MenuItem editMenu = menu.findItem(R.id.menu_edit_group);
        editMenu.setVisible(mOptionsMenuGroupPresent);

        final MenuItem deleteMenu = menu.findItem(R.id.menu_delete_group);
        deleteMenu.setVisible(mOptionsMenuGroupDeletable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit_group: {
                if (mListener != null) mListener.onEditRequested(mGroupUri);
                break;
            }
            //{Added by yongan.qiu on 2012-8-2 begin.
            case R.id.menu_add_members: {
                
                break;
            }
            case R.id.menu_msg: {
                Intent intent = new Intent(Constants.ACTION_MULTI_PICK);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                intent.putExtra(Constants.EXTRA_MULTIPLE_CHOICE, true);
                intent.putExtra(Constants.EXTRA_SELECTION, createExtraSelection());
                intent.putExtra(Constants.EXTRA_ACTION_TITLE, R.string.menu_msg);
                intent.putExtra(Constants.EXTRA_ACTION_ICON, R.drawable.ic_menu_msg_holo_dark);
                startActivityForResult(intent, REQUEST_CODE_PICK_PHONE);
                return true;
            }
            case R.id.menu_email: {
                Intent intent = new Intent(Constants.ACTION_MULTI_PICK);
                intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
                intent.putExtra(Constants.EXTRA_MULTIPLE_CHOICE, true);
                intent.putExtra(Constants.EXTRA_SELECTION, createExtraSelection());
                intent.putExtra(Constants.EXTRA_ACTION_TITLE, R.string.menu_email);
                intent.putExtra(Constants.EXTRA_ACTION_ICON, R.drawable.ic_menu_email_holo_dark);
                startActivityForResult(intent, REQUEST_CODE_PICK_EMAIL);
                return true;
            }
            //}Added by yongan.qiu end.
            case R.id.menu_delete_group: {
                GroupDeletionDialogFragment.show(getFragmentManager(), mGroupId, mGroupName,
                        mCloseActivityAfterDelete);
                return true;
            }
        }
        return false;
    }

    //{Added by yongan.qiu on 2012-8-2 begin.
    /**
     * Create extra selection that representing selecting from this group.
     * @return extra selection
     */
    private String createExtraSelection() {
        long[] contactIds = mAdapter.getContactIds();
        StringBuilder selection = new StringBuilder();
        selection.append(Data.CONTACT_ID + " IN (");
        if (contactIds != null && contactIds.length > 0) {
            for (Long id : contactIds) {
                selection.append(id).append(',');
            }
            if (contactIds.length > 0) {
                selection.delete(selection.length() - 1, selection.length());
            }
        }
        selection.append(")");
        return selection.toString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_PHONE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Parcelable[] uris = data.getParcelableArrayExtra(Intents.EXTRA_PHONE_URIS);
                    Intent mmsIntent = new Intent(Constants.MMS_SEND_TO);
                    mmsIntent.putExtra(Intents.EXTRA_PHONE_URIS, uris);
                    mmsIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(mmsIntent);
                    } catch (ActivityNotFoundException e) {
                        // TODO no activity to send mms.
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case REQUEST_CODE_PICK_EMAIL: {
                if (resultCode == Activity.RESULT_OK && data != null) {
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

            default:
                break;
        }
    }

    private static final String[] EMAIL_PROJECTION = {
        Email._ID,
        Email.DISPLAY_NAME,
        Email.ADDRESS
    };
    private static final int EMAIL_ID = 0;
    private static final int EMAIL_DISPLAY_NAME = 1;
    private static final int EMAIL_ADDRESS = 2;

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
        Cursor cursor = getActivity().getContentResolver().query(Email.CONTENT_URI, EMAIL_PROJECTION, where, null, null);
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

    public void closeActivityAfterDelete(boolean closeActivity) {
        mCloseActivityAfterDelete = closeActivity;
    }

    public long getGroupId() {
        return mGroupId;
    }
}
