package com.android.contacts.activities;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import com.android.contacts.R;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Switch;
import android.widget.Toast;

import android.os.ServiceManager;
import com.android.internal.telephony.AdnRecord;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.AdnRecordCache;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;

public class ExportSIMContacts extends Activity implements OnItemClickListener {
    /** Called when the activity is first created. */
	private static final String TAG = "ExportSIMContacts";
	ListView listview ;
	ActionBar actionBar;
	Cursor mCursor ;
	
	private int QUERY_TOKEN = 1;
//	private ProgressDialog mProgressDialog;
	private Set<String> mIndexSet = new HashSet<String>();
	private final static int TOAST_EXPORT = 1;
	private ExportAsyncTask mExportAsyncTask;
	private boolean ResumeExport = true;
	private int successCount=0;
	private int failedCount=0;
	private boolean isSIMReady = false;
	private IIccPhoneBook simPhoneBook = null;
	private int[] index =null;
	private int indexLimited = 0;
	private boolean isExporting = false;
	private List<String[]> exportQueue = new ArrayList<String[]>(); 
	Uri uri = Phone.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT)).build();
	private int mCount = 0;
	private NotificationManager mNotificationManager;
	private ContentResolver mContentResolver = null;
	private Uri adnUri = Uri.parse("content://icc/adn");
	private int mNotifyCycleCount = 5;
	
	ContentValues values = new ContentValues();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sim_export_activity);
        isSIMReady = false;
        actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
		
        setTitle(R.string.exportSimContacts);
        String[] gridFromColumns = new String[] { ContactsContract.Data.DISPLAY_NAME , ContactsContract.Data.DATA1};
		int[] gridToLayoutIDs = new int[] { R.id.phoneName , R.id.phoneNumber};
		mCursor = getContentResolver().query(uri, null, null, null, ContactsContract.Data.DISPLAY_NAME);
        final SimpleCursorAdapter sAdapter = new SimpleCursorAdapter(this, R.layout.sim_export_activity_item, mCursor, gridFromColumns, gridToLayoutIDs);
        listview = (ListView) findViewById(R.id.listview);
        listview.setAdapter(sAdapter);
        listview.setOnItemClickListener(this);
		 checkSIMState();
		 mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		 mContentResolver = getContentResolver();
    }
    
    private class CreateIndexAsyncTask extends AsyncTask<Integer, Integer, Integer>{
    	Uri sUri = Uri.parse("content://icc/adn");
		@Override
		protected Integer doInBackground(Integer... params) {
			Cursor cursor = getContentResolver().query(sUri, null, null, null, null);
			cursor.moveToPosition(-1);
			String id;
			String name;
			mIndexSet.clear();
			while (cursor.moveToNext()) {
				id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data._ID));
				mIndexSet.add(id);
			}
			cursor.close();
			isSIMReady = true;
			return null;
		}
		
    }
    
    
    private String formateNumber (String str){
    	String str1;
    	String str2;
    	StringBuilder sb = new StringBuilder();
    	str1 = str;
    	char c;
    	for(int i = 0 ; i < str1.length() ; i ++){
    		c = str1.charAt(i);
    		if(c >= '0' && c <= '9'){
    			sb.append(c);
    		}
    	}
    	return sb.toString();
    	
    }
    
    private Notification createPrepareNotification(){
    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setOngoing(true)
    	.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(mCount * 100 / mCursor.getCount())))
    	.setContentTitle(getString(R.string.prepareExportSimContacts))
    	.setTicker(getString(R.string.prepareExportSimContacts))
    	.setProgress(mCursor.getCount(), 0, false);
    	return builder.getNotification();
    }
    
    private Notification createProgressNotification(){
    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setOngoing(true)
    	.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(mCount * 100 / mCursor.getCount())))
    	.setContentTitle(getString(R.string.doingExportSimContacts))
    	.setTicker(getString(R.string.doingExportSimContacts))
    	.setProgress(mCursor.getCount(), mCount, false);
    	return builder.getNotification();
    }
    
    private Notification createFinishNotification(){
    	Notification.Builder builder = new Notification.Builder(this);
    	builder.setSmallIcon(android.R.drawable.stat_notify_sdcard)
    	.setContentText(getString(R.string.percentage,
                String.valueOf(mCount * 100 / mCursor.getCount())))
    	.setContentTitle(getString(R.string.finishExportSimContacts))
    	.setTicker(getString(R.string.finishExportSimContacts));
    	return builder.getNotification();
    }
    
    private class ExportAsyncTask extends AsyncTask<Integer, Integer, Integer>{

    	String name;
    	String number;
		@Override
		protected Integer doInBackground(Integer... params) {
			successCount = 0;
			failedCount = 0;
			mNotificationManager.notify(0, createPrepareNotification());
			for(int i = 0; i < mCursor.getCount(); i++){
				mCursor.moveToPosition(i);
				name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
				number = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Data.DATA1));
				if(!number.isEmpty()){
					number = formateNumber(number);
				}
				if (ResumeExport) {
					try {
						exportOneContact(name, number);
						
						
					} catch (Exception e) {
					}
				publishProgress(i+1);
				}else{
					return null;
				}
				mCount ++;
				if((mCount % mNotifyCycleCount) == 0){
					mNotificationManager.notify(0, createProgressNotification());
				}
			}
			
			mNotificationManager.notify(0, createFinishNotification());
			return null;
		}
    }
    
    private void checkSIMState(){
		TelephonyManager telephonyManager = new TelephonyManager(ExportSIMContacts.this);
		int simState = telephonyManager.getSimState();
		Uri uri = Uri.parse("content://icc/adn");
		if (simState == 5) {
			CreateIndexAsyncTask task = new CreateIndexAsyncTask();
			task.execute(1);
		} else {
			AlertDialog dlg = new AlertDialog.Builder(this)
					.setTitle(getResources().getString(R.string.sim_wrong))
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(
							getResources().getString(R.string.sim_state_wrong))
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									ExportSIMContacts.this.finish();
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									ExportSIMContacts.this.finish();
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
    }
    private Handler nHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch(msg.what)
    		{
    		case TOAST_EXPORT:{
    			Toast.makeText(getApplication(), successCount + getString(R.string.export_success) +failedCount + getString(R.string.export_failed), Toast.LENGTH_SHORT).show();
    			break;
    		}
    		}
    	
    	}
    };
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
    	case R.id.all_export:{
    		ResumeExport = true;
    		if(!isSIMReady){
    			showDialog2();
    			ResumeExport = false;
    			break;
    		}
    		
    		mExportAsyncTask = new ExportAsyncTask();
    		mExportAsyncTask.execute(1);
    		
    		break;
    	}
    	}
    	return super.onOptionsItemSelected(item);
    }
    
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		
		if(!isSIMReady){
			showDialog2();
			ResumeExport = false;
			return;
		} else {

			
			mCursor.moveToPosition(arg2);
			final String name = mCursor.getString(mCursor
					.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
			String number = mCursor.getString(mCursor
					.getColumnIndex(ContactsContract.Data.DATA1));
			final String formatedNumber = formateNumber(number);
			
			successCount = 0;
			failedCount = 0;
			String[] str = new String[]{ name , formatedNumber};
			if(isExporting){
				exportQueue.add(str);
			}else{
				exportQueue.add(str);
				new Thread() {
					public void run() {
							doExportOneByOne();
						}}.start();
			}
		}
	}
	
	private void doExportOneByOne(){
		isExporting = true;
		try {
			exportOneContact(exportQueue.get(0)[0], exportQueue.get(0)[1]);
		} catch (Exception e) {
		}
		
		Message msg = new Message();
		msg.what = TOAST_EXPORT;
		nHandler.sendMessage(msg);
		exportQueue.remove(0);
		if(!exportQueue.isEmpty()){
			doExportOneByOne();
		}else{
			isExporting = false;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.export_sim_contacts, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	private boolean exportOneContact(String str1, String str2){
		values.put("tag", str1);
		values.put("number", str2);
		Uri newSimContactUri = mContentResolver.insert(adnUri, values);
		Log.d(TAG, "exportOneContact" + " tag:" + str1 + " number:" + str2 + " success uri:" + newSimContactUri);
		if(newSimContactUri != null){
			successCount ++;
			return true;
		}else{
			failedCount ++;
			return false;
		}
	}
	
//	private boolean exportOneContact(String str1, String str2)
//			throws RemoteException {
//		String pin2 = null;
//		int currentIndex = 0;
//		if(simPhoneBook == null ||  index == null || indexLimited == 0){
//		   simPhoneBook = IIccPhoneBook.Stub.asInterface(ServiceManager.getService("simphonebook"));
//		   index = simPhoneBook.getAdnRecordsSize(IccConstants.EF_ADN);
//		   indexLimited = index[2];
//		}
//		
//		for (int i = 1; i <= indexLimited; i++) {
//			if (!mIndexSet.contains(i + "")) {
//				currentIndex = i;
//				mIndexSet.add(currentIndex + "");
//				break;
//			}
//		}
//		if (currentIndex == 0 || currentIndex > indexLimited) {
//			Runnable exportRunnable = new Runnable() {
//				@Override
//				public void run() {
//					showDialog();
//					
//				}
//			};
//			nHandler.post(exportRunnable);
//			ResumeExport = false;
//		}else{
//			AdnRecord firstAdn = new AdnRecord(str1, str2);
//			boolean success = false;
//			try {
//				success = simPhoneBook.updateAdnRecordsInEfByIndex(
//						IccConstants.EF_ADN, firstAdn.getAlphaTag(),
//						firstAdn.getNumber(), currentIndex, pin2);
//				
//			} catch (RemoteException e) {
//				Log.e("^^", e.toString(), e);
//			}
//			if(success){
//				successCount ++;
//			}else{
//				failedCount ++;
//			}
//			if(!ResumeExport){
//				Message msg = new Message();
//				msg.what = TOAST_EXPORT;
//				nHandler.sendMessage(msg);
//			}
//			
//			return success;
//		}
//		  
//		return false;
//	}
	
	
	public void showDialog() {
		
		AlertDialog dlg = new AlertDialog.Builder(this)
				.setTitle(getResources().getString(R.string.sim_wrong))
				.setMessage(getResources().getString(R.string.sim_full))
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
	
	public void showDialog2() {
		
		AlertDialog dlg = new AlertDialog.Builder(this)
				.setTitle(getResources().getString(R.string.sim_wrong))
				.setMessage(getResources().getString(R.string.sim_state_wrong))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								checkSIMState();
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								checkSIMState();
							}
						}).create();
		dlg.show();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
