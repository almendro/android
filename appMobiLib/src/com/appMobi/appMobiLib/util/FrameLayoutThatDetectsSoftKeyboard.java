package com.appMobi.appMobiLib.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/*
 * from http://stackoverflow.com/questions/2150078/android-is-software-keyboard-shown
 */

public class FrameLayoutThatDetectsSoftKeyboard extends FrameLayout {
	private boolean isShowing = false;
	
    public boolean isSoftKeyboardShowing() {
		return isShowing;
	}

	public FrameLayoutThatDetectsSoftKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FrameLayoutThatDetectsSoftKeyboard(Context context) {
    	super(context);
	}

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Activity activity = (Activity)getContext();
        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
        int diff = (screenHeight - statusBarHeight) - height;
        isShowing = diff>128;// assume all soft keyboards are at least 128 pixels high
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);       
    }
}
