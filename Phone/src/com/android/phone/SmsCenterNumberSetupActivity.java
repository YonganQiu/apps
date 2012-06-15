package com.android.phone;

import java.lang.reflect.Field;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.SimpleAdapter.ViewBinder;

import android.os.AsyncResult;
import com.android.internal.telephony.Phone;

/**
 * add by yang.wu at 2012.5.23
 * @author yang.wu
 *
 */
public class SmsCenterNumberSetupActivity extends Activity implements OnClickListener
{

	private final static int GET_SMSCADDRESS = 100;
	private final static int SET_SMSCADDRESS = 101;
	
	private EditText mEditText;
	private Handler mResultHandler;
	private Button mSetBtn;
	private Button mCancelBtn;
	private ProgressBar mprogressBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.sms_center_number_setup);
		
		setTitle(R.string.sms_center_number_label);
		
		mEditText = (EditText)findViewById(R.id.sms_center_number_edittext);
		mCancelBtn = (Button)findViewById(R.id.cancel_btn);
		mSetBtn = (Button)findViewById(R.id.set_btn);
		mprogressBar = (ProgressBar)findViewById(R.id.progress_bar);
		
		mSetBtn.setOnClickListener(this);
		mCancelBtn.setOnClickListener(this);
		
		mResultHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) 
			{
				if(msg.what == GET_SMSCADDRESS)
				{
					if(msg.obj instanceof AsyncResult)
					{
						AsyncResult asyncResult = (AsyncResult)msg.obj;
						String address = (String)asyncResult.result;
						mEditText.setText(address);
					}
					else
					{
						mEditText.setText("");
					}
					mprogressBar.setVisibility(View.GONE);
				}
				else if(msg.what == SET_SMSCADDRESS)
				{
					if(msg.obj instanceof AsyncResult)
					{
						AsyncResult asyncResult = (AsyncResult)msg.obj;
						String result = (String)asyncResult.result;
						Log.i("SmsCenterNumberSetupActivity", "result : " + result);
					}
				}
				
			}
		};
	}

	@Override
	protected void onStart() 
	{
		super.onStart();
		
		Phone phone = PhoneApp.getPhone();
		phone.getSmscAddress(mResultHandler.obtainMessage(GET_SMSCADDRESS));
	}

	@Override
	public void onClick(View v) 
	{
		int id = v.getId();
		if(id == R.id.set_btn)
		{
			String address = mEditText.getText().toString();
			if(!TextUtils.isEmpty(address))
			{
				Phone phone = PhoneApp.getPhone();
				phone.setSmscAddress(address, mResultHandler.obtainMessage(SET_SMSCADDRESS));
			}
			
			finish();
		}
		else if(id == R.id.cancel_btn)
		{
			finish();
		}
	}
}
