package com.android.contacts.dialpad;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.contacts.R;

public class DialerFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialer_fragment, container, false);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tag", "DialerFragment");
	}
	
	public boolean isFragmentVisible(int id) {
		Fragment fragment = getFragmentManager().findFragmentById(id);
		if(fragment != null) {
			return fragment.isVisible();
		}
		return false;
	}
	
	public void setFragmentVisible(int id, int animEnter, int animExit, boolean visible) {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentById(id);
		if(fragment != null && fragment.isVisible() ^ visible) {
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.setCustomAnimations(animEnter, animExit);
			if(visible) {
				transaction.show(fragment);
			} else {
				transaction.hide(fragment);
			}
			transaction.commitAllowingStateLoss();
			fragmentManager.executePendingTransactions();
		}
	}
}