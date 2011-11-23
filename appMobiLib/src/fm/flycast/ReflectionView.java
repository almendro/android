package fm.flycast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ReflectionView extends ImageView
{
	public Bitmap _bitmap = null;
	public int height = 26;
	public int width = 160;
	
	public ReflectionView(Context context)
	{
		super(context);
	}
	
	public ReflectionView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	protected void onSizeChanged (int w, int h, int oldw, int oldh)
	{
		width = w;
		height = h;
		createMirror();
	}
	
	public void createMirror()
	{
		if( _bitmap == null ) return;
        
		try
		{
	        Matrix matrix = new Matrix();
	        matrix.preScale(1, -1);
	        
	        if( _bitmap.getWidth() != 160 || _bitmap.getHeight() != 160 )
	        {
	        	_bitmap = Bitmap.createScaledBitmap( _bitmap, 160, 160, true );
	        }
	        
	        Bitmap reflection = Bitmap.createBitmap(_bitmap, 0, 160-height, width, height, matrix, false);
	        
	        Canvas canvas = new Canvas(reflection);  
	        LinearGradient shader = new LinearGradient(0, 0, 0, height, 0x9FFFFFFF, 0x2FFFFFFF, TileMode.CLAMP); 
	        Paint paint = new Paint();
	        paint.setShader(shader);
	        paint.setXfermode(new PorterDuffXfermode(Mode.LIGHTEN));
	        canvas.drawRect(0, 0, width, height, paint);
	        
	        super.setImageDrawable(new BitmapDrawable(reflection));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void setImageBitmap(Bitmap bm)
	{
		super.setImageBitmap(bm);
		_bitmap = bm;
		createMirror();
	}
	
	public void setImageDrawable(Drawable drawable)
	{
		super.setImageDrawable(drawable);
		_bitmap = ((BitmapDrawable)drawable).getBitmap();
		createMirror();
	}
	
	public void setImageResource(int resId)
	{
		super.setImageResource(resId);
		_bitmap = BitmapFactory.decodeResource(getResources(), resId);
		createMirror();
	}
}