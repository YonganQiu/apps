package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.Collapser.Collapsible;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.util.Constants;

public class OneKeyDialSetting extends Activity {
	private final static String TAG = OneKeyDialSetting.class.getSimpleName();
	private final static int PICK_PHONE = 1001;

	private String mKey;
	private ContactPhotoManager mContactPhotoManager;
	private int[] mContactIds = { R.id.contact2, R.id.contact3, R.id.contact4,
			R.id.contact5, R.id.contact6, R.id.contact7, R.id.contact8,
			R.id.contact9 };
	private int[] mPhotoIds = { R.id.photo2, R.id.photo3, R.id.photo4,
			R.id.photo5, R.id.photo6, R.id.photo7, R.id.photo8, R.id.photo9 };
	private int[] mNumberIds = { R.id.number2, R.id.number3, R.id.number4,
			R.id.number5, R.id.number6, R.id.number7, R.id.number8,
			R.id.number9 };
	private int[] mPushStateIds = { R.id.push_state2, R.id.push_state3,
			R.id.push_state4, R.id.push_state5, R.id.push_state6,
			R.id.push_state7, R.id.push_state8, R.id.push_state9 };

	private TextView[] mNumTextViews = new TextView[mNumberIds.length];

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContactPhotoManager = ContactPhotoManager.getInstance(this);

