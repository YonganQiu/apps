package com.android.phone;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SimpleCursorAdapterInSimContacts extends SimpleCursorAdapter{

	protected static final int NAME_COLUMN = 0;
	protected static final int NUMBER_COLUMN = 1;
	    
	private int[] mTo = new int[]{};
	public SimpleCursorAdapterInSimContacts(Context context, int layout,
			Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
		mTo = to;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		super.bindView(view, context, cursor);
		String name = cursor.getString(cursor.getColumnIndex("display_name"));
		if(name.isEmpty()){
			TextView v = (TextView) view.findViewById(mTo[0]);
			v.setText(cursor.getString(cursor.getColumnIndex("data1")));
		}
		
	}
}
