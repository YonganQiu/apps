package com.android.contacts.dialpad;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.android.contacts.R;

public class DialerFragment extends Fragment {
	private boolean is_filter_show = false;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View mContentView = inflater.inflate(R.layout.dialer_fragment, container, false);
		return mContentView;
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
	
	//add JiangzhouQ 12.06.18
	private Interpolator decelerator = new DecelerateInterpolator();
	private Interpolator accelerator = new AccelerateInterpolator();
	public void onAnimationFragment(){
		Log.d("^^", "onAnimationFragment.start");
		Fragment fragment = getFragmentManager().findFragmentById(R.id.call_log_fragment);
		ListView list = (ListView)fragment.getView().findViewById(android.R.id.list);
		ObjectAnimator animShow = ObjectAnimator.ofFloat(list, "X", 0f ,200f);
		animShow.setDuration(400);
		animShow.setInterpolator(decelerator);
		ObjectAnimator animHide = ObjectAnimator.ofFloat(list, "X", 200f, 0f);
		animHide.setDuration(400);
		animHide.setInterpolator(accelerator);
		if(!is_filter_show){
			animShow.start();
			is_filter_show = true;
		}else{
			animHide.start();
			is_filter_show = false;
		}
	}
	//end JiangzhouQ 12.06.18

}