package com.android.contacts.dialpad;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.android.contacts.R;
import com.android.contacts.activities.ViewPagerVisibilityListener;
import com.android.contacts.calllog.CallLogFragment;
import com.android.contacts.list.FilteredResultsFragment;
import com.android.contacts.util.Constants;
import com.android.contacts.view.SlipMenuRelativeLayout;

public class DialerFragment extends Fragment{
	
	SlipMenuRelativeLayout slipMenuRelativeLayout;
	
	//{Added by yongan.qiu on 2012.6.21 begin.
	public interface OnFragmentReadyListener {
		void onFragmentReady();
	}
	
//	private View mCallTypeDialpadButtons;
	
	OnFragmentReadyListener mOnFragmentReadyListener;
	
	private CallLogFragment mCallLogFragment;
	private FilteredResultsFragment mFilteredResultsFragment;
	private DialpadFragment mDialpadFragment;
	//}Added by yongan.qiu end.

	//for post runnable;
	private Handler mHandler = new Handler(){
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View contentView = inflater.inflate(R.layout.dialer_fragment, container, false);
				RadioGroup radioGroup = (RadioGroup) contentView.findViewById(R.id.radio_group);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkCalllogPullOut()) {
					slipMenuRelativeLayout.startAnimatorPushIn();
				}
				final CallLogFragment callFragment = (CallLogFragment) getCalllogFragment();
				if(callFragment != null){
					switch(checkedId){
					case R.id.type_all:
						mHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
								callFragment.fetchCallsForDType(Constants.CALL_TYPE_ALL);
							}
						}, 500);
						Log.d("^^", "1 pressed!");
						break;
					case R.id.type_incoming:
						mHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
								callFragment.fetchCallsForDType(Constants.CALL_TYPE_IN);
							}
						}, 500);
						Log.d("^^", "2 pressed!");
						break;
					case R.id.type_outgoing:
						mHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
								callFragment.fetchCallsForDType(Constants.CALL_TYPE_OUT);
							}
						}, 500);
						Log.d("^^", "3 pressed!");
						break;
					case R.id.type_missed:
						mHandler.postDelayed(new Runnable(){
							@Override
							public void run() {
								callFragment.fetchCallsForDType(Constants.CALL_TYPE_MISSED);
							}
						}, 500);
						Log.d("^^", "4 pressed!");
						break;
				}
				}
				
			}
		});
//		mCallTypeDialpadButtons = contentView.findViewById(R.id.callTypeDialpadButtons);
        contentView.findViewById(R.id.call_log_fragment_touch_area).setOnTouchListener(
                new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                    	if(mDialpadFragment != null && mDialpadFragment.isVisible()){
                        setFragmentShow(R.id.dialpad_fragment, R.animator.fragment_slide_down_enter, R.animator.fragment_slide_down_exit, false);
                    	}
                    	return false;
                    }
                });

        contentView.findViewById(R.id.callTypeButton).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onAnimationFragment();
                    }
                });

        contentView.findViewById(R.id.dialpadButton).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    	if (checkCalllogPullOut()) {
        					slipMenuRelativeLayout.startAnimatorPushIn();
        					mHandler.postDelayed(new Runnable(){
    							@Override
    							public void run() {
    								setFragmentShow(R.id.dialpad_fragment, R.animator.fragment_slide_down_enter,
    		                                R.animator.fragment_slide_down_exit, true);
    							}
                        	}, 500);
        				}else{
        					setFragmentShow(R.id.dialpad_fragment, R.animator.fragment_slide_down_enter,
	                                R.animator.fragment_slide_down_exit, true);
        				}
                    }
                });
        
        return contentView;
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        slipMenuRelativeLayout = (SlipMenuRelativeLayout) getActivity().findViewById(R.id.slip_menu_layout);
        FragmentManager frgMgr = getFragmentManager();
        mCallLogFragment = (CallLogFragment)frgMgr.findFragmentById(R.id.call_log_fragment);
        mDialpadFragment = (DialpadFragment)frgMgr.findFragmentById(R.id.dialpad_fragment);
        
        setFragmentShow(R.id.dialpad_fragment, R.animator.fragment_slide_down_enter,
                R.animator.fragment_slide_down_exit, true);
        
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tag", "DialerFragment");
	}
	
	//{Added by yongan.qiu on 2012.6.21 begin.
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mCallLogFragment = (CallLogFragment) getFragmentManager().findFragmentById(R.id.call_log_fragment);
		mFilteredResultsFragment = (FilteredResultsFragment) getFragmentManager().findFragmentById(R.id.filtered_results_fragment);
		mDialpadFragment = (DialpadFragment) getFragmentManager().findFragmentById(R.id.dialpad_fragment);
		if (mOnFragmentReadyListener != null) {
			mOnFragmentReadyListener.onFragmentReady();
		}
	}
	
	public void setFragmentReadyListener(OnFragmentReadyListener listener) {
		mOnFragmentReadyListener = listener;
	}
	//}Added by yongan.qiu end.

	public boolean checkCalllogPullOut(){
		
		if(slipMenuRelativeLayout == null){
			slipMenuRelativeLayout = (SlipMenuRelativeLayout) getActivity().findViewById(R.id.slip_menu_layout);
		}else{
			return slipMenuRelativeLayout.getPulledState();
		}
		return false;
	}
	
	public int getLeftDragger(){
		if(slipMenuRelativeLayout == null){
			slipMenuRelativeLayout = (SlipMenuRelativeLayout) getActivity().findViewById(R.id.slip_menu_layout);
		}else{
			return slipMenuRelativeLayout.getLeftTriggerDistance();
		}
		return 0;
	}
	
	public boolean isFragmentShow(int id) {
		Fragment fragment = getFragmentManager().findFragmentById(id);
		if(fragment != null) {
			return !fragment.isHidden();
		}
		return false;
	}
	
	private Fragment getCalllogFragment(){
		Fragment fragment = getFragmentManager().findFragmentById(R.id.call_log_fragment);
			return fragment;
	}
	public void setFragmentShow(int id, int animEnter, int animExit, boolean isShow) {
		Log.d("^^", "setFragmentShow:" + isShow);
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentById(id);
		if(fragment != null && !fragment.isHidden() ^ isShow) {
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.setCustomAnimations(animEnter, animExit);
			if(isShow) {
				transaction.show(fragment);
				slipMenuRelativeLayout.isDialerPadShow = true;
				Log.d("^^", "isDialerPadShow:true");
			} else {
				transaction.hide(fragment);
				slipMenuRelativeLayout.isDialerPadShow = false;
				Log.d("^^", "isDialerPadShow : false");
			}
			transaction.commitAllowingStateLoss();
			fragmentManager.executePendingTransactions();
		}
	}
	
	//{Added by yongan.qiu on 2012.6.21 begin.
	private static final String TAG = "DialerFragment";
	public void showFilteredResultsFragment(FragmentTransaction transaction) {
		swapFragment(transaction, mCallLogFragment, mFilteredResultsFragment);
	}
	
	public void showCallLogFragment(FragmentTransaction transaction) {
		swapFragment(transaction, mFilteredResultsFragment, mCallLogFragment);
	}
	
	public void swapFragment(FragmentTransaction transaction, Fragment hide, Fragment show) {
		if (hide != null && !hide.isHidden()) {
			transaction.hide(hide);
		}
		if (show != null && show.isHidden()) {
			transaction.show(show);
		}
		transaction.commit();
	}
	//}Added by yongan.qiu end.
	
	//add JiangzhouQ 12.06.18
	public void onAnimationFragment(){
		if(!checkCalllogPullOut()){
			slipMenuRelativeLayout.startAnimatorPullOut();
		}else{
			slipMenuRelativeLayout.startAnimatorPushIn();
		}
	}
	//end JiangzhouQ 12.06.18
}
