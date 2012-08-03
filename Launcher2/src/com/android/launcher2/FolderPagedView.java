package com.android.launcher2;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

public class FolderPagedView extends ViewGroup {

	int mMaxCountX = 9;
	int mMaxCountY = 9;

	private static final int INVALID_SCREEN = -1;
	private static final int SNAP_VELOCITY = 600;
	private int mCurrWidth = -1;
	private int mCurrHeight = -1;

	private int mCurrentScreen;
	private int mNextScreen = INVALID_SCREEN;
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;

	private float mLastMotionX;
	private final static int TOUCH_STATE_REST = 0;
	private final static int TOUCH_STATE_SCROLLING = 1;

	private int mTouchState = TOUCH_STATE_REST;

	private int mTouchSlop;
	private int mMaximumVelocity;

	private static final float NANOTIME_DIV = 1000000000.0f;
	private static final float SMOOTHING_SPEED = 0.75f;
	private static final float SMOOTHING_CONSTANT = (float) (0.016 / Math
			.log(SMOOTHING_SPEED));
	private float mSmoothingTime;
	private float mTouch;
	private float mLastTouch;

	private static final float BASELINE_FLING_VELOCITY = 2500.f;
	private static final float FLING_VELOCITY_INFLUENCE = 0.4f;
	
	private static final float SCROLL_ANIMATER_ONE = 0.15f;
	private static final float SCROLL_ANIMATER_TWO = 0.85f;

	public FolderPagedView(Context context) {
		super(context);
		init();
	}

