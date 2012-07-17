package com.android.contacts.sim;

import java.util.ArrayList;

import com.android.contacts.util.Constants;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.R;

public class SIMOperationService extends Service{

	private static final String TAG = "SIMOperationService";
	private int[] mOperationArray = null;
	private int mCurrentCount = 0;
	private boolean mCanceled = true;
	private Cursor mCursor;
	private int mCount = 0;
	private NotificationManager mNotificationManager;
	static final ContentValues sEmptyContentValues = new ContentValues();
	private Account mAccount;
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("^^", "--------service created");
		mCanceled = false;
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d("^^", "---------service started");
		super.onStart(intent, startId);
		int operationType = intent.getIntExtra(Constants.SIM_OPERATION_TYPE, Constants.SIM_OPERATION_UNKNOWN);
		mOperationArray = intent.getIntArrayExtra(Constants.SIM_OPERATION_ARRAY);
		if (operationType == Constants.SIM_OPERATION_IMPORT_ALL) {
			final String accountName = intent.getStringExtra("account_name");
			final String accountType = intent.getStringExtra("account_type");
			if (!TextUtils.isEmpty(accountName)
					&& !TextUtils.isEmpty(accountType)) {
				Log.d(TAG, "intent info:" + operationType + " " + accountName + " " + accountType );
				mAccount = new Account(accountName, accountType);
			}
		}
         if(operationType != Constants.SIM_OPERATION_UNKNOWN){
        	 handleRequest(operationType);
         }
         
	}
	
	private void handleRequest(int operationType){
		switch(operationType){
		case Constants.SIM_OPERATION_IMPORT:
			operateImport();
			break;
		case Constants.SIM_OPERATION_EXPORT:
			operateExport();
			break;
		case Constants.SIM_OPERATION_DELETE:
			operateDelete();
			break;
		case Constants.SIM_OPERATION_CANCEL:
			Log.d("^^", "mCanceled = true");
			mCanceled = true;
			break;
		case Constants.SIM_OPERATION_IMPORT_ALL:
			operationImportAll();
			break;
		}
	}
	
	private void operateImport(){
		Log.d("^^", "mCanceled = false");
		Log.d("^^", "operateImport");
	}
	private void operateExport(){
		
	}
	private void operateDelete(){
		Log.d("^^", "mCanceled = false");
		Log.d("^^", "operateDelete");
	}
	
	
	private void operationImportAll(){
		mCursor = getContentResolver().query(Uri.parse("content://icc/adn"), null, null,
				null, null);
		ImportAllSimContactsThread thread = new ImportAllSimContactsThread();
		thread.start();
	}
	private class ImportAllSimContactsThread extends Thread implements
			OnCancelListener {
		public ImportAllSimContactsThread() {
			super("ImportAllSimContactsThread");
			mCanceled = false;
		}

		@Override
		public void run() {
            final ContentValues emptyContentValues = new ContentValues();
            final ContentResolver resolver = getContentResolver();
            mCursor.moveToPosition(-1);
            mNotificationManager.notify(0,  createPrepareNotification());
            while (!mCanceled && mCursor.moveToNext()) {
                actuallyImportOneSimContact(mCursor, resolver, mAccount);
                mCount ++;
                if((mCount % 5) == 0){
                	mNotificationManager.notify(0, createProgressNotification());
                }
            }
        }

		public void onCancel(DialogInterface dialog) {
			mCanceled = true;
		}
	}

    private static void actuallyImportOneSimContact(
            final Cursor cursor, final ContentResolver resolver, Account account) {
        final NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(cursor.getString(1));
        final String name = namePhoneTypePair.name;
        final int phoneType = namePhoneTypePair.phoneType;
        final String phoneNumber = cursor.getString(2);
        final String emailAddresses = null;
        final String[] emailAddressArray;
        
        if (!TextUtils.isEmpty(emailAddresses)) {
            emailAddressArray = emailAddresses.split(",");
        } else {
            emailAddressArray = null;
        }

        final ArrayList<ContentProviderOperation> operationList =
            new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        String myGroupsId = null;
        if (account != null) {
            builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
            builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
        } else {
            builder.withValues(sEmptyContentValues);
        }
        operationList.add(builder.build());

        builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
        builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        builder.withValue(StructuredName.DISPLAY_NAME, name);
        operationList.add(builder.build());

        builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
        builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        builder.withValue(Phone.TYPE, phoneType);
        builder.withValue(Phone.NUMBER, phoneNumber);
        builder.withValue(Data.IS_PRIMARY, 1);
        operationList.add(builder.build());

        if (emailAddresses != null) {
            for (String emailAddress : emailAddressArray) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
                builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
                builder.withValue(Email.DATA, emailAddress);
                operationList.add(builder.build());
            }
        }

        if (myGroupsId != null) {
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(GroupMembership.RAW_CONTACT_ID, 0);
            builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
            builder.withValue(GroupMembership.GROUP_SOURCE_ID, myGroupsId);
            operationList.add(builder.build());
        }

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
    }
    
    private Notification createPrepareNotification(){
    	Notification.Builder builder = new Notification.Builder(SIMOperationService.this);
    	Log.d("^^", "SIMOperationService buidler:" + builder);
    	builder.setOngoing(true)
    	.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(mCount * 100 / mCursor.getCount())))
    	.setContentTitle(getString(R.string.prepareImportSimContacts))
    	.setTicker(getString(R.string.prepareImportSimContacts))
    	.setProgress(mCursor.getCount(), 0, false);
    	return builder.getNotification();
    }
    
    private Notification createProgressNotification(){
    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setOngoing(true)
    	.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(mCount * 100 / mCursor.getCount())))
    	.setContentTitle(getString(R.string.doingImportSimContacts))
    	.setTicker(getString(R.string.doingImportSimContacts))
    	.setProgress(mCursor.getCount(), mCurrentCount, false);
    	return builder.getNotification();
    }
    
    private Notification createFinishNotification(){
    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(mCount * 100 / mCursor.getCount())))
    	.setContentTitle(getString(R.string.finishImportSimContacts))
    	.setTicker(getString(R.string.finishImportSimContacts));
    	return builder.getNotification();
    }
	
    private static class NamePhoneTypePair {
        final String name;
        final int phoneType;
        public NamePhoneTypePair(String nameWithPhoneType) {
            // Look for /W /H /M or /O at the end of the name signifying the type
            int nameLen = nameWithPhoneType.length();
            if (nameLen - 2 >= 0 && nameWithPhoneType.charAt(nameLen - 2) == '/') {
                char c = Character.toUpperCase(nameWithPhoneType.charAt(nameLen - 1));
                if (c == 'W') {
                    phoneType = Phone.TYPE_WORK;
                } else if (c == 'M' || c == 'O') {
                    phoneType = Phone.TYPE_MOBILE;
                } else if (c == 'H') {
                    phoneType = Phone.TYPE_HOME;
                } else {
                    phoneType = Phone.TYPE_OTHER;
                }
                name = nameWithPhoneType.substring(0, nameLen - 2);
            } else {
                phoneType = Phone.TYPE_OTHER;
                name = nameWithPhoneType;
            }
        }
    }
    
	
	private String getSortOrder(String[] projectionType) {
		return Contacts.SORT_KEY_PRIMARY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d("^^", "---------service binded!");
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("^^", "-----service destroyed!");
	}
}
