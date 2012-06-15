/*
 * Copyright (C) 2007 The Android Open Source Project
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

import java.nio.charset.Charset;
import java.util.HashSet;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.sim.SIMContactsPickerFragment;
import com.android.contacts.vcard.SelectAccountActivity;
import com.android.contacts.widget.ContextMenuAdapter;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;

import com.android.internal.telephony.AdnRecord;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.AdnRecordCache;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class SIMContactSelectionActivity extends ContactsActivity implements
		View.OnCreateContextMenuListener, OnQueryTextListener, OnClickListener,
		OnCloseListener , TextWatcher{
	private static final String TAG = "ContactSelectionActivity";
	private static final int SELECT_ACCOUNT = 1;
	private static final int SELECT_ACCOUNT_FOR_TOTALIMPORT = 2;
	private AccountWithDataSet mAccount;

	private static final String KEY_ACTION_CODE = "actionCode";
	private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

	// Delay to allow the UI to settle before making search view visible

	private ContactsIntentResolver mIntentResolver;
	protected SIMContactsPickerFragment mListFragment;

	private int mActionCode = -1;

	private final static int SIM_ADD_SUCCESS = 3;
	private ContactsRequest mRequest;
	int lastStringLength;
	int limitedLength = 0;
	EditText uName;
	// private SearchView mSearchView;
	/**
	 * Can be null. If null, the "Create New Contact" button should be on the
	 * menu.
	 */

	public SIMContactSelectionActivity() {
		mIntentResolver = new ContactsIntentResolver(this);
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		if (fragment instanceof ContactEntryListFragment<?>) {
			mListFragment = (SIMContactsPickerFragment) fragment;
			setupActionListener();
		}
	}


	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
			
		if (savedState != null) {
			mActionCode = savedState.getInt(KEY_ACTION_CODE);
		}

		// Extract relevant information from the intent
		mRequest = mIntentResolver.resolveIntent(getIntent());
		if (!mRequest.isValid()) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		setTitle(getString(R.string.sim_manager));
		setContentView(R.layout.sim_manager);

		if (mActionCode != mRequest.getActionCode()) {
			mActionCode = mRequest.getActionCode();
			configureListFragment();
		}

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(R.string.sim_manager);

		
	}

	private Handler nHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1: {
				break;
			}
			case 2: {
				String formatedText = uName.getText().toString()
						.substring(0, lastStringLength);
				uName.setText(formatedText);
				uName.setSelection(uName.getText().toString().length());
				break;
			}
			case SIM_ADD_SUCCESS:{
				Toast.makeText(SIMContactSelectionActivity.this, R.string.sim_add_success, Toast.LENGTH_SHORT).show();
				break;
			}
			}

		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// If we want "Create New Contact" button but there's no such a button
		// in the layout,
		// try showing a menu for it.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sim_manager_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Go back to previous screen, intending "cancel"
			setResult(RESULT_CANCELED);
			finish();
			return true;
		case R.id.sim_contacts_add:
			LayoutInflater inflater = LayoutInflater
					.from(SIMContactSelectionActivity.this);
			View DialogView = inflater.inflate(R.layout.modify_dialog, null);
			uName = (EditText) DialogView.findViewById(R.id.username);
			uName.addTextChangedListener(this);
			final EditText uNumber = (EditText) DialogView
					.findViewById(R.id.usernumber);
			final AlertDialog dlg = new AlertDialog.Builder(
					SIMContactSelectionActivity.this)
					.setTitle(R.string.new_sim_contact)
					.setView(DialogView)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									final String currentName = uName.getText()
											.toString();
									final String currentNumber = uNumber
											.getText().toString();
									new Thread() {
										public void run() {
											try {
												boolean success = mListFragment.exportOneContact(currentName,
														currentNumber);
												if(success == true){
													Message msg = new Message();
													msg.what = SIM_ADD_SUCCESS;
													nHandler.sendMessage(msg);
												}
												Intent intent = new Intent(
														"SIMChange");
												sendBroadcast(intent);
											} catch (Exception e) {
												Log.e(TAG, e.toString());
											}

										}
									}.start();
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
			startQueryLimited queryLimited = new startQueryLimited();
			queryLimited.start();

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_ACTION_CODE, mActionCode);
	}

	/**
	 * Creates the fragment based on the current request.
	 */
	public void configureListFragment() {

		mListFragment = new SIMContactsPickerFragment();

		mListFragment.setLegacyCompatibilityMode(mRequest
				.isLegacyCompatibilityMode());
		mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

		getFragmentManager().beginTransaction()
				.replace(R.id.list_container, mListFragment)
				.commitAllowingStateLoss();
	}

	public void setupActionListener() {
		mListFragment
				.setOnPhoneNumberPickerActionListener(new PhoneNumberPickerActionListener());
		mListFragment.startWatchSIMContactsModify();
	}

	private final class PhoneNumberPickerActionListener implements
			OnPhoneNumberPickerActionListener {
		@Override
		public void onPickPhoneNumberAction(Uri dataUri) {
			returnPickerResult(dataUri);
		}

		@Override
		public void onShortcutIntentCreated(Intent intent) {
			returnPickerResult(intent);
		}

		public void onHomeInActionBarSelected() {
			SIMContactSelectionActivity.this.onBackPressed();
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ContextMenuAdapter menuAdapter = mListFragment.getContextMenuAdapter();
		if (menuAdapter != null) {
			return menuAdapter.onContextItemSelected(item);
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		mListFragment.setQueryString(newText, true);
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	@Override
	public boolean onClose() {
		return true;
	}

	public void returnPickerResult(Uri data) {
		Intent intent = new Intent();
		intent.setData(data);
		returnPickerResult(intent);
	}

	public void returnPickerResult(Intent intent) {
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case android.R.drawable.ic_input_add: {

		}
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

		int charLength = uName.getText().toString().length();
		boolean isASCII[] = new boolean[charLength];
		boolean isASCIII = true;
		for (int i = 0; i < charLength; i++) {
			isASCII[i] = (uName.getText().toString().charAt(i) <= 127 && uName
					.getText().toString().charAt(i) >= 0);
		}
		for (int i = 0; i < charLength; i++) {
			isASCIII = isASCIII & isASCII[i];
		}
		Message msg = new Message();
		msg.what = 2;
		if (isASCIII) {
			if (uName.getText().toString()
					.getBytes(Charset.forName("US-ASCII")).length > limitedLength) {
				nHandler.sendMessage(msg);
			} else {
				lastStringLength = uName.getText().toString().length();
			}
		} else {
			if (uName.getText().toString().getBytes(Charset.forName("Unicode")).length > limitedLength) {

				nHandler.sendMessage(msg);
			} else {
				lastStringLength = uName.getText().toString().length();
			}
		}

	}

	private int getLimitedLength() throws RemoteException {
		IIccPhoneBook simPhoneBook = IIccPhoneBook.Stub
				.asInterface(ServiceManager.getService("simphonebook"));
		int[] index = simPhoneBook.getAdnRecordsSize(IccConstants.EF_ADN);
		int indexLimited = index[2];
		int indexLimitedLength = index[0] - 14;
		return indexLimitedLength;
	}

	private class startQueryLimited extends Thread {
		public startQueryLimited() {
			super("startQueryLimited");
		}

		@Override
		public void run() {
			try {
				limitedLength = getLimitedLength();
			} catch (RemoteException e) {
			}

		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SELECT_ACCOUNT) {
			if (resultCode == RESULT_OK) {
				String accountName = intent.getStringExtra(SelectAccountActivity.ACCOUNT_NAME);
				String accountType = intent.getStringExtra(SelectAccountActivity.ACCOUNT_TYPE);
				String accountData = intent.getStringExtra(SelectAccountActivity.DATA_SET);
				
				mAccount = new AccountWithDataSet(accountName, accountType,
						accountData);
				String name = mListFragment.nameForActivity;
				String number = mListFragment.numberForActivity;
				importOneContactWhenMultipickAccount(name, number ,accountName, accountType);
			} else {
//				finish();
			}
		}else if(requestCode == SELECT_ACCOUNT_FOR_TOTALIMPORT) {
			if (resultCode == RESULT_OK) {
				String accountName = intent.getStringExtra(SelectAccountActivity.ACCOUNT_NAME);
				String accountType = intent.getStringExtra(SelectAccountActivity.ACCOUNT_TYPE);
				String accountData = intent.getStringExtra(SelectAccountActivity.DATA_SET);
				
				mAccount = new AccountWithDataSet(accountName, accountType,
						accountData);
				mListFragment.startImportMultiPickAccountAsyncTask(mAccount);
			} else {
//				finish();
			}
		}
		
	}
	private void importOneContactWhenMultipickAccount(String str1 , String str2 ,String str3 ,String str4) {

		ContentValues values = new ContentValues();
		values.put(RawContacts.ACCOUNT_NAME, str3);
		values.put(RawContacts.ACCOUNT_TYPE, str4);
		Uri rawContactUri = getContentResolver().insert(
				RawContacts.CONTENT_URI, values);
		long rawContactId = ContentUris.parseId(rawContactUri);

		if (!str1.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			values.put(StructuredName.GIVEN_NAME, str1);
			getContentResolver().insert(ContactsContract.Data.CONTENT_URI,
					values);
		}

		if (!str2.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			values.put(Phone.NUMBER, str2);
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			getContentResolver().insert(ContactsContract.Data.CONTENT_URI,
					values);
		}

	}
	
}
