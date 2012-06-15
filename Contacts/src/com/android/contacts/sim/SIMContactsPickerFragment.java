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
package com.android.contacts.sim;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.contacts.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Contacts.People;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.activities.ExportSIMContacts;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.DirectoryListLoader;
import com.android.contacts.list.LegacyPhoneNumberListAdapter;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.ShortcutIntentBuilder;
import com.android.contacts.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.sim.SIMContactsListAdapter.PhoneQuery;
import com.android.contacts.sim.SimContacts.SimContact;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.vcard.SelectAccountActivity;

import android.os.ServiceManager;
import com.android.internal.telephony.AdnRecord;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.AdnRecordCache;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;

/**
 * Fragment containing a phone number list for picking.
 */
public class SIMContactsPickerFragment extends
		ContactEntryListFragment<ContactEntryListAdapter> implements
		OnShortcutIntentCreatedListener {
	private static final String TAG = SIMContactsPickerFragment.class
			.getSimpleName();

	private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;
	public String nameForActivity;
	public String numberForActivity;
	private OnPhoneNumberPickerActionListener mListener;
	private String mShortcutAction;
	private static final int SELECT_ACCOUNT = 1;
	private static final int SELECT_ACCOUNT_FOR_TOTALIMPORT = 2;
	
	private AccountWithDataSet mAccount;
	private ContactListFilter mFilter;
	BroadcastReceiver mSIMChangeReceiver;
	private String ACTION = "SIMChange";
	private View mAccountFilterHeader;
	private ProgressDialog mProgressDialog;
	private boolean ResumeDelete = true;
	private boolean ResumeImport = true;
	private deleteAsyncTask deleteTask;
	private final static int TOAST_DELETE = 1;
	private final static int TOAST_IMPORT = 2;
	importAsyncTask mImportAsyncTask;
	
	private boolean continueQuery = true;
	private int successCount = 0 ;
	private int failedCount = 0 ;
	private int toDeleteCount = 0;
	private boolean BROADCAST_REGISTERED = false;
	private createSimContactListAsyncTask mCreateSimContactListAsyncTask;
	/**
	 * Lives as ListView's header and is shown when
	 * {@link #mAccountFilterHeader} is set to View.GONE.
	 */
	private View mPaddingView;

	private static final String KEY_FILTER = "filter";
	public Set<String> mIndexSet = new HashSet<String>();
	public ArrayList<SimContact> mSimContactList = new ArrayList<SimContact>();
	private SimContact[] simContactsForMultiAccountImport;
	/** true if the loader has started at least once. */
	private boolean mLoaderStarted;

	private ContactListItemView.PhotoPosition mPhotoPosition = ContactListItemView.DEFAULT_PHOTO_POSITION;

	private class FilterHeaderClickListener implements OnClickListener {
		@Override
		public void onClick(View view) {
			AccountFilterUtil
					.startAccountFilterActivityForResult(
							SIMContactsPickerFragment.this,
							REQUEST_CODE_ACCOUNT_FILTER);
		}
	}

	private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

	public SIMContactsPickerFragment() {
		setQuickContactEnabled(false);
		setPhotoLoaderEnabled(true);
		setSectionHeaderDisplayEnabled(true);
		setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DATA_SHORTCUT);

		// Show nothing instead of letting caller Activity show something.
		setHasOptionsMenu(true);
		BROADCAST_REGISTERED = false;
	}

	private Handler nHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(isAdded()){
			switch (msg.what) {
			case TOAST_DELETE:
    			Toast.makeText(getContext(), successCount + getString(R.string.delete_success) +failedCount + getString(R.string.delete_failed), Toast.LENGTH_SHORT).show();
    			break;
    		
			case TOAST_IMPORT:
    			Toast.makeText(getContext(), successCount + getString(R.string.import_success_total) +failedCount + getString(R.string.import_failed), Toast.LENGTH_SHORT).show();
    			break;
			}
			}

		}
	};
	
	
	
	public void setOnPhoneNumberPickerActionListener(
			OnPhoneNumberPickerActionListener listener) {
		this.mListener = listener;
	}

	@Override
	protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
		super.onCreateView(inflater, container);

		View paddingView = inflater.inflate(
				R.layout.contact_detail_list_padding, null, false);
		mPaddingView = paddingView
				.findViewById(R.id.contact_detail_list_padding);
		getListView().addHeaderView(paddingView);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				nameForActivity = ((ContactListItemView) view).getNameTextView().getText().toString();
				numberForActivity = ((ContactListItemView) view).getDataView().getText().toString();
				final AccountTypeManager accountTypes = AccountTypeManager
						.getInstance(getActivity());
				final List<AccountWithDataSet> accountList = accountTypes
						.getAccounts(true);
				if (accountList.size() == 0) {
					mAccount = null;
					importOneContactWhenNoMultipickAccount(nameForActivity,numberForActivity);	
				} else if (accountList.size() == 1) {
					mAccount = accountList.get(0);
					importOneContactWhenMultipickAccount(nameForActivity, numberForActivity, mAccount.name, mAccount.type);
				} else {
					getActivity().startActivityForResult(new Intent(getActivity(),
							SelectAccountActivity.class), SELECT_ACCOUNT);
					return;
				}
				
				Toast.makeText(getContext(), getString(R.string.import_success , nameForActivity), Toast.LENGTH_SHORT).show();

			
			}
		});
		setVisibleScrollbarEnabled(!isLegacyCompatibilityMode());
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		getListView().setMultiChoiceModeListener(new ContactMultiChoiceModeListener());
//		mCreateSimContactListAsyncTask = new createSimContactListAsyncTask();
//		mCreateSimContactListAsyncTask.execute();
		
		AsyncQueryHandler queryHandler = new AsyncQueryHandler(getContext().getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie,
					Cursor cursor) {
				
				if(continueQuery){
				mSimContacts = new SimContacts(cursor, Contacts.SORT_KEY_PRIMARY);
				mCounts = mSimContacts.getCounts();
				mTitles = mSimContacts.getTitles();
				lateCursor = mSimContacts.getSimContacts();
				lateCursor.moveToPosition(-1);
				String id;
				String name;
				String number;
				SimContact simContact;
				mIndexSet.clear();
				mSimContactList.clear();
				while (lateCursor.moveToNext()) {
					id = lateCursor.getString(PhoneQuery.PHONE_ID);
					name = lateCursor.getString(PhoneQuery.PHONE_DISPLAY_NAME);
					number = lateCursor.getString(PhoneQuery.PHONE_NUMBER);
					simContact = new SimContact(id, null, null, number, null, null, null, name, null, null);
					mSimContactList.add(simContact);
					Log.d("^^", "id:" + id + " name:" + name);
					mIndexSet.add(id);
				}
				
				
				lateCursor.moveToPosition(-1);
				if(adapter != null && adapter instanceof SIMContactsListAdapter) {
					adapter.setTitles(mTitles);
					adapter.setCounts(mCounts);
					adapter.changeCursor(lateCursor);
				}
				if(getActivity() != null){
				getActivity().findViewById(R.id.loadingContact).setVisibility(View.GONE);
				}
				}
				
			}
		};
		queryHandler.startQuery(0, null, mSIMUri, null, null, null, null);
	}

	@Override
	protected void setSearchMode(boolean flag) {
		super.setSearchMode(flag);
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
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == android.R.id.home) { // See
											// ActionBar#setDisplayHomeAsUpEnabled()
			if (mListener != null) {
				mListener.onHomeInActionBarSelected();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * @param shortcutAction
	 *            either {@link Intent#ACTION_CALL} or
	 *            {@link Intent#ACTION_SENDTO} or null.
	 */
	public void setShortcutAction(String shortcutAction) {
		this.mShortcutAction = shortcutAction;
	}

	@Override
	protected void onItemClick(int position, long id) {
		final Uri phoneUri;
		if (!isLegacyCompatibilityMode()) {
			SIMContactsListAdapter adapter = (SIMContactsListAdapter) getAdapter();
			phoneUri = adapter.getDataUri(position);

		} else {
			LegacyPhoneNumberListAdapter adapter = (LegacyPhoneNumberListAdapter) getAdapter();
			phoneUri = adapter.getPhoneUri(position);
		}

		if (phoneUri != null) {
			pickPhoneNumber(phoneUri);
		} else {
			Log.w(TAG, "Item at " + position
					+ " was clicked before adapter is ready. Ignoring");
		}
	}

	@Override
	protected void startLoading() {
		mLoaderStarted = true;
		super.startLoading();
	}

	// begin : added by JiangzhouQ
	SIMCursorLoader mSIMCursorLoader;
	int[] mCounts;
	String[] mTitles;
	SimContacts mSimContacts;
	Cursor mCursor;
	Cursor lateCursor;
	Uri mSIMUri = Uri.parse("content://icc/adn");

	@Override
	public CursorLoader createCursorLoader() {
		mSIMCursorLoader = new SIMCursorLoader(getActivity());
		return mSIMCursorLoader;

	}

	// end : added by JiangzhouQ
	public SIMContactsListAdapter adapter;

	public void beforeUpdateIndexer() {
		Cursor nCursor = null;
		nCursor = getContext().getContentResolver().query(mSIMUri, null, null,
				null, null);


		mSimContacts = new SimContacts(nCursor, Contacts.SORT_KEY_PRIMARY);
		mCounts = mSimContacts.getCounts();
		mTitles = mSimContacts.getTitles();
		adapter.beforeUpdateIndexer(mCounts, mTitles);
		lateCursor = mSimContacts.getSimContacts();
		lateCursor.moveToPosition(-1);
		String id;
		String name;
		String number;
		SimContact simContact;
		mIndexSet.clear();
		mSimContactList.clear();
		while (lateCursor.moveToNext()) {
			id = lateCursor.getString(PhoneQuery.PHONE_ID);
			name = lateCursor.getString(PhoneQuery.PHONE_DISPLAY_NAME);
			number = lateCursor.getString(PhoneQuery.PHONE_NUMBER);
			simContact = new SimContact(id, null, null, number, null, null, null, name, null, null);
			mSimContactList.add(simContact);
			mIndexSet.add(id);
			Log.d("^^", "id:" + id + " name:" + name);
		}
	}
	
	private class createSimContactListAsyncTask extends AsyncTask<Long, Integer, Void> {

		@Override
		protected Void doInBackground(Long... params) {
			createSimContactList();
			return null;
		}
		@Override
		protected void onPreExecute() {
//			TextView emptyView = (TextView) getView().findViewById(R.id.loadingText);
//			emptyView.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		@Override
		protected void onPostExecute(Void result) {
//			TextView emptyView = (TextView) getView().findViewById(R.id.loadingText);
//			emptyView.setVisibility(View.GONE);
			
			super.onPostExecute(result);
		}
	}
	
	public void createSimContactList(){
		Long time1 = System.currentTimeMillis();
		Cursor nCursor = null;
		nCursor = getContext().getContentResolver().query(mSIMUri, null, null,
				null, null);
		mSimContacts = new SimContacts(nCursor, Contacts.SORT_KEY_PRIMARY);
		mCounts = mSimContacts.getCounts();
		mTitles = mSimContacts.getTitles();
		lateCursor = mSimContacts.getSimContacts();
		lateCursor.moveToPosition(-1);
		String id;
		String name;
		String number;
		SimContact simContact;
		mIndexSet.clear();
		while (lateCursor.moveToNext()) {
			id = lateCursor.getString(PhoneQuery.PHONE_ID);
			name = lateCursor.getString(PhoneQuery.PHONE_DISPLAY_NAME);
			number = lateCursor.getString(PhoneQuery.PHONE_NUMBER);
			simContact = new SimContact(id, null, null, number, null, null, null, name, null, null);
			mSimContactList.add(simContact);
			mIndexSet.add(id);
		}
		Long time2 = System.currentTimeMillis();
		Long time3 = time2 -time1;
	
	}

	@Override
	protected ContactEntryListAdapter createListAdapter() {

//		mCursor = getContext().getContentResolver().query(mSIMUri, null, null,
//				null, null);
//
//		mSimContacts = new SimContacts(mCursor, Contacts.SORT_KEY_PRIMARY);
//		
//		mCounts = mSimContacts.getCounts();
//		mTitles = mSimContacts.getTitles();

		if (!isLegacyCompatibilityMode()) {
			adapter = new SIMContactsListAdapter(getActivity(), mCounts,
					mTitles , getActivity());
			adapter.setDisplayPhotos(true);
			return adapter;
		} else {
			LegacyPhoneNumberListAdapter adapter = new LegacyPhoneNumberListAdapter(
					getActivity());
			adapter.setDisplayPhotos(true);
			return adapter;
		}
	}

	@Override
	protected void configureAdapter() {
		super.configureAdapter();

		final ContactEntryListAdapter adapter = getAdapter();
		if (adapter == null) {
			return;
		}

		if (!isSearchMode() && mFilter != null) {
			adapter.setFilter(mFilter);
		}

		if (!isLegacyCompatibilityMode()) {
			((SIMContactsListAdapter) adapter).setPhotoPosition(mPhotoPosition);
		}
	}

	@Override
	protected View inflateView(LayoutInflater inflater, ViewGroup container) {
		return inflater.inflate(R.layout.contact_list_content_sim, null);
	}

	public void pickPhoneNumber(Uri uri) {
		if (mShortcutAction == null) {

			mListener.onPickPhoneNumberAction(uri);
		} else {
			if (isLegacyCompatibilityMode()) {
				throw new UnsupportedOperationException();
			}
			ShortcutIntentBuilder builder = new ShortcutIntentBuilder(
					getActivity(), this);
			builder.createPhoneNumberShortcutIntent(uri, mShortcutAction);
		}
	}

	public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
		mListener.onShortcutIntentCreated(shortcutIntent);
	}

	@Override
	public void onPickerResult(Intent data) {
		mListener.onPickPhoneNumberAction(data.getData());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
			if (getActivity() != null) {
				AccountFilterUtil.handleAccountFilterResult(
						ContactListFilterController.getInstance(getActivity()),
						resultCode, data);
			} else {
				Log.e(TAG,
						"getActivity() returns null during Fragment#onActivityResult()");
			}
		}
	}

	public void setPhotoPosition(ContactListItemView.PhotoPosition photoPosition) {
		mPhotoPosition = photoPosition;
		if (!isLegacyCompatibilityMode()) {
			final SIMContactsListAdapter adapter = (SIMContactsListAdapter) getAdapter();
			if (adapter != null) {
				adapter.setPhotoPosition(photoPosition);
			}
		} else {
			Log.w(TAG,
					"setPhotoPosition() is ignored in legacy compatibility mode.");
		}
	}

	public void startWatchSIMContactsModify() {
		mSIMChangeReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String actionStr = intent.getAction();
				if (actionStr.equals(ACTION)) {
					mSIMCursorLoader.forceLoad();
					beforeUpdateIndexer();
				}

			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION);
		if (!BROADCAST_REGISTERED) {
			getActivity().registerReceiver(mSIMChangeReceiver, filter);
			BROADCAST_REGISTERED = true;
		}
	}

	public boolean exportOneContact(String str1, String str2)
			throws RemoteException {
		String pin2 = null;
		int currentIndex = 0;
		IIccPhoneBook simPhoneBook = IIccPhoneBook.Stub
				.asInterface(ServiceManager.getService("simphonebook"));
		int[] index = simPhoneBook.getAdnRecordsSize(IccConstants.EF_ADN);
		int indexLimited = index[2];
		if(mIndexSet.isEmpty()){
			beforeUpdateIndexer();
		}
		for (int i = 1; i <= indexLimited; i++) {
			if (!mIndexSet.contains(i + "")) {
				currentIndex = i;
				mIndexSet.add(currentIndex + "");
				break;
			}
		}
		if (currentIndex == 0 || currentIndex > indexLimited) {
			Runnable exportRunnable = new Runnable() {
				@Override
				public void run() {
					showDialog();
				}
			};
			nHandler.post(exportRunnable);
		} else {
			AdnRecord firstAdn = new AdnRecord(str1, str2);
			boolean success = false;
			try {
				// if (LOG_SWITCH == true) {

				// }
				success = simPhoneBook.updateAdnRecordsInEfByIndex(
						IccConstants.EF_ADN, firstAdn.getAlphaTag(),
						firstAdn.getNumber(), currentIndex, pin2);
				
			} catch (RemoteException e) {
				Log.e("^^", e.toString(), e);
			}
			return success;
		}
		return false;
	}
	
	public void showDialog() {
		AlertDialog dlg = new AlertDialog.Builder(getContext())
				.setTitle(R.string.sim_wrong)
				.setMessage(R.string.sim_full)
				.setIcon(android.R.drawable.ic_dialog_alert)
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
	}
	//begin: added by yunzhou.song
    private class ContactMultiChoiceModeListener implements ListView.MultiChoiceModeListener {
		private TextView mSelectedConvCount;
		private View mMultiSelectActionBarView;
		private HashSet<SimContact> mSelectedSimContactSet;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			 MenuInflater inflater = new MenuInflater(getContext());
			 mSelectedSimContactSet = new HashSet<SimContact>();
            inflater.inflate(R.menu.sim_contact_multi_select_menu, menu);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = (ViewGroup)LayoutInflater.from(getContext())
                    .inflate(R.layout.contact_multi_select_actionbar, null);

                mSelectedConvCount =
                    (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_conv_count);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            ((TextView)mMultiSelectActionBarView.findViewById(R.id.title))
                .setText(R.string.select_contacts);
            return true; 
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup)LayoutInflater.from(getContext())
                    .inflate(R.layout.contact_multi_select_actionbar, null);
                mode.setCustomView(v);

                mSelectedConvCount = (TextView)v.findViewById(R.id.selected_conv_count);
            }
            return true; 
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
            case R.id.delete:
				if (!mSelectedSimContactSet.isEmpty()) {
					
					SimContact[] simContacts = new SimContact[mSelectedSimContactSet
							.size()];
					mSelectedSimContactSet.toArray(simContacts);
					deleteTask = new deleteAsyncTask();
					deleteTask.execute(simContacts);
					
		    		mProgressDialog = new ProgressDialog(getContext());
					mProgressDialog.setTitle(R.string.delete_sim_contacts);
					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					mProgressDialog.setProgress(0);
					mProgressDialog.setCanceledOnTouchOutside(false);
					mProgressDialog.setOnCancelListener( new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							// TODO Auto-generated method stub
							deleteTask.cancel(true);
							ResumeDelete = false;
							Message msg = new Message();
							msg.what = TOAST_DELETE;
							nHandler.sendMessage(msg);
						}
					});
					mProgressDialog.setMax(mSelectedSimContactSet.size());
					toDeleteCount = mSelectedSimContactSet.size();
					mProgressDialog.show();

				}
                mode.finish();
                break;

            case R.id.import_sim_to_contact:
				final AccountTypeManager accountTypes = AccountTypeManager
						.getInstance(getActivity());
				final List<AccountWithDataSet> accountList = accountTypes
						.getAccounts(true);
				if (accountList.size() == 0) {
					mAccount = null;
					if (!mSelectedSimContactSet.isEmpty()) {
						SimContact[] simContacts = new SimContact[mSelectedSimContactSet
								.size()];
						mSelectedSimContactSet.toArray(simContacts);
						mImportAsyncTask = new importAsyncTask();
						mImportAsyncTask.execute(simContacts);
						mProgressDialog = new ProgressDialog(getContext());
						mProgressDialog.setTitle(R.string.import_sim_contacts);
						mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						mProgressDialog.setProgress(0);
						mProgressDialog.setCanceledOnTouchOutside(false);
						mProgressDialog.setOnCancelListener( new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								// TODO Auto-generated method stub
								mImportAsyncTask.cancel(true);
								ResumeImport = false;
								Message msg = new Message();
								msg.what = TOAST_IMPORT;
								nHandler.sendMessage(msg);
							}
						});
						mProgressDialog.setMax(mSelectedSimContactSet.size());
						toDeleteCount = mSelectedSimContactSet.size();
						mProgressDialog.show();
						
					}
				} else if (accountList.size() == 1) {
					mAccount = accountList.get(0);
					if (!mSelectedSimContactSet.isEmpty()) {
						SimContact[] simContacts = new SimContact[mSelectedSimContactSet
								.size()];
						mSelectedSimContactSet.toArray(simContacts);
						importOneAccountAsyncTask task = new importOneAccountAsyncTask(mAccount);
						task.execute(simContacts);
						mProgressDialog = new ProgressDialog(getContext());
						mProgressDialog.setTitle(R.string.import_sim_contacts);
						mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						mProgressDialog.setProgress(0);
						mProgressDialog.setCanceledOnTouchOutside(false);
						mProgressDialog.setOnCancelListener( new OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								// TODO Auto-generated method stub
								ResumeImport = false;
								Message msg = new Message();
								msg.what = TOAST_IMPORT;
								nHandler.sendMessage(msg);
							}
						});
						mProgressDialog.setMax(mSelectedSimContactSet.size());
						toDeleteCount = mSelectedSimContactSet.size();
						mProgressDialog.show();
					}
				} else {
					if (!mSelectedSimContactSet.isEmpty()) {
						simContactsForMultiAccountImport = new SimContact[mSelectedSimContactSet
								.size()];
						mSelectedSimContactSet.toArray(simContactsForMultiAccountImport);

					}
					toDeleteCount = mSelectedSimContactSet.size();
					
					getActivity().startActivityForResult(
							new Intent(getActivity(),
									SelectAccountActivity.class),
							SELECT_ACCOUNT_FOR_TOTALIMPORT);
				}
            	
            	mode.finish();
            	break;
            default:
                break;
        }
        return true; 
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mSelectedSimContactSet.clear();
            View itemView;
            ListView listView = getListView();
            int childCount = listView.getChildCount();
            for(int i = 0; i < childCount; i++) {
                itemView = listView.getChildAt(i);
                if(itemView instanceof ContactListItemView) {
//                    ((ContactListItemView)itemView).setSelection(false);
                }
            }
			
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			ListView listView = getListView();

            int adjPosition = position - listView.getHeaderViewsCount();
            ContactListItemView itemView = (ContactListItemView) listView
                    .findViewById(adjPosition);
