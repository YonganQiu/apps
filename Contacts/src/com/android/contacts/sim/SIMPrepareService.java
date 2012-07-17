package com.android.contacts.sim;

import java.util.ArrayList;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.android.contacts.ContactsApplication;
import com.android.contacts.util.Constants;

public class SIMPrepareService extends Service{

	static final ContentValues sEmptyContentValues = new ContentValues();
	private static final String TAG = "SIMPrepareService";
	private static final int QUERY_TOKEN = 0;
//	private SIMQueryHandler mSIMQueryHandler;
	private Cursor mCursor;
    private static final String[] COLUMN_NAMES = new String[] {
        "display_name",
        "data1"
    };
    
    private volatile Looper mLooper;
    private Handler mHandler;
    
	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("SIMPrepareService");
        thread.start();
        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper);
        
//		mSIMQueryHandler = new SIMQueryHandler(getContentResolver());
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mHandler.post(new Runnable() {

			@Override
			public void run() {
				deleteSIMContacts(getContentResolver());
				startQuery();

				Account mAccount = new Account("sim1", "sim");
				mCursor.moveToPosition(-1);
				while (mCursor.moveToNext()) {
					String name = mCursor.getString(0);
					Log.d(TAG, "name:" + name);
					actuallyImportOneSimContact(mCursor, getContentResolver(),
							mAccount);
				}
				((ContactsApplication) getApplication()).SIMPreparing = false;
				sendBroadcast();

			}
		});
	
		
		return START_STICKY;
	}
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	protected Uri getUri() {
		return Uri.parse("content://icc/adn");
	}


    private void startQuery() {
        Uri uri = getUri();
        Log.d(TAG, "start query:" + uri);
//        mSIMQueryHandler.startQuery(QUERY_TOKEN, null, uri, COLUMN_NAMES,
//                null, null, null);
        mCursor = getContentResolver().query( getUri(), COLUMN_NAMES, null, null, null);
        ((ContactsApplication) getApplication()).SIMPreparing = true;
        sendBroadcast();
        
    }
    
//    private class SIMQueryHandler extends AsyncQueryHandler{
//
//		public SIMQueryHandler(ContentResolver cr) {
//			super(cr);
//		}
//    	
//		@Override
//		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
//			mCursor = cursor;
//			ImportAllSimContactsThread importThread = new ImportAllSimContactsThread();
//			importThread.start();
//		}
//    }
    
    private class deleteAllSimContactsThread extends Thread{
		public deleteAllSimContactsThread() {
			super("deleteAllSimContactsThread");
		}

		@Override
		public void run() {
			deleteSIMContacts(getContentResolver());
			startQuery();
		}

	}
    
	private class ImportAllSimContactsThread extends Thread {
		public ImportAllSimContactsThread() {
			super("ImportAllSimContactsThread");
		}

		@Override
		public void run() {
			Account mAccount = new Account("sim1", "sim");
			mCursor.moveToPosition(-1);
			while(mCursor.moveToNext()){
				String name = mCursor.getString(0);
				Log.d(TAG, "name:" + name);
				actuallyImportOneSimContact(mCursor,getContentResolver(),mAccount);
			}
			 ((ContactsApplication) getApplication()).SIMPreparing = false;
			 sendBroadcast();
		}

	}
    private void sendBroadcast(){
    	Intent intent = new Intent();
    	intent.setAction(Constants.SIM_READING_STATE);
    	Log.d("^^", "simprepareservice sendBroadcast");
    	sendBroadcast(intent);
    }
    
    private static void actuallyImportOneSimContact(
            final Cursor cursor, final ContentResolver resolver, Account account) {
        final NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(cursor.getString(0));
        final String name = namePhoneTypePair.name;
        final String phoneNumber = cursor.getString(1);
        
       ContentValues values = new ContentValues();
       if(account != null){
		values.put(RawContacts.ACCOUNT_NAME, account.name);
		values.put(RawContacts.ACCOUNT_TYPE, account.type
				);
       }
		Uri rawContactUri = resolver.insert(
				RawContacts.CONTENT_URI, values);
		long rawContactId = ContentUris.parseId(rawContactUri);

		if (!name.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			values.put(StructuredName.GIVEN_NAME, name);
			resolver.insert(ContactsContract.Data.CONTENT_URI,
					values);
		}

		if (!phoneNumber.isEmpty()) {
			values.clear();
			values.put(Data.RAW_CONTACT_ID, rawContactId);
			values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			values.put(Phone.NUMBER, phoneNumber);
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			resolver.insert(ContactsContract.Data.CONTENT_URI,
					values);
		}
		
    }
    
    private void deleteSIMContacts(final ContentResolver resolver){
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
}
