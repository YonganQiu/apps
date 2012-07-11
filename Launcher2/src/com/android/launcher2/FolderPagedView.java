package com.android.launcher2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import com.android.launcher.R;

public class FolderPagedView extends ViewGroup{
	
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
	private float mLastMotionY;

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

	private boolean mIsXScrolled;

	private int mDefaultScreen = 0;

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

		mIsXScrolled = true;
		mDefaultScreen = 0;
		Context context = getContext();
		mScroller = new Scroller(context,
				new AccelerateDecelerateInterpolator());
		mCurrentScreen = mDefaultScreen;

		final ViewConfiguration configuration = ViewConfiguration
				.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		removeAllViews();
		Log.e("lljhome", "Childcount" + getChildCount());
		CellLayout cl = (CellLayout) LayoutInflater.from(getContext()).inflate(
				R.layout.folder_layout, this, false);

		cl.setGridSize(0, 0);
		addView(cl);

			Log.e("lljhome","Childcount" + getChildCount());
		}

		@Override
		public void scrollTo(int x, int y) {
			super.scrollTo(x, y);
			if (mIsXScrolled) {
				mTouch = x;
			} else {
				mTouch = y;
			}
			mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
		}

		@Override
		public void computeScroll() {
			if (mScroller.computeScrollOffset()) {
				mScrollX = mScroller.getCurrX();
				RuntimeException e = new RuntimeException();
				e.fillInStackTrace();
				mScrollY = mScroller.getCurrY();
				if (mIsXScrolled) {
					mTouch = mScrollX;
				} else {
					mTouch = mScrollY;
				}
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
				if (mIsXScrolled) {
					distance = mTouch - mScrollX;
					mScrollX += distance * e;
				} else {
					distance = mTouch - mScrollY;
					mScrollY += distance * e;
				}

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
				if (mIsXScrolled) {
					final float scrollPos = (float) mScrollX / getWidth();
					final int leftScreen = (int) scrollPos;
					final int rightScreen = leftScreen + 1;
					if (leftScreen >= 0) {
						drawChild(canvas, getChildAt(leftScreen), drawingTime);
					}
					if (scrollPos != leftScreen && rightScreen < getChildCount()) {
						drawChild(canvas, getChildAt(rightScreen), drawingTime);
					}
				} else {
					final float scrollPos = (float) mScrollY / getHeight();
					final int topScreen = (int) scrollPos;
					final int bottomScreen = topScreen + 1;
					if (topScreen >= 0) {
						drawChild(canvas, getChildAt(topScreen), drawingTime);
					}
					if (scrollPos != topScreen && bottomScreen < getChildCount()) {
						drawChild(canvas, getChildAt(bottomScreen), drawingTime);
					}
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
				if (mIsXScrolled) {
					scrollTo(mCurrentScreen * width, 0);
				} else {
					scrollTo(0, mCurrentScreen * height);
				}
				mCurrWidth = width;
				mCurrHeight = height;
			}
		}

		@Override
		protected void onLayout(boolean changed, int left, int top, int right,
				int bottom) {
			final int count = getChildCount();

			if (mIsXScrolled) {
				int childLeft = 0;
				for (int i = 0; i < count; i++) {
					final View child = getChildAt(i);
					if (child.getVisibility() != View.GONE) {
						final int childWidth = child.getMeasuredWidth();
						child.layout(childLeft, 0, childLeft + childWidth,
								child.getMeasuredHeight());
						childLeft += childWidth;
					}
				}
			} else {
				int childTop = 0;
				for (int i = 0; i < count; i++) {
					final View child = getChildAt(i);
					if (child.getVisibility() != View.GONE) {
						final int childHeight = child.getMeasuredHeight();
						child.layout(0, childTop, child.getMeasuredWidth(),
								childTop + childHeight);
						childTop += childHeight;
					}
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
				final float y = ev.getY();
				// Remember location of down touch
				mLastMotionX = x;
				mLastMotionY = y;
				if (mIsXScrolled) {
					mLastTouch = mScrollX;
				} else {
					mLastTouch = mScrollY;
				}

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
				final float y = ev.getY();
				final int xDiff = (int) Math.abs(x - mLastMotionX);
				final int yDiff = (int) Math.abs(y - mLastMotionY);

				final int touchSlop = mTouchSlop;
				boolean xMoved = xDiff > touchSlop;
				boolean yMoved = yDiff > touchSlop;
				if (mIsXScrolled && xMoved) {
					// Scroll if the user moved far enough along the X axis
					mTouchState = TOUCH_STATE_SCROLLING;
					mLastMotionX = x;
					mTouch = mScrollX;
					mLastTouch = mScrollX;
					mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
				} else if (!mIsXScrolled && yMoved) {
					// Scroll if the user moved far enough along the Y axis
					mTouchState = TOUCH_STATE_SCROLLING;
					mLastMotionY = y;
					mTouch = mScrollY;
					mLastTouch = mScrollY;
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
				mLastMotionY = ev.getY();
				if (mIsXScrolled) {
					mLastTouch = mScrollX;
				} else {
					mLastTouch = mScrollY;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (mTouchState == TOUCH_STATE_SCROLLING) {
					// Scroll to follow the motion event
					if (mIsXScrolled) {
						float x = ev.getX();

						float deltaX = mLastMotionX - x;
						float newTouch = mLastTouch + deltaX;
						int childCount = getChildCount();
						if (childCount > 1
								&& newTouch >= 0
								&& newTouch <= (getChildAt(childCount - 2)
										.getRight())) {
							mTouch = newTouch;
							mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
							invalidate();
							break;
						}

						float leftmost = (float) -(getWidth() * 0.15);
						float rightmost = (float) (getWidth() * 0.15);
						if (childCount > 0) {
							rightmost = (float) (getChildAt(childCount - 1)
									.getRight() - getWidth() * 0.85);
						}
						deltaX = (float) (getWidth() * 0.15 * Math
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
					} else {
						final float y = ev.getY();
						float deltaY = mLastMotionY - y;
						float newTouch = mLastTouch + deltaY;
						int childCount = getChildCount();
						if (childCount > 1
								&& newTouch >= 0
								&& newTouch <= (getChildAt(childCount - 2)
										.getBottom())) {
							mTouch = newTouch;
							mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
							invalidate();
							break;
						}

						float topmost = (float) -(getHeight() * 0.15);
						float bottommost = (float) (getHeight() * 0.15);
						if (childCount > 0) {
							bottommost = (float) (getChildAt(childCount - 1)
									.getBottom() - getHeight() * 0.85);
						}
						deltaY = (float) (getHeight() * 0.15 * Math
								.sin((deltaY / getHeight()) * (Math.PI / 2)));
						newTouch = mLastTouch + deltaY;
						if (newTouch < topmost) {
							newTouch = topmost;
						} else if (newTouch > bottommost) {
							newTouch = bottommost;
						}
						if (mTouch != newTouch) {
							mTouch = newTouch;
							mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
							invalidate();
						}
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mTouchState == TOUCH_STATE_SCROLLING) {
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

					if (mIsXScrolled) {
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
					} else {
						final int velocityY = (int) velocityTracker.getYVelocity();
						final int screenHeight = getHeight();
						final int whichScreen = (mScrollY + (screenHeight / 2))
								/ screenHeight;

						if (velocityY > SNAP_VELOCITY && mCurrentScreen > 0) {
							snapToScreen(mCurrentScreen - 1, velocityY, true);
						} else if (velocityY < -SNAP_VELOCITY
								&& mCurrentScreen < getChildCount() - 1) {
							snapToScreen(mCurrentScreen + 1, velocityY, true);
						} else {
							snapToScreen(whichScreen, 0, true);
						}
					}

				}
				mTouchState = TOUCH_STATE_REST;
				releaseVelocityTracker();
				break;
			case MotionEvent.ACTION_CANCEL:
				if (mTouchState == TOUCH_STATE_SCROLLING) {
					if (mIsXScrolled) {
						final int screenWidth = getWidth();
						final int whichScreen = (mScrollX + (screenWidth / 2))
								/ screenWidth;
						snapToScreen(whichScreen, 0, true);
					} else {
						final int screenHeight = getHeight();
						final int whichScreen = (mScrollY + (screenHeight / 2))
								/ screenHeight;
						snapToScreen(whichScreen, 0, true);
					}
				}
				mTouchState = TOUCH_STATE_REST;
				releaseVelocityTracker();
				break;
			}

			return true;
		}

		public void moveToDefaultScreen(boolean animate) {
			if (animate) {
				snapToScreen(mDefaultScreen);
			} else {
				setCurrentScreen(mDefaultScreen);
			}
			getChildAt(mDefaultScreen).requestFocus();
		}

		void setCurrentScreen(int currentScreen) {
			
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			mNextScreen = INVALID_SCREEN;
			mCurrentScreen = Math.max(0,
					Math.min(currentScreen, getChildCount() - 1));
			if (mIsXScrolled) {
				scrollTo(mCurrentScreen * getWidth(), 0);
			} else {
				scrollTo(0, mCurrentScreen * getHeight());
			}
			invalidate();
		}

		int getCurrentScreen() {
			return mCurrentScreen;
		}
		
		int getCurrentScreenInNegativeDirection(){
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
			if (mIsXScrolled) {
				final int newX = whichScreen * getWidth();
				final int delta = newX - mScrollX;
				mScroller.startScroll(mScrollX, 0, delta, 0, duration);
			} else {
				final int newY = whichScreen * getHeight();
				final int delta = newY - mScrollY;
				mScroller.startScroll(0, mScrollY, 0, delta, duration);
			}
			invalidate();
		}
	
}
