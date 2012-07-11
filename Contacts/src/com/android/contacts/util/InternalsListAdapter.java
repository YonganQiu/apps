package com.android.contacts.util;

import com.android.contacts.R;
import java.util.ArrayList;
import java.util.List;

import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.SimAccountType;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * @author yongan.qiu
 *
 */
public class InternalsListAdapter extends BaseAdapter {
	private final LayoutInflater mInflater;
	private final Context mContext;
	private List<AccountWithDataSet> mInternals;
	private AccountTypeManager mAccountTypes;
	
	public enum InternalListFilter {
		ALL_INTERNALS,                   // All read-only and writable accounts
		INTERNALS_CONTACT_WRITABLE,      // Only where the account type is contact writable
		INTERNALS_GROUP_WRITABLE         // Only accounts where the account type is group writable
	}
	
	public InternalsListAdapter(Context context, InternalListFilter internalListFilter) {
		this(context, internalListFilter, null);
	}

	public InternalsListAdapter(Context context, InternalListFilter internalListFilter,
		AccountWithDataSet currentAccount) {
		mContext = context;
		mAccountTypes = AccountTypeManager.getInstance(context);
		mInternals = getInternals(internalListFilter);
		if (currentAccount != null
				&& !mInternals.isEmpty()
				&& !mInternals.get(0).equals(currentAccount)
				&& mInternals.remove(currentAccount)) {
			mInternals.add(0, currentAccount);
		}
		mInflater = LayoutInflater.from(context);
	}
	
	private List<AccountWithDataSet> getInternals(InternalListFilter internalListFilter) {
		if (internalListFilter == InternalListFilter.INTERNALS_GROUP_WRITABLE) {
			return new ArrayList<AccountWithDataSet>(mAccountTypes.getGroupWritableInternals());
		}
		return new ArrayList<AccountWithDataSet>(mAccountTypes.getInternals(
				internalListFilter == InternalListFilter.INTERNALS_CONTACT_WRITABLE));
	}
	
	@Override
	public int getCount() {
		return mInternals.size();
	}

	@Override
	public AccountWithDataSet getItem(int position) {
		return mInternals.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View resultView = convertView != null ? convertView
				: mInflater.inflate(R.layout.internal_selector_list_item, parent, false);

		final TextView text = (TextView) resultView.findViewById(R.id.text);
		final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);

		final AccountWithDataSet local = mInternals.get(position);
		final AccountType accountType = mAccountTypes.getAccountType(local.type, local.dataSet);

		text.setText(accountType.getDisplayLabel(mContext));

		icon.setImageDrawable(accountType.getDisplayIcon(mContext));

		return resultView;

	}

}