//            itemView.setSelection(checked);

            final int checkedCount = listView.getCheckedItemCount();
            mSelectedConvCount.setText(Integer.toString(checkedCount));

            SimContact simContact = mSimContactList.get(adjPosition);
//            Uri uri = getAdapter().getContactUri(adjPosition);
            if (checked) {
//                mSelectedUriSet.add(uri);
            	mSelectedSimContactSet.add(simContact);
            	Log.d("^^", "Name:" + simContact.mName + " ID:" + simContact.mId + "added");
            } else {
//                mSelectedUriSet.remove(uri);
            	mSelectedSimContactSet.remove(simContact);
            	Log.d("^^", "Name:" + simContact.mName + " ID:" + simContact.mId + "removed");
            } 
			
		}}
    //end: added by yunzhou.song
    
	private void importOneContactWhenNoMultipickAccount(String str1, String str2) {

		ContentValues values = new ContentValues();
		Uri rawContactUri = getContext().getContentResolver().insert(
				RawContacts.CONTENT_URI, values);
		long rawContactId = ContentUris.parseId(rawContactUri);

		if (!str1.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			values.put(StructuredName.GIVEN_NAME, str1);
			getContext().getContentResolver().insert(
					ContactsContract.Data.CONTENT_URI, values);
		}

		if (!str2.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			values.put(Phone.NUMBER, str2);
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			getContext().getContentResolver().insert(
					ContactsContract.Data.CONTENT_URI, values);
		}

	}
	
	private void importOneContactWhenMultipickAccount(String str1 , String str2 ,String str3 ,String str4) {

		ContentValues values = new ContentValues();
		values.put(RawContacts.ACCOUNT_NAME, str3);
		values.put(RawContacts.ACCOUNT_TYPE, str4);
		Uri rawContactUri = getContext().getContentResolver().insert(
				RawContacts.CONTENT_URI, values);
		long rawContactId = ContentUris.parseId(rawContactUri);

		if (!str1.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			values.put(StructuredName.GIVEN_NAME, str1);
			getContext().getContentResolver().insert(ContactsContract.Data.CONTENT_URI,
					values);
		}

		if (!str2.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			values.put(Phone.NUMBER, str2);
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			getContext().getContentResolver().insert(ContactsContract.Data.CONTENT_URI,
					values);
		}

	}

	
	public class deleteAsyncTask extends
			AsyncTask<SimContact[], Integer, Integer> {

		@Override
		protected Integer doInBackground(SimContact[]... params) {
			ResumeDelete = true;
			successCount = 0;
			failedCount = 0;
			for(int i = 0 ; i < params[0].length ; i++){
				Log.d("^^", "to delete id:" + params[0][i].mId);
				if(ResumeDelete){
					boolean success = adapter.deleteOneContact(params[0][i].mId);
					publishProgress(i+1);
					if(success){
						successCount ++;
					}else{
						failedCount ++;
					}
				}else{
					mSIMCursorLoader.forceLoad();
					beforeUpdateIndexer();
					break;
				}
			}
			
			
			
			Message msg = new Message();
			msg.what = TOAST_DELETE;
			nHandler.sendMessage(msg);
			
			return null;
		}
		

		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mSIMCursorLoader.forceLoad();
			beforeUpdateIndexer();
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			if(mProgressDialog != null){
				mProgressDialog.setProgress(values[0]);
				if(values[0] == toDeleteCount){
					mProgressDialog.dismiss();
				}
			}
			super.onProgressUpdate(values);
		}
	}

	public class importAsyncTask extends
			AsyncTask<SimContact[], Integer, Integer> {


		@Override
		protected Integer doInBackground(SimContact[]... params) {
			ResumeImport = true;
			successCount = 0;
			failedCount = 0;
			
			for(int i = 0 ; i < params[0].length ; i++){
				if(ResumeImport){
					importOneContactWhenNoMultipickAccount(params[0][i].mName,
							params[0][i].mNumber);
					publishProgress(i+1);
						successCount ++;
				}
			}
			
			
			Message msg = new Message();
			msg.what = TOAST_IMPORT;
			nHandler.sendMessage(msg);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mSIMCursorLoader.forceLoad();
			beforeUpdateIndexer();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(mProgressDialog != null){
				mProgressDialog.setProgress(values[0]);
			}
			if(values[0] == toDeleteCount){
				mProgressDialog.dismiss();
			}
			super.onProgressUpdate(values);
		}
	


	}
	
	public class importOneAccountAsyncTask extends
			AsyncTask<SimContact[], Integer, Integer> {

		private AccountWithDataSet nAccount;
		public importOneAccountAsyncTask(AccountWithDataSet account){
			nAccount = account;
		}


		@Override
		protected Integer doInBackground(SimContact[]... params) {
			ResumeImport = true;
			successCount = 0;
			failedCount = 0;
			
			for(int i = 0 ; i < params[0].length ; i++){
				if(ResumeImport){
					importOneContactWhenMultipickAccount(params[0][i].mName,
							params[0][i].mNumber, nAccount.name, nAccount.type);
					publishProgress(i+1);
						successCount ++;
				}
			}
			
			
			Message msg = new Message();
			msg.what = TOAST_IMPORT;
			nHandler.sendMessage(msg);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mSIMCursorLoader.forceLoad();
			beforeUpdateIndexer();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(mProgressDialog != null){
				mProgressDialog.setProgress(values[0]);
			}
			if(values[0] == toDeleteCount){
				mProgressDialog.dismiss();
			}
			super.onProgressUpdate(values);
		}
	

	}
	
	public void startImportMultiPickAccountAsyncTask(AccountWithDataSet account){
		AccountWithDataSet mAccount;
		mAccount = account;
		importMultiPickAccountAsyncTask task = new importMultiPickAccountAsyncTask();
		task.execute(mAccount);
		mProgressDialog = new ProgressDialog(getContext());
		mProgressDialog.setTitle(R.string.import_sim_contacts);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setProgress(0);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener( new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				// TODO Auto-generated method stub
				ResumeImport = false;
				Message msg = new Message();
				msg.what = TOAST_IMPORT;
				nHandler.sendMessage(msg);
			}
		});
		mProgressDialog.setMax(toDeleteCount);
		mProgressDialog.show();
		
	}
	public class importMultiPickAccountAsyncTask extends
			AsyncTask<AccountWithDataSet, Integer, Integer> {

		@Override
		protected Integer doInBackground(AccountWithDataSet... account) {
			ResumeImport = true;
			successCount = 0;
			failedCount = 0;
			for (int i = 0; i < simContactsForMultiAccountImport.length; i++) {
				if (ResumeImport) {
					importOneContactWhenMultipickAccount(simContactsForMultiAccountImport[i].mName,
							simContactsForMultiAccountImport[i].mNumber, account[0].name, account[0].type);
					publishProgress(i + 1);
					successCount++;
				}
			}
			Message msg = new Message();
			msg.what = TOAST_IMPORT;
			nHandler.sendMessage(msg);
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mSIMCursorLoader.forceLoad();
			beforeUpdateIndexer();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if(mProgressDialog != null){
				mProgressDialog.setProgress(values[0]);
			}
			if(values[0] == toDeleteCount){
				mProgressDialog.dismiss();
			}
			super.onProgressUpdate(values);
		}
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		ResumeDelete = false;
		ResumeImport = false;
		if(BROADCAST_REGISTERED == true){
		getActivity().unregisterReceiver(mSIMChangeReceiver);
		}
	}
	
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		continueQuery = false;
		Log.d("^^", "onclosed");
	}
}
