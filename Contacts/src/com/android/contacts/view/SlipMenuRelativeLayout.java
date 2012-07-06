package com.android.contacts.view;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.FragmentManager;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;
import android.widget.Scroller;

/**
 * @author gangzhou.qi
 *  2012.07
 */
public class SlipMenuRelativeLayout extends RelativeLayout{

	private VelocityTracker mVelocityTracker;
	private float mDownX;
    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;
    private boolean mIsPullOut = false;
    private int mTouchState = TOUCH_STATE_REST;
    private int TOUCH_SLOP;
    private float mLastMotionX;
    private float mTouchX = 0;
    private float[] mLimitedDistance = new float[]{0,0};
    private Interpolator accelerator = new AccelerateInterpolator();
    private ObjectAnimator mObjectAnimator; 
    ValueAnimator anim;
    private int mLeftTrigger = 0;
    private List<ParamsPosition> mParamsList = new LinkedList<ParamsPosition>();
    
	public SlipMenuRelativeLayout(Context context) {
		super(context);
		init();
	}
	public SlipMenuRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	public SlipMenuRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init(){
		TOUCH_SLOP = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		setLimitedDIstance(300, 0);
		setLeftTriggerDistance(200);
        
	}
	
	public void setLimitedDIstance(float x , float y){
		mLimitedDistance[0] = x;
		mLimitedDistance[1] = y;
	}
	
	public void setLeftTriggerDistance(int distance){
		mLeftTrigger = distance;
	}
	
	public int getLeftTriggerDistance(){
		return mLeftTrigger;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {


		Log.d("^^", "-----SlipMenuRelativeLayout onInterceptTouchEvent : received the enent!");
        final int actionMasked = event.getActionMasked();
        final float x = event.getX();
        final float y = event.getY();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
//              mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
            	  mDownX = x;
                break;
                
            case MotionEvent.ACTION_MOVE:
            	if (mTouchState != TOUCH_STATE_SCROLLING) {
					float moveDistanceX = x - mDownX;
					if (mTouchState == TOUCH_STATE_REST ) {
						if (Math.abs(moveDistanceX) > TOUCH_SLOP && mDownX < 200 && mIsPullOut == false) {
							mTouchState = TOUCH_STATE_SCROLLING;
							mLastMotionX = x;
							mTouchX = getChildAt(1).getX();
						}else if (Math.abs(moveDistanceX) > TOUCH_SLOP && mIsPullOut == true && mDownX > mLimitedDistance[0]){
							mTouchState = TOUCH_STATE_SCROLLING;
							mLastMotionX = x;
							mTouchX = getChildAt(1).getX();
						}
					}
				}
        }
        
        boolean returnValue = mTouchState == TOUCH_STATE_SCROLLING;
        return returnValue;
    
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
        final int actionMasked = event.getActionMasked();
        final float x = event.getX();
        final float y = event.getY();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                break;
            
                
            case MotionEvent.ACTION_MOVE:
							if(mTouchState == TOUCH_STATE_SCROLLING){
		                    float deltaX = mLastMotionX - x;
		                    mLastMotionX = x;
		                    if (deltaX < 0) {
		                        float rightmost = mLimitedDistance[0];
		                        if (mTouchX > rightmost) {
		                        	float a = mTouchX-rightmost;
		                        	float b = (float) (getWidth() * 0.15 * Math.sin((deltaX / getWidth()) * (Math.PI / 2)));
		                        	Log.d("^^", "mTouchX > rightmost " + "a :" + a + " b :" + b);
		                            mTouchX -= Math.max(rightmost -mTouchX,  (float) (getWidth() * 0.3 * Math
		        							.sin((deltaX / getWidth()) * (Math.PI / 2))));
		                            getChildAt(1).layout((int)mTouchX, 0, (int)mTouchX + getWidth(), getHeight());
		                        }else{
		                        	    mTouchX -= deltaX;
		                        	    getChildAt(1).layout((int)mTouchX, 0, (int)mTouchX + getWidth(), getHeight());
		                        	    
		                        }
		                    } else if (deltaX > 0) {
		
		                    	if(mTouchX > 0){
		                    		mTouchX -= deltaX;
		                    		 getChildAt(1).layout((int)mTouchX, 0, (int)mTouchX + getWidth(), getHeight());
		                    	}
		                    	
		                    }
							}
                break;
                
            case MotionEvent.ACTION_UP:

            	mTouchState = TOUCH_STATE_REST;
            	
            	if(anim != null){
            		anim.removeAllListeners();
            	}
            	float currentX = getChildAt(1).getX();
            	if(Math.abs(currentX - 0) > Math.abs(currentX - mLimitedDistance[0])){
            		startAnimatorPullOut();
            	}else{
            		startAnimatorPushIn();
            	}
            	
            	break;
        }
		return true;
	}

	public boolean getPulledState(){
		return mIsPullOut;
	}

	//start the animator to pull out the paper.
	//it can be called when you want to pull the paper by the way you want.
	public void startAnimatorPullOut(){
		anim = ValueAnimator.ofInt((int)getChildAt(1).getX(), (int)mLimitedDistance[0]);
		anim.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				getChildAt(1).layout((Integer)animation.getAnimatedValue(), 0, (Integer)animation.getAnimatedValue() + getWidth(), getHeight());
				
				mParamsList.get(1).mLeft = (int)mLimitedDistance[0];
				mParamsList.get(1).mRight = (int)mLimitedDistance[0] + getChildAt(1).getWidth();
			}
		});
		anim.setDuration(300);
		anim.start();
		mIsPullOut = true;
	}

	// start the animator to push in the paper.
	// it can be called when you want to push the paper by the way you want.
	public void startAnimatorPushIn(){
		anim = ValueAnimator.ofInt((int)getChildAt(1).getX(), 0);
		anim.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				getChildAt(1).layout((Integer)animation.getAnimatedValue(), 0, (Integer)animation.getAnimatedValue() + getWidth(), getHeight());
				mParamsList.get(1).mLeft = 0;
				mParamsList.get(1).mRight = getChildAt(1).getWidth();
			}
		});
		anim.setDuration(300);
		anim.start();
		mIsPullOut = false;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(mParamsList.size() == 0){
			super.onLayout(changed, l, t, r, b);
			int count = getChildCount();
	        for (int i = 0; i < count; i++) {
	        	 
	            View child = getChildAt(i);
	            	ParamsPosition pp = new ParamsPosition();
	            	pp.mLeft = (int) child.getX();
	            	pp.mTop = (int) child.getY();
	            	pp.mRight = child.getRight();
	            	pp.mBottom = child.getBottom();
	            	mParamsList.add(pp);
	            	Log.d("^^", "add list i:" + i + "pp:" + pp.mLeft + " " + pp.mTop + " " + pp.mRight+ " " + pp.mBottom);
	        }
		}else{
		int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
                ParamsPosition pp = mParamsList.get(i);
                child.layout(pp.mLeft,pp.mTop,pp.mRight,pp.mBottom);
        }
		}
	}
	
	//write down the position before onLayout()
	//otherwise you will lose them and the position of the childs will be reset.
	protected class ParamsPosition{
		public int mLeft;
		public int mTop;
		public int mRight;
		public int mBottom;
	}
	
}
