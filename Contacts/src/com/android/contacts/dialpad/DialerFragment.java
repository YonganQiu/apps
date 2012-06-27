package com.android.contacts.dialpad;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
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

import com.android.contacts.R;
import com.android.contacts.calllog.CallLogFragment;
import com.android.contacts.list.CallLogPhoneNumberFragment;

public class DialerFragment extends Fragment {
	
	//{Added by yongan.qiu on 2012.6.21 begin.
	public interface OnFragmentReadyListener {
		void onFragmentReady();
	}
	
	private View mCallTypeDialpadButtons;
	
	OnFragmentReadyListener mOnFragmentReadyListener;
	
	private CallLogFragment mCallLogFragment;
	private CallLogPhoneNumberFragment mCallLogPhoneNumberFragment;
	private DialpadFragment mDialpadFragment;
	//}Added by yongan.qiu end.
	
	private boolean is_filter_show = false;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View contentView = inflater.inflate(R.layout.dialer_fragment, container, false);
		mCallTypeDialpadButtons = contentView.findViewById(R.id.callTypeDialpadButtons);
        contentView.findViewById(R.id.call_log_fragment_touch_area).setOnTouchListener(
                new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        setFragmentShow(R.id.dialpad_fragment, R.animator.fragment_slide_down_enter,
                                R.animator.fragment_slide_down_exit, false);
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
                        setFragmentShow(R.id.dialpad_fragment, R.animator.fragment_slide_down_enter,
                                R.animator.fragment_slide_down_exit, true);
                    }
                });
        
        return contentView;
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
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
		mCallLogPhoneNumberFragment = (CallLogPhoneNumberFragment) getFragmentManager().findFragmentById(R.id.call_log_phone_number_fragment);
		mDialpadFragment = (DialpadFragment) getFragmentManager().findFragmentById(R.id.dialpad_fragment);
		if (mOnFragmentReadyListener != null) {
			mOnFragmentReadyListener.onFragmentReady();
		}
	}
	
	public void setFragmentReadyListener(OnFragmentReadyListener listener) {
		mOnFragmentReadyListener = listener;
	}
	//}Added by yongan.qiu end.

	public boolean isFragmentShow(int id) {
		Fragment fragment = getFragmentManager().findFragmentById(id);
		if(fragment != null) {
			return !fragment.isHidden();
		}
		return false;
	}
	
	public void setFragmentShow(int id, int animEnter, int animExit, boolean isShow) {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentById(id);
		if(fragment != null && !fragment.isHidden() ^ isShow) {
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.setCustomAnimations(animEnter, animExit);
			if(isShow) {
				transaction.show(fragment);
			} else {
				transaction.hide(fragment);
			}
			transaction.commitAllowingStateLoss();
			fragmentManager.executePendingTransactions();
		}
		if(id == R.id.dialpad_fragment) {
            mCallTypeDialpadButtons.setVisibility(isShow ? View.GONE : View.VISIBLE);
        }
	}
	
	//{Added by yongan.qiu on 2012.6.21 begin.
	private static final String TAG = "DialerFragment";
	public void showCallLogPhoneNumberFragment(FragmentTransaction transaction) {
		Log.i(TAG, "showCallLogPhoneNumberFragment().");
		swapFragment(transaction, mCallLogFragment, mCallLogPhoneNumberFragment);
	}
	
	public void showCallLogFragment(FragmentTransaction transaction) {
		Log.i(TAG, "showCallLogFragment().");
		swapFragment(transaction, mCallLogPhoneNumberFragment, mCallLogFragment);
	}
	
	public void swapFragment(FragmentTransaction transaction, Fragment hide, Fragment show) {
		Log.i(TAG, "hide != null && !hide.isHidden() " + (hide != null && !hide.isHidden()));
		if (hide != null && !hide.isHidden()) {
			Log.i(TAG, "hide " + hide);
			transaction.hide(hide);
		}
		Log.i(TAG, "hide != null && !hide.isHidden() " + (hide != null && !hide.isHidden()));
		if (hide != null && !hide.isHidden()) {
			Log.i(TAG, "show " + show);
			transaction.show(show);
		}
		transaction.commit();
	}
	//}Added by yongan.qiu end.
	
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