		initContentView();
		initContentData();

		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		initContentView();
		initContentData();
	}

	private void initContentView() {
		setContentView(R.layout.one_key_dial_setting);

		for (int i = 0; i < mPhotoIds.length; i++) {
			((ImageView) findViewById(mPhotoIds[i]))
					.setImageResource(ContactPhotoManager
							.getDefaultAvatarResId(true, false));
		}

		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.contact2:
					mKey = "2";
					break;
				case R.id.contact3:
					mKey = "3";
					break;
				case R.id.contact4:
					mKey = "4";
					break;
				case R.id.contact5:
					mKey = "5";
					break;
				case R.id.contact6:
					mKey = "6";
					break;
				case R.id.contact7:
					mKey = "7";
					break;
				case R.id.contact8:
					mKey = "8";
					break;
				case R.id.contact9:
					mKey = "9";
					break;
				}
				if (!TextUtils.isEmpty(mKey)) {
					displaySetNumberDialog();
				}
			}
		};

		for (int i = 0; i < mContactIds.length; i++) {
			findViewById(mContactIds[i]).setOnClickListener(listener);
		}

		listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Integer index = (Integer) v.getTag();
				if (index != null) {
					View view = findViewById(mContactIds[index]);
					if (view != null) {
						view.performClick();
					}
				}
			}
		};

		View view;
		for (int i = 0; i < mPushStateIds.length; i++) {
			view = findViewById(mPushStateIds[i]);
			view.setTag(i);
			view.setOnClickListener(listener);
		}
	}

	private void initContentData() {
		PhoneItem phoneItem1, phoneItem2;
		SharedPreferences mSharedPref = getSharedPreferences(Constants.ONE_KEY_DIAL_SETTING_SHARED_PREFS_NAME,
				Context.MODE_WORLD_READABLE);
		for (int i = 0; i < mNumTextViews.length; i++) {
			mNumTextViews[i] = (TextView) findViewById(mNumberIds[i]);
			mKey = String.valueOf(i + 2);
			phoneItem1 = getPhoneItem(mKey, mSharedPref, this);
			if (phoneItem1 != null) {
				phoneItem2 = queryPhone(phoneItem1.mId, phoneItem1.mContactId,
						this);
				if (phoneItem2 != null) {
					phoneItem1.mPhotoId = phoneItem2.mPhotoId;
					phoneItem1.mPhoneNumber = phoneItem2.mPhoneNumber;
					phoneItem1.mDisplayName = phoneItem2.mDisplayName;
					savePhoneItem(phoneItem1);
				}
				updateView(mKey, phoneItem1);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK && requestCode == PICK_PHONE) {
			Uri uri = data.getData();
			Cursor cursor = queryPhones(uri);
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long id = cursor.getLong(cursor.getColumnIndex(Phone._ID));
					long contactId = cursor.getLong(cursor
							.getColumnIndex(Phone.CONTACT_ID));
					long photoId = cursor.getLong(cursor
							.getColumnIndex(Phone.PHOTO_ID));
					String phoneNumber = cursor.getString(cursor
							.getColumnIndex(Phone.NUMBER));
					String displayName = cursor.getString(cursor
							.getColumnIndex(Phone.DISPLAY_NAME));

					PhoneItem phoneItem = new PhoneItem(id, contactId, photoId,
							phoneNumber, displayName, null, 0, null, this);
					savePhoneItem(phoneItem);
					updateView(mKey, phoneItem);
					cursor.close();
					return;
				}
				cursor.close();
				return;
			}
			Toast.makeText(this, R.string.noNumberSelectedContact,
					Toast.LENGTH_LONG).show();
		}
	}

	private void displaySetNumberDialog() {
		// Wrap our context to inflate list items using correct theme
		final Context dialogContext = new ContextThemeWrapper(this,
				android.R.style.Theme_Light);
		final LayoutInflater dialogInflater = (LayoutInflater) dialogContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final SharedPreferences sharedPref = getSharedPreferences(
				Constants.ONE_KEY_DIAL_SETTING_SHARED_PREFS_NAME, Context.MODE_WORLD_READABLE);

		// Adapter that shows a list of string resources
		final ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,
				android.R.layout.simple_list_item_1) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = dialogInflater.inflate(
							android.R.layout.simple_list_item_1, parent, false);
				}

				final int resId = this.getItem(position);
				if (resId == R.string.clear) {
					PhoneItem phoneItem = getPhoneItem(mKey, sharedPref,
							OneKeyDialSetting.this);
					if (phoneItem != null) {
						((TextView) convertView)
								.setText(OneKeyDialSetting.this.getString(
										resId, mNumTextViews[Integer
												.valueOf(mKey) - 2].getText()));
						return convertView;
					}
				}
				((TextView) convertView).setText(resId);
				return convertView;
			}
		};

		adapter.add(R.string.selectFromContacts);
		adapter.add(R.string.inputByHand);
		if (sharedPref.contains(mKey)) {
			adapter.add(R.string.clear);
		}

		final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();

				final int resId = adapter.getItem(which);
				switch (resId) {
				case R.string.selectFromContacts: {
					Intent intent = new Intent(Intent.ACTION_PICK);
					intent.setType(Phone.CONTENT_TYPE);
					startActivityForResult(intent, PICK_PHONE);
					break;
				}
				case R.string.inputByHand: {
					createInputByHandDialog().show();
					break;
				}
				case R.string.clear: {
					clearPhoneItem();
					updateView(
							mKey,
							getPhoneItem(mKey, sharedPref,
									OneKeyDialSetting.this));
					break;
				}
				default: {
					Log.e(TAG, "Unexpected resource: "
							+ getResources().getResourceEntryName(resId));
				}
				}
			}
		};

		new AlertDialog.Builder(this).setTitle(R.string.setNumber)
				.setNegativeButton(android.R.string.cancel, null)
				.setSingleChoiceItems(adapter, -1, clickListener).show();
	}

	private Dialog createInputByHandDialog() {
		final EditText numView = new EditText(this);
		numView.setPadding(20, 50, 20, 20);
		numView.setImeOptions(EditorInfo.IME_ACTION_DONE);
		numView.setInputType(EditorInfo.TYPE_CLASS_PHONE);
		numView.requestFocus();

		final SharedPreferences sharedPref = getSharedPreferences(
				Constants.ONE_KEY_DIAL_SETTING_SHARED_PREFS_NAME, Context.MODE_WORLD_READABLE);
		PhoneItem phoneItem = getPhoneItem(mKey, sharedPref, this);
		if (phoneItem != null) {
			numView.setText(phoneItem.mPhoneNumber);
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.inputByHand);
		builder.setView(numView);

		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String phoneNumber = numView.getText().toString()
								.trim();
						PhoneItem phoneItem = getPhoneItem(mKey, sharedPref,
								OneKeyDialSetting.this);
						if (phoneItem == null) {
							phoneItem = new PhoneItem(-1, -1, -1, null, null,
									null, 0, null, OneKeyDialSetting.this);
							;
						}
						if (TextUtils.isEmpty(phoneNumber)) {
							return;
						} else if (!phoneNumber.equals(phoneItem.mPhoneNumber)) {
							phoneItem.mId = -1;
							phoneItem.mContactId = -1;
							phoneItem.mPhotoId = -1;
							phoneItem.mPhoneNumber = phoneNumber;
						}
						savePhoneItem(phoneItem);
						updateView(mKey, phoneItem);
					}
				});

		builder.setNegativeButton(android.R.string.cancel, null);

		return builder.create();
	}

	public void clearPhoneItem() {
		SharedPreferences sharedPref = getSharedPreferences(Constants.ONE_KEY_DIAL_SETTING_SHARED_PREFS_NAME,
				Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.remove(mKey);
		editor.remove(Constants.PERF_KEY_CONTACT_ID + mKey);
		editor.remove(Constants.PERF_KEY_NUMBER + mKey);
		editor.remove(Constants.PERF_KEY_PHONE_ID + mKey);
		editor.commit();
	}

	private PhoneItem getPhoneItem(String key, SharedPreferences sharedPref,
			Context context) {
		if (!TextUtils.isEmpty(key) && sharedPref != null
				&& sharedPref.contains(key)) {
			long contactId = sharedPref.getLong(Constants.PERF_KEY_CONTACT_ID
					+ key, -1);
			long id = sharedPref.getLong(Constants.PERF_KEY_PHONE_ID + key, -1);
			String phoneNumber = sharedPref.getString(key, null);
			return new PhoneItem(id, contactId, -1, phoneNumber, null, null, 0,
					null, context);
		}
		return null;
	}

	private PhoneItem queryPhone(long phoneId, long contactId, Context context) {
		PhoneItem phoneItem = null;
		if (contactId > 0 && phoneId > 0) {
			ContentResolver resolver = context.getContentResolver();
			String[] projection = new String[] { Phone._ID, // 0
					Phone.CONTACT_ID, // 1
					Phone.PHOTO_ID, // 2
					Phone.NUMBER, // 3
					Phone.DISPLAY_NAME, // 4
					RawContacts.ACCOUNT_TYPE,// 5
					Phone.TYPE, // 6
					Phone.LABEL, // 7
			};

			Cursor cursor = resolver.query(Phone.CONTENT_URI, projection,
					Phone._ID + "=" + phoneId + " AND " + Phone.CONTACT_ID
							+ "=" + contactId, null, null);
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long id = cursor.getLong(cursor.getColumnIndex(Phone._ID));
					long photoId = cursor.getLong(cursor
							.getColumnIndex(Phone.PHOTO_ID));
					String phoneNumber = cursor.getString(cursor
							.getColumnIndex(Phone.NUMBER));
					String displayName = cursor.getString(cursor
							.getColumnIndex(Phone.DISPLAY_NAME));
					String accountType = cursor.getString(cursor
							.getColumnIndex(RawContacts.ACCOUNT_TYPE));
					int type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));
					String label = cursor.getString(cursor
							.getColumnIndex(Phone.LABEL));

					phoneItem = new PhoneItem(id, contactId, photoId,
							phoneNumber, displayName, accountType, type, label,
							context);
				}
				cursor.close();
			}
		}
		return phoneItem;
	}

	public Cursor queryPhones(Uri phoneUri) {
		if (phoneUri != null) {
			ContentResolver resolver = getContentResolver();
			String[] projection = new String[] { Phone._ID, // 0
					Phone.CONTACT_ID, // 1
					Phone.PHOTO_ID, // 2
					Phone.NUMBER, // 3
					Phone.DISPLAY_NAME, // 4
					RawContacts.ACCOUNT_TYPE, // 5
					Phone.TYPE, // 6
					Phone.LABEL, // 7
			};

			Cursor cursor = resolver.query(phoneUri, projection, null, null,
					null);
			if (cursor != null) {
				return cursor;
			}
		}
		return null;
	}

	private void savePhoneItem(PhoneItem phoneItem) {
		final SharedPreferences sharedPref = getSharedPreferences(
				Constants.ONE_KEY_DIAL_SETTING_SHARED_PREFS_NAME, Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(mKey, phoneItem.mPhoneNumber);
		editor.putLong(Constants.PERF_KEY_CONTACT_ID + mKey,
				phoneItem.mContactId);
		editor.putLong(Constants.PERF_KEY_PHONE_ID + mKey, phoneItem.mId);
		editor.commit();
	}

	private void updateView(String key, PhoneItem phoneItem) {
		int index = Integer.valueOf(key) - 2;
		if (index >= 0 && index < mNumTextViews.length) {
			if (phoneItem != null) {
				mNumTextViews[index]
						.setText(TextUtils.isEmpty(phoneItem.mDisplayName) ? phoneItem.mPhoneNumber
								: phoneItem.mDisplayName);
				if (phoneItem.mPhotoId > 0) {
					mContactPhotoManager.loadPhoto(
							(ImageView) findViewById(mPhotoIds[index]),
							ContentUris.withAppendedId(Data.CONTENT_URI,
									phoneItem.mPhotoId), true, true);
				} else {
					((ImageView) findViewById(mPhotoIds[index]))
							.setImageResource(ContactPhotoManager
									.getDefaultAvatarResId(true, false));
				}
			} else {
				mNumTextViews[index].setText(R.string.unset);
				((ImageView) findViewById(mPhotoIds[index]))
						.setImageResource(ContactPhotoManager
								.getDefaultAvatarResId(true, false));
			}
		}
	}

	public static class PhoneItem implements Collapsible<PhoneItem> {
		public long mId;
		public long mContactId;
		public long mPhotoId;
		public String mPhoneNumber;
		public String mDisplayName;
		public String mAccountType;
		public long mType;
		public String mLabel;
		public Context mContext;

		public PhoneItem(long id, long contactId, long photoId,
				String phoneNumber, String displayName, String accountType,
				int type, String label, Context context) {
			mId = id;
			mContactId = contactId;
			mPhotoId = photoId;
			mPhoneNumber = phoneNumber;
			mDisplayName = displayName;
			mAccountType = accountType;
			mType = type;
			mLabel = label;
			mContext = context;
		}

		public boolean collapseWith(PhoneItem phoneItem) {
			if (!shouldCollapseWith(phoneItem)) {
				return false;
			}
			// Just keep the number and id we already have.
			return true;
		}

		public boolean shouldCollapseWith(PhoneItem phoneItem) {
			if (PhoneNumberUtils.compare(mContext, mPhoneNumber,
					phoneItem.mPhoneNumber)) {
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return mPhoneNumber;
		}
	}
}