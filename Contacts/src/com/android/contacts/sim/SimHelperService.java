package com.android.contacts.sim;

import java.util.ArrayList;
import java.util.Iterator;

import com.android.contacts.ContactsApplication;
import com.android.contacts.util.Constants;

import com.android.contacts.R;
import android.accounts.Account;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

public class SimHelperService extends IntentService{
	private static final String TAG = "SimHelperService";
	
	public static final String ACTION_IMPORT = "com.android.contacts.action_import";
	public static final String ACTION_EXPORT = "com.android.contacts.action_export";
	public static final String ACTION_DELETE = "com.android.contacts.action_delete";
	public static final String ACTION_CANCEL = "com.android.contacts.action_cancel";
	public static final String ACTION_IMPORT_ALL = "com.android.contacts.action_import_all";
	public static final String ACTION_EXPORT_ALL = "com.android.contacts.action_export_all";
	public static final String ACTION_PREPARE = "com.android.contacts.action_prepare";
	
	public static final String EXTRA_ACCOUNT_NAME = "account_name";
	public static final String EXTRA_ACCOUNT_TYPE = "account_type";
	private boolean isShowNotification = true;
    private static final String[] COLUMN_NAMES = new String[] {
        "display_name",
        "data1"
    };
    
    static final ContentValues sEmptyContentValues = new ContentValues();
    private NotificationManager mNotificationManager;
	public static class OperationContact{
		public String simId;
		public String operationName;
		public String operationNumber;
		
		public OperationContact(String id, String name, String number){
			this.simId = id;
			this.operationName = name;
			this.operationNumber = number;
		}
		
	}
	
	
	

	public SimHelperService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if(mNotificationManager == null){
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		}
		String action = intent.getAction();
		Log.d(TAG, "receive the intnet action: " + action);
		if(ACTION_IMPORT.equals(action)){
			
		}else if (ACTION_EXPORT.equals(action)){
			
		}else if(ACTION_DELETE.equals(action)){
			
		}else if(ACTION_CANCEL.equals(action)){
			
		}else if(ACTION_IMPORT_ALL.equals(action)){
			isShowNotification = true;
			importAllContacts(intent);
		}else if(ACTION_EXPORT_ALL.equals(action)){
			
		}else if(ACTION_PREPARE.equals(action)){
			isShowNotification = false;
			prepareSimContacts();
		}
	}

	private void prepareSimContacts() {
		deleteAllSimContacts(getContentResolver());
		Cursor cur = startQuery();
		if (cur.getCount() <= 0) {
			return;
		}
		try {
			cur.moveToPosition(-1);
			ArrayList<OperationContact> operationContactFromSim = loadSimRecordsFromCursor(cur);
			importBatch(operationContactFromSim, getContentResolver(),getSimAccount());
		} catch (Exception e) {
			e.printStackTrace();
		}
		((ContactsApplication) getApplication()).SIMPreparing = false;
		sendBroadcast();
		cur.close();
	}
	
	private void importAllContacts(Intent intent){
		Cursor cur = startQuery();
		if (cur.getCount() <= 0) {
			return;
		}
		mNotificationManager.notify(0, createPrepareNotification(0, cur.getCount()));
		try {
			cur.moveToPosition(-1);
			ArrayList<OperationContact> operationContactFromSim = loadSimRecordsFromCursor(cur);
			importBatch(operationContactFromSim, getContentResolver(),new Account(intent.getStringExtra(EXTRA_ACCOUNT_NAME), intent.getStringExtra(EXTRA_ACCOUNT_TYPE)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		mNotificationManager.notify(0, createFinishNotification(cur.getCount(), cur.getCount()));
		((ContactsApplication) getApplication()).SIMPreparing = false;
		sendBroadcast();
		cur.close();
	}
	
    class SimRecord {
        String name;
        String number;
        SimRecord(String name, String number) {
            this.name = name;
            this.number = number;
            Log.i(TAG, "name = " + name + ", number = " + number);
        }
    }

    private ArrayList<OperationContact> loadSimRecordsFromCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        int count = cursor.getCount();
        if (count <= 0) {
            return null;
        }
        ArrayList<OperationContact> operationList = new ArrayList<OperationContact>();
        int i = 0;
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
        	operationList.add(new OperationContact(null, cursor.getString(0) ,cursor.getString(1)));
        }
        
        return operationList;
    }

    private void importBatch(ArrayList<OperationContact> operationContact, ContentResolver resolver, Account account) {
        if (operationContact == null || operationContact.size() <= 0) {
            return;
        }

        int count = operationContact.size();
        
        ContentValues values = new ContentValues();
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        ContentProviderResult[] results = null;
        ContentProviderOperation operation;
        for (int i = 0; i < count; i++) {
            values.clear();
            if(account != null){
                values.put(RawContacts.ACCOUNT_NAME, account.name);
                values.put(RawContacts.ACCOUNT_TYPE, account.type);
               }
             operation = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                     .withValues(values).build();
             operations.add(operation);
        }

        try {
            results = resolver.applyBatch(ContactsContract.AUTHORITY, operations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

        operations.clear();
        long rawContactId;

        for (int i = 0; i < count; i++) {
            OperationContact OneOperationContact = operationContact.get(i);
            rawContactId = ContentUris.parseId(results[i].uri);
            Log.i(TAG, "importing name = " + OneOperationContact.operationName + ", number = " + OneOperationContact.operationNumber + ", id = " + rawContactId);
            if (!TextUtils.isEmpty(OneOperationContact.operationName)) {
                values.clear();
                values.put(Data.RAW_CONTACT_ID, rawContactId);
                values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                values.put(StructuredName.GIVEN_NAME, OneOperationContact.operationName);
                operation = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValues(values).build();
                operations.add(operation);
            }

            if (!TextUtils.isEmpty(OneOperationContact.operationNumber)) {
                values.clear();
                values.put(Data.RAW_CONTACT_ID, rawContactId);
                values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                values.put(Phone.NUMBER, OneOperationContact.operationNumber);
                values.put(Phone.TYPE, Phone.TYPE_MOBILE);
                operation = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValues(values).build();
                operations.add(operation);
            }

            if (operations.size() >= 450) {
                try {
                    resolver.applyBatch(ContactsContract.AUTHORITY, operations);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                operations.clear();
            }
            if(isShowNotification && i % 20 == 0 ){
            	mNotificationManager.notify(0, createProgressNotification(i, count));
            }
        }

        if (operations.size() > 0) {
            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, operations);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }
    } 
    
    private static void importOneSimContact(
            final Cursor cursor, final ContentResolver resolver, Account account) {
        final NamePhoneTypePair namePhoneTypePair =
                new NamePhoneTypePair(cursor.getString(cursor.getColumnIndex("display_name")));
            final String name = namePhoneTypePair.name;
            final int phoneType = namePhoneTypePair.phoneType;
            final String phoneNumber = cursor.getString(cursor.getColumnIndex("data1"));
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


            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (RemoteException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (OperationApplicationException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
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
    
    private void sendBroadcast(){
    	Intent intent = new Intent();
    	intent.setAction(Constants.SIM_READING_STATE);
    	Log.d("^^", "simprepareservice sendBroadcast");
    	sendBroadcast(intent);
    }
    
    private Cursor startQuery() {
        Uri uri = getUri();
        Log.d(TAG, "start query:" + uri);
        Cursor cur = getContentResolver().query( getUri(), COLUMN_NAMES, null, null, null);
        ((ContactsApplication)getApplication()).SIMPreparing = true;
        sendBroadcast();
        return cur;
        
    }
    
	private Uri getUri() {
		return Uri.parse("content://icc/adn");
	}
	
	private Account getSimAccount(){
		return new Account("sim1", "sim");
	}
	
    private void deleteAllSimContacts (final ContentResolver resolver){
    	final ArrayList<ContentProviderOperation> operationList =
                new ArrayList<ContentProviderOperation>();
    	
    	 ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(RawContacts.CONTENT_URI);
        builder.withSelection(RawContacts.ACCOUNT_TYPE + "==?",new String[]{"sim"});
        operationList.add(builder.build());
        
        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        Log.d(TAG, "delete sim task!");
    }
    
    private Notification createPrepareNotification(int currentCount, int totalCount){
    	Log.d(TAG, "createPrepareNotification currentCount:" + currentCount + " totalCount:" + totalCount);
    	Notification.Builder builder = new Notification.Builder(SimHelperService.this);
    	Log.d("^^", "SIMOperationService buidler:" + builder);
    	builder.setOngoing(true)
    	.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(currentCount / totalCount)))
    	.setContentTitle(getString(R.string.prepareImportSimContacts))
    	.setTicker(getString(R.string.prepareImportSimContacts))
    	.setProgress(totalCount , 0, false);
    	return builder.getNotification();
    }
    
    private Notification createProgressNotification(int currentCount, int totalCount){
    	Log.d(TAG, "createProgressNotification  currentCount:" + currentCount + " totalCount:" + totalCount);
    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setOngoing(true)
    	.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(currentCount / totalCount)))
    	.setContentTitle(getString(R.string.doingImportSimContacts))
    	.setTicker(getString(R.string.doingImportSimContacts))
    	.setProgress(totalCount, currentCount, false);
    	return builder.getNotification();
    }
    
    private Notification createFinishNotification(int currentCount, int totalCount){
    	Log.d(TAG, "createFinishNotification currentCount:" + currentCount + " totalCount:" + totalCount);
    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(currentCount / totalCount)))
    	.setContentTitle(getString(R.string.finishImportSimContacts))
    	.setTicker(getString(R.string.finishImportSimContacts));
    	return builder.getNotification();
    }
    
}