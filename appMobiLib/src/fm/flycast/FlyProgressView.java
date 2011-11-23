package fm.flycast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class FlyProgressView extends View
{
	private static int _tintColor = 0xFFFFFF00;//Yellow
	private static int _markColor = 0xFFFF0000;//Red
	private static int _backColor = 0xFF000000;//Black
	private static int _doneColor = 0xFF00FF00;//Green
	boolean _isDone = false;
	float _position = -1.0f;
	float _progress = -1.0f;
	int width = 320;
	
	public FlyProgressView(Context context)
	{
		super(context);
	}
	
	public FlyProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onDraw (Canvas canvas)
	{
		Paint paint = new Paint();
		paint.setColor(_backColor);
		paint.setStyle(Paint.Style.FILL);
		
		Rect rr = canvas.getClipBounds();
		width = rr.width();
		
		// Background
		canvas.drawColor(_backColor);
		
		// Download marker
		if( _isDone == false )
		{
			paint.setColor(_tintColor);
		}
		else
		{
			paint.setColor(_doneColor);
		}		
		canvas.drawRect( new Rect(0, 0, (int) (width * _progress), 1), paint );

		// Playing marker
		if( _position >= 0.0 )
		{
			paint.setColor(_markColor);
			int xcoord = (int) (_position * (width-6));
			canvas.drawRect( new Rect(xcoord, 0, xcoord+6, 1), paint );
		}
		
		//Log.d("[appMobi]", _tintColor + ":" + _markColor + ":" + _backColor + ":" + _doneColor);
	}
	
	static public void setTintColor(int aColor )
	{
		_tintColor = aColor;
	}

	static public void setMarkColor(int aColor )
	{
		_markColor = aColor;
	}

	static public void setBackColor(int aColor )
	{
		_backColor = aColor;
	}

	static public void setDoneColor(int aColor )
	{
		_doneColor = aColor;
	}

	public void setDone(boolean done)
	{
		_isDone = done;
		invalidate();
	}

	public void setPosition(float position)
	{
		_position = position;
		invalidate();
	}

	public float getPosition()
	{
		return _position;
	}

	public void setProgress(float progress)
	{
		_progress = progress;
		invalidate();
	}
}