package com.android.contacts.util;

import com.android.contacts.model.AccountWithDataSet;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * 
 * @author yongan.qiu
 *
 */
public final class InternalsAndAccountsMergeAdapter extends BaseAdapter {
	private InternalsListAdapter mLocalsAdapter;
	private AccountsListAdapter mAccountsAdapter;
	
	public InternalsAndAccountsMergeAdapter(InternalsListAdapter internalsAdapter, AccountsListAdapter accountsAdapter) {
		if (internalsAdapter == null) {
			throw new IllegalArgumentException("Locals adapter should not be null.");
		}
		mLocalsAdapter = internalsAdapter;
		mAccountsAdapter = accountsAdapter;
	}
	
	@Override
	public int getCount() {
		if (mAccountsAdapter == null) {
			return mLocalsAdapter.getCount();
		}
		return mLocalsAdapter.getCount() + mAccountsAdapter.getCount();
	}

	@Override
	public AccountWithDataSet getItem(int position) {
		int localsCount = mLocalsAdapter.getCount();
		if (position < localsCount) {
			return mLocalsAdapter.getItem(position);
		} else {
			return mAccountsAdapter.getItem(position - localsCount);
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int localsCount = mLocalsAdapter.getCount();
		if (position < localsCount) {
			return mLocalsAdapter.getView(position, null, parent);
		} else {
			return mAccountsAdapter.getView(position - localsCount, null, parent);
		}
	}

}
