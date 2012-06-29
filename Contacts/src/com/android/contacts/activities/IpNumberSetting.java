
package com.android.contacts.activities;

import com.android.contacts.R;
import com.android.contacts.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class IpNumberSetting extends Activity {
    private static final int DIALOG_ADD_IP_NUMBER = 1;
    private static final int DIALOG_DELETE_IP_NUMBER = 2;
    private static final String KEY_IP_NUMBER = "ip_number";

    private String mIpNumber;
    private ListView mListView;
    private IpNumberAdapter mIpNumberAdapter;
    private SharedPreferences mSharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ip_number_setting);

        findViewById(R.id.addIpNumber).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_ADD_IP_NUMBER);
            }
        });

        mSharedPrefs = getSharedPreferences(Constants.IP_NUMBER_SETTING_SHARED_PREFS_NAME,
                Context.MODE_WORLD_WRITEABLE);
        mIpNumber = mSharedPrefs.getString(KEY_IP_NUMBER, null);

        mIpNumberAdapter = new IpNumberAdapter();
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    mIpNumber = mIpNumberAdapter.getIpNumberDataList().get(position).mNumber;
                } else {
                    mIpNumber = null;
                }
                RadioButton radioButton;
                int childCount = mListView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    radioButton = (RadioButton) mListView.getChildAt(i).findViewById(
                            R.id.radioButton);
                    radioButton.setChecked(false);
                }
                radioButton = (RadioButton) view.findViewById(R.id.radioButton);
                radioButton.setChecked(true);
            }
        });
        mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    Bundle args = new Bundle();
                    args.putString(KEY_IP_NUMBER,
                            mIpNumberAdapter.getIpNumberDataList().get(position).mNumber);
                    showDialog(DIALOG_DELETE_IP_NUMBER, args);
                }
                return true;
            }
        });
        mListView.setAdapter(mIpNumberAdapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        if (mIpNumber != null) {
            editor.putString(KEY_IP_NUMBER, mIpNumber);
        } else {
            editor.remove(KEY_IP_NUMBER);
        }
        editor.commit();
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_ADD_IP_NUMBER:
                return createAddIpNumberDialog();
            case DIALOG_DELETE_IP_NUMBER:
                return createDeleteIpNumberDialog(args.getString(KEY_IP_NUMBER));
        }
        return null;
    }

    private Dialog createAddIpNumberDialog() {
        final View view = getLayoutInflater().inflate(R.layout.add_ip_number, null);
        final EditText editText = (EditText) view
                .findViewById(R.id.ipNumber);
        editText.setText(null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(this, android.R.style.Theme_Holo));
        builder.setTitle(R.string.addIpNumber);
        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissDialog(DIALOG_ADD_IP_NUMBER);
                        editText.setText(null);
                    }
                });

        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String ipNumber = editText.getText().toString();
                        if (mSharedPrefs.contains(ipNumber)) {
                            Toast.makeText(IpNumberSetting.this,
                                    getString(R.string.repeatIpNumber, ipNumber),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            SharedPreferences.Editor editor = mSharedPrefs.edit();
                            String time = String.valueOf(System.currentTimeMillis());
                            editor.putString(ipNumber, time);
                            editor.commit();
                            mIpNumberAdapter.getIpNumberDataList().add(
                                    new IpNumberData(time, ipNumber));
                            mIpNumberAdapter.notifyDataSetChanged();
                        }
                        editText.setText(null);
                    }
                });

        return builder.create();
    }

    private Dialog createDeleteIpNumberDialog(final String ipNumber) {
        final View view = getLayoutInflater().inflate(R.layout.delete_ip_number, null);
        ((TextView) view.findViewById(R.id.ipNumber)).setText(getString(R.string.deleteIpNumberMsg,
                ipNumber));

        final AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(this, android.R.style.Theme_Holo));
        builder.setTitle(R.string.deleteIpNumber);
        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissDialog(DIALOG_DELETE_IP_NUMBER);
                    }
                });

        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (ipNumber != null) {
                            SharedPreferences.Editor editor = mSharedPrefs.edit();
                            editor.remove(ipNumber);
                            editor.commit();
                            ArrayList<IpNumberData> list = mIpNumberAdapter.getIpNumberDataList();
                            int size = list.size();
                            for (int i = 0; i < size; i++) {
                                if (list.get(i).mNumber.equals(ipNumber)) {
                                    list.remove(i);
                                    break;
                                }
                            }
                            if (ipNumber.equals(mIpNumber)) {
                                mIpNumber = null;
                            }
                            mIpNumberAdapter.notifyDataSetChanged();
                        }
                    }
                });

        return builder.create();
    }
    
    public static String getIpNumber(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.IP_NUMBER_SETTING_SHARED_PREFS_NAME,
                Context.MODE_WORLD_WRITEABLE);
        return sharedPrefs.getString(KEY_IP_NUMBER, null);
    }

    private class IpNumberData {
        public final String mTime;
        public final String mNumber;

        public IpNumberData(String time, String number) {
            mTime = time;
            mNumber = number;
        }
    }

    private class SortCallData implements Comparator<IpNumberData> {

        public int compare(IpNumberData data1, IpNumberData data2) {
            int result = 0;
            if (data1 != null && data2 != null) {
                result = data1.mTime.compareTo(data2.mTime);
            }
            return result;
        }
    }

    private class IpNumberAdapter extends BaseAdapter {
        private ArrayList<IpNumberData> mIpNumberDataList = new ArrayList<IpNumberData>();

        private IpNumberAdapter() {
            mIpNumberDataList.add(new IpNumberData("0", getString(R.string.disableIpNumber)));
            Map<String, ?> map = mSharedPrefs.getAll();
            if (!map.isEmpty()) {
                String key;
                Iterator<String> iterator = map.keySet().iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    if (!KEY_IP_NUMBER.equals(key)) {
                        mIpNumberDataList.add(new IpNumberData((String) map.get(key), key));
                    }
                }
                Collections.sort(mIpNumberDataList, new SortCallData());
            }
        }

        @Override
        public int getCount() {
            return mIpNumberDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return mIpNumberDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.ip_number_list_item, null);
            }
            String number = mIpNumberDataList.get(position).mNumber;
            ((RadioButton) view.findViewById(R.id.radioButton))
                    .setChecked(number.equals(mIpNumber) || (position == 0 && mIpNumber == null));
            ((TextView) view.findViewById(R.id.number))
                    .setText(number);
            return view;
        }

        public ArrayList<IpNumberData> getIpNumberDataList() {
            return mIpNumberDataList;
        }
    }
}
