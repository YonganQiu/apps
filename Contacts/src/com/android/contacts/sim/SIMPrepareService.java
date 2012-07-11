package com.android.contacts.sim;

import java.util.ArrayList;

import com.android.contacts.editor.AggregationSuggestionEngine.RawContact;

import android.R.string;
import android.accounts.Account;
import android.app.Service;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Contacts.SettingsColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

public class SIMPrepareService extends Service{

	static final ContentValues sEmptyContentValues = new ContentValues();
	private static final String TAG = "SIMPrepareService";
	private static final int QUERY_TOKEN = 0;
	private SIMQueryHandler mSIMQueryHandler;
    private static final String[] COLUMN_NAMES = new String[] {
        "display_name",
        "data1"
    };
    
	@Override
	public void onCreate() {
		super.onCreate();
		mSIMQueryHandler = new SIMQueryHandler(getContentResolver());
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		deleteSIMContacts(getContentResolver());
		startQuery();
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
        mSIMQueryHandler.startQuery(QUERY_TOKEN, null, uri, COLUMN_NAMES,
                null, null, null);
    }
    
    private class SIMQueryHandler extends AsyncQueryHandler{

		public SIMQueryHandler(ContentResolver cr) {
			super(cr);
		}
    	
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			Account mAccount = new Account("sim1", "sim");
			while(cursor.moveToNext()){
				String name = cursor.getString(0);
				Log.d(TAG, "name:" + name);
				actuallyImportOneSimContact(cursor,getContentResolver(),mAccount);
			}
		}
    }
    
    private static void actuallyImportOneSimContact(
            final Cursor cursor, final ContentResolver resolver, Account account) {
        final NamePhoneTypePair namePhoneTypePair =
            new NamePhoneTypePair(cursor.getString(0));
        final String name = namePhoneTypePair.name;
        final int phoneType = namePhoneTypePair.phoneType;
        final String phoneNumber = cursor.getString(1);
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

//        builder = ContentProviderOperation.newDelete(Data.CONTENT_URI);
//        builder.withSelection(RawContacts.ACCOUNT_TYPE + "==?",new String[]{"sim"});
//        operationList.add(builder.build());
        
        
        
        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
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
