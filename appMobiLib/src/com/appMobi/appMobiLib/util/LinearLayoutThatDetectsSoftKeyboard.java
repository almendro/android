package com.appMobi.appMobiLib.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/*
 * from http://stackoverflow.com/questions/2150078/android-is-software-keyboard-shown
 */

public class LinearLayoutThatDetectsSoftKeyboard extends LinearLayout {
	private boolean wasShowing = false;
	
    public LinearLayoutThatDetectsSoftKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearLayoutThatDetectsSoftKeyboard(Context context) {
    	super(context);
	}

	public interface Listener {
        public void onSoftKeyboardShown(boolean isShowing);
    }
    private Listener listener;
    public void setListener(Listener listener) {
        this.listener = listener;
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
        if (listener != null) {
        	if(diff>128 != wasShowing){// assume all soft keyboards are at least 128 pixels high
            	//only fire the event if it is a change
        		wasShowing = diff>128;
                listener.onSoftKeyboardShown(wasShowing); 
        	}
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);       
    }
}
