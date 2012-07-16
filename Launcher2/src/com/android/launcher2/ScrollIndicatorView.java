
package com.android.launcher2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.android.launcher.R;

public class ScrollIndicatorView extends View {

    private Bitmap[] mLetterBitmaps;

    private Paint mUnSelectPaint;

    private int mIconWidth;

    private int mIconHeight;

    private int mSpaceHeight;

    private int mIconCount;

    public static final int UNSELECT_ICON = -1;

    private int mSelectIndex = 0;

    private int mLetterWindowHeight;

    private Launcher mLauncher;
    private AppsCustomizeTabHost mAppsCustomizeTabHost;
    private boolean mShowLetterPopuWindow = true;
    
    private int mPaddingTop;
    
    private int mPaddingBottom;
    
    private int[] mEnables;
    
    public static final int ENABLE = 1;
    public static final int DISABLE = 2;

    public ScrollIndicatorView(Context context) {
        super(context);
        initShortcutSearchView(context);
    }

    public ScrollIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initShortcutSearchView(context);
    }

    public ScrollIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initShortcutSearchView(context);
    }

    private void initShortcutSearchView(Context context) {
        TypedArray letterIconIds = context.getResources()
                .obtainTypedArray(R.array.letter_icons_bar);
        mIconCount = letterIconIds.length();
        mLetterBitmaps = new Bitmap[mIconCount];
        for (int i = 0; i < mIconCount; i++) {
            mLetterBitmaps[i] = BitmapFactory.decodeResource(context.getResources(),
                    letterIconIds.getResourceId(i, 0));
            if (i == 0) {
                mIconWidth = mLetterBitmaps[i].getWidth();
                mIconHeight = mLetterBitmaps[i].getHeight();
            }
        }

        mEnables = new int[mIconCount];
        mEnables[0] = ENABLE;
        mUnSelectPaint = new Paint();
        mUnSelectPaint.setAlpha(120);
    }

    public void setParent(Launcher launcher, AppsCustomizeTabHost appsCustomizeTabHost) {
        mLauncher = launcher;
        mAppsCustomizeTabHost = appsCustomizeTabHost;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = wm.getDefaultDisplay().getRawWidth();
        int screenHeight = wm.getDefaultDisplay().getRawHeight();
       
        boolean isPort = screenHeight > screenWidth;
        if (isPort) {
            mPaddingTop = 0;
            mPaddingBottom = 0;
        } else {
            mPaddingTop = mLetterWindowHeight / 2 - mIconHeight / 2;
            mPaddingBottom = mLetterWindowHeight / 2 - mIconHeight / 2;
        }

        int residueHeight = height - mPaddingTop - mPaddingBottom - mIconHeight * mIconCount;
        if (residueHeight <= 0) {
            mSpaceHeight = 0;
        } else {
            mSpaceHeight = residueHeight / (mIconCount + 1);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onDraw(Canvas canvas) {
        canvas.translate(0, getWidth() / 2 - mIconWidth / 2);
        for (int i = 0; i < mIconCount; i++) {
            int top = mPaddingTop + i * (mIconHeight + mSpaceHeight) + mSpaceHeight;
            if (mSelectIndex == i) {
                canvas.drawBitmap(mLetterBitmaps[i], 5, top, null);
            } else if(mEnables[i] == ENABLE){
                mUnSelectPaint.setAlpha(120);
                canvas.drawBitmap(mLetterBitmaps[i], 5, top, mUnSelectPaint);
            } else {
                mUnSelectPaint.setAlpha(60);
                canvas.drawBitmap(mLetterBitmaps[i], 5, top, mUnSelectPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        final float x = event.getX();
        final float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                int focusIndex = getTouchIndex((int) x, (int) y);
                showLetterIndicator(focusIndex);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                showLetterIndicator(-1);
                break;
            }
        }
        return true;
    }

    private int getTouchIndex(int x, int y) {
        if (x < -getWidth()) {
            return -1;
        }

        int index = y / (mIconHeight + mSpaceHeight);
        if (index < 0) {
            index = 0;
        } else if (index >= mIconCount) {
            index = mIconCount - 1;
        }

        return index;
    }

    private void showLetterIndicator(int focusIndex) {
        if (focusIndex == -1 || mEnables[focusIndex] != ENABLE) {
            return;
        }
        if (mSelectIndex == focusIndex) {
            int y = mIconHeight / 2 + (mSelectIndex * (mIconHeight + mSpaceHeight) + mPaddingTop);
            mLauncher.showLetterPopuWindow(mSelectIndex, y);
            mAppsCustomizeTabHost.updateLetterIndex(mSelectIndex, -y, mIconCount);
            return;
        }
        mSelectIndex = focusIndex;
        invalidate();

        int y = mIconHeight / 2 + /*mLetterWindowHeight / 2 +*/ (mSelectIndex
                * (mIconHeight + mSpaceHeight) + mPaddingTop);
        mLauncher.showLetterPopuWindow(mSelectIndex, y);
        mAppsCustomizeTabHost.updateLetterIndex(mSelectIndex, -y, mIconCount);
    }
    
    public void setSelectIndex(int selectIndex){
        mSelectIndex = selectIndex;
        invalidate();
    }

    public int getSelectIndex(){
        return mSelectIndex;
    }
    
    public void setEnables(int[] enables) {
        System.arraycopy(enables, 0, mEnables, 1, enables.length);
    }
}