	public FolderPagedView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		init();
	}

	public FolderPagedView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init();
	}

	private void init() {

		Context context = getContext();
		mScroller = new Scroller(context,
				new AccelerateDecelerateInterpolator());
		mCurrentScreen = 0;

		final ViewConfiguration configuration = ViewConfiguration
				.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	@Override
	public void scrollTo(int x, int y) {
		super.scrollTo(x, y);
		mTouch = x;

		mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			mScrollX = mScroller.getCurrX();
			RuntimeException e = new RuntimeException();
			e.fillInStackTrace();
			mScrollY = mScroller.getCurrY();
			mTouch = mScrollX;
			mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
			postInvalidate();
		} else if (mNextScreen != INVALID_SCREEN) {
			mCurrentScreen = Math.max(0,
					Math.min(mNextScreen, getChildCount() - 1));
			mNextScreen = INVALID_SCREEN;
		} else if (mTouchState == TOUCH_STATE_SCROLLING) {
			final float now = System.nanoTime() / NANOTIME_DIV;
			final float e = (float) Math.exp((now - mSmoothingTime)
					/ SMOOTHING_CONSTANT);
			float distance = mTouch - mScrollX;
			distance = mTouch - mScrollX;
			mScrollX += distance * e;

			mSmoothingTime = now;

			// Keep generating points as long as we're more than 1px away from
			// the target
			if (distance > 1.f || distance < -1.f) {
				postInvalidate();
			}
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		// super.dispatchDraw(canvas);
		// if(true)
		// return;
		boolean restore = false;
		int restoreCount = 0;

		// ViewGroup.dispatchDraw() supports many features we don't need:
		// clip to padding, layout animation, animation listener, disappearing
		// children, etc. The following implementation attempts to fast-track
		// the drawing dispatch by drawing only what we know needs to be drawn.

		boolean fastDraw = mTouchState != TOUCH_STATE_SCROLLING
				&& mNextScreen == INVALID_SCREEN;
		// If we are not scrolling or flinging, draw only the current screen
		if (fastDraw) {
			drawChild(canvas, getChildAt(mCurrentScreen), getDrawingTime());
		} else {
			final long drawingTime = getDrawingTime();

			final float scrollPos = (float) mScrollX / getWidth();
			final int leftScreen = (int) scrollPos;
			final int rightScreen = leftScreen + 1;
			if (leftScreen >= 0) {
				drawChild(canvas, getChildAt(leftScreen), drawingTime);
			}
			if (scrollPos != leftScreen && rightScreen < getChildCount()) {
				drawChild(canvas, getChildAt(rightScreen), drawingTime);
			}

		}

		if (restore) {
			canvas.restoreToCount(restoreCount);
		}
	}

	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		computeScroll();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"SmoothPagedView can only be used in EXACTLY mode.");
		}

		final int height = MeasureSpec.getSize(heightMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"SmoothPagedView can only be used in EXACTLY mode.");
		}

		// The children are given the same width and height as the workspace
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}

		if (mCurrWidth != width || mCurrHeight != height) {
			scrollTo(mCurrentScreen * width, 0);
			mCurrWidth = width;
			mCurrHeight = height;
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		final int count = getChildCount();

		int childLeft = 0;
		int childWidth;
		View child;
		for (int i = 0; i < count; i++) {
			child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {
				childWidth = child.getMeasuredWidth();
				child.layout(childLeft, 0, childLeft + childWidth,
						child.getMeasuredHeight());
				childLeft += childWidth;
			}
		}
	}

	private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
	}

	private void releaseVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST)) {
			return true;
		}

		acquireVelocityTrackerAndAddMovement(ev);

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			final float x = ev.getX();
			// Remember location of down touch
			mLastMotionX = x;
			mLastTouch = mScrollX;

			/*
			 * If being flinged and user touches the screen, initiate drag;
			 * otherwise don't. mScroller.isFinished should be false when being
			 * flinged.
			 */
			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			final float x = ev.getX();
			final int xDiff = (int) Math.abs(x - mLastMotionX);
			final int touchSlop = mTouchSlop;
			boolean xMoved = xDiff > touchSlop;
			if (xMoved) {
				// Scroll if the user moved far enough along the X axis
				mTouchState = TOUCH_STATE_SCROLLING;
				mLastMotionX = x;
				mTouch = mScrollX;
				mLastTouch = mScrollX;
				mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
			}
			break;
		}

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mTouchState = TOUCH_STATE_REST;
			releaseVelocityTracker();
			break;
		}

		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode.
		 */
		return mTouchState != TOUCH_STATE_REST;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mTouchState != TOUCH_STATE_SCROLLING) {
			onInterceptTouchEvent(ev);
			return true;
		}
		acquireVelocityTrackerAndAddMovement(ev);

		final int action = ev.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			// Remember where the motion event started
			mLastMotionX = ev.getX();
			mLastTouch = mScrollX;
			break;
		case MotionEvent.ACTION_MOVE:
			// Scroll to follow the motion event

			float x = ev.getX();

			float deltaX = mLastMotionX - x;
			float newTouch = mLastTouch + deltaX;
			int childCount = getChildCount();
			if (childCount > 1 && newTouch >= 0
					&& newTouch <= (getChildAt(childCount - 2).getRight())) {
				mTouch = newTouch;
				mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
				invalidate();
				break;
			}

			float leftmost = (float) -(getWidth() * SCROLL_ANIMATER_ONE);
			float rightmost = (float) (getWidth() * SCROLL_ANIMATER_ONE);
			if (childCount > 0) {
				rightmost = (float) (getChildAt(childCount - 1).getRight() - getWidth() * SCROLL_ANIMATER_TWO);
			}
			deltaX = (float) (getWidth() * SCROLL_ANIMATER_ONE * Math
					.sin((deltaX / getWidth()) * (Math.PI / 2)));
			newTouch = mLastTouch + deltaX;

			if (newTouch < leftmost) {
				newTouch = leftmost;
			} else if (newTouch > rightmost) {
				newTouch = rightmost;
			}
			if (mTouch != newTouch) {
				mTouch = newTouch;
				mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
				invalidate();
			}

			break;
		case MotionEvent.ACTION_UP:
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

			final int velocityX = (int) velocityTracker.getXVelocity();
			final int screenWidth = getWidth();
			final int whichScreen = (mScrollX + (screenWidth / 2))
					/ screenWidth;
			if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
				snapToScreen(mCurrentScreen - 1, velocityX, true);
			} else if (velocityX < -SNAP_VELOCITY
					&& mCurrentScreen < getChildCount() - 1) {
				snapToScreen(mCurrentScreen + 1, velocityX, true);
			} else {
				snapToScreen(whichScreen, 0, true);
			}

			mTouchState = TOUCH_STATE_REST;
			releaseVelocityTracker();
			break;
		case MotionEvent.ACTION_CANCEL:
			final int screenWidth1 = getWidth();
			final int whichScreen1 = (mScrollX + (screenWidth1 / 2))
					/ screenWidth1;
			snapToScreen(whichScreen1, 0, true);

			mTouchState = TOUCH_STATE_REST;
			releaseVelocityTracker();
			break;
		}

		return true;
	}

	void setCurrentScreen(int currentScreen) {

		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}
		mNextScreen = INVALID_SCREEN;
		mCurrentScreen = Math.max(0,
				Math.min(currentScreen, getChildCount() - 1));
		scrollTo(mCurrentScreen * getWidth(), 0);

		invalidate();
	}

	int getCurrentScreen() {
		return mCurrentScreen;
	}

	int getCurrentScreenInNegativeDirection() {
		return (getChildCount() - 1 - mCurrentScreen);
	}

	void snapToScreen(int whichScreen) {
		snapToScreen(whichScreen, 0, false);
	}

	private void snapToScreen(int whichScreen, int velocity, boolean settle) {

		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));

		mNextScreen = whichScreen;

		final int screenDelta = Math.max(1,
				Math.abs(whichScreen - mCurrentScreen));

		int duration = (screenDelta + 1) * 100;
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}

		velocity = Math.abs(velocity);
		if (velocity > 0) {
			duration += (duration / (velocity / BASELINE_FLING_VELOCITY))
					* FLING_VELOCITY_INFLUENCE;
		} else {
			duration += 100;
		}

		awakenScrollBars(duration);
		final int newX = whichScreen * getWidth();
		final int delta = newX - mScrollX;
		mScroller.startScroll(mScrollX, 0, delta, 0, duration);

		invalidate();
	}

}
