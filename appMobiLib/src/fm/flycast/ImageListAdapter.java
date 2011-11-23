package fm.flycast;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.appMobi.appMobiLib.util.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.appMobi.appMobiLib.R;


public class ImageListAdapter extends BaseAdapter {
	private static final String TAG_IMAGELISTADAPTER = "ImageListAdapter";
	public static final String TAG_IMAGEROW = "ImageRow";
	//public static final int FlyBackCount= 2;
    private Context mContext; // Remember our context so we can use it when constructing views	
    public DPXMLTracklist mTracklist = null; 
    public int mtrackcurrentindex = 0;
    public int mtrackmaxplayed = 0;    
    private DPXMLObject ChildXMLObject = null;
    private DPXMLTrack ChildXMLTrack = null;
	//private URI mURI;
    private Handler imagehandler = null; 
    
    public FlyCastPlayer appMobi = null;//new PlayerAppMobiLatest();
    
	public void lockView(){
		//if(Debug.isDebuggerConnected()) Log.d("ImageListAdapter", "lockView");
		// August 03,2010 If we use the following line sometimes it throws null Pointer exception
		//mTracklist = null;
	}    
    
	public void updateData(DPXMLTracklist tracklist){
		if(Debug.isDebuggerConnected()) Log.d(TAG_IMAGELISTADAPTER, "updateData");
		mTracklist = tracklist;
        //this.notifyDataSetInvalidated();
        this.notifyDataSetChanged ();
	}
    
    public ImageListAdapter(Context context, FlyCastPlayer player, DPXMLTracklist tracklist, int trackcurrentindex, int trackmaxplayed, Handler handler) {
    	//if(Debug.isDebuggerConnected()) Log.d("ImageListAdapter", "INIT");
    	mContext = context;
        mTracklist = tracklist;
        appMobi = player;
        if( tracklist != null )
        	if(Debug.isDebuggerConnected()) Log.d("Image", "Size of the track list is " + tracklist.children.size());
        mtrackcurrentindex = trackcurrentindex;
        mtrackmaxplayed = trackmaxplayed;
        setImagehandler(handler);
    }

    public int getCount() {
		if( mTracklist == null)
		{
			return 0;
		}  
		else {
//BH removed this			
//	    	if(Player.FLYBACKING){
//	    		return mTracklist.children.size();
//	    	}
	    	int Count = 0;
			for (int index = 0; index < mTracklist.children.size(); index++) {
				// BOB -- CHANGED THIS
				//if (index > mtrackmaxplayed + 1){//if (index > mtrackcurrentindex + 1){
				//	break;	//Biz rule - do not display track if beyond current play song + 1 
				//}
				DPXMLObject thisObject = (DPXMLObject) mTracklist.children.get(index);
				if (thisObject.type == DPXMLObject.TRACK){
					//DPXMLTrack temptrack = (DPXMLTrack)thisObject ;
					Count++;
				}
				//else break;
			}		
			return Count;
			//Count = mTracklist.children.size();
		}
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
    	
        
    	if(Debug.isDebuggerConnected()) Log.d("Image", "Get View Called for position " + position);
        ViewHolder holder;
        if (convertView == null) {
            ImageView i = new ImageView(mContext);
      	
            convertView = i;
        	
           //-- Create a ViewHolder and store references to the child views we want to bind data to
            holder = new ViewHolder();
            holder.mImageView = (ImageView)convertView;
        	
        	convertView.setTag(holder);        	
        }
        else {
            // Get the ViewHolder back to get fast access to the TextView and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }
        
        holder.mImageView.setAdjustViewBounds(true);
        holder.mImageView.setLayoutParams(new Gallery.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        holder.mImageView.setBackgroundResource(R.drawable.picture_frame);
        
        //holder.mImageView.setImageResource(R.drawable.unknown);
        
        try {
            if ((mTracklist==null)||(mTracklist.children.size() == 0) 
            		|| (position>=mTracklist.children.size()) || (position<0 == true)) {
            	//TODO: Handle this
            }
//            else if () {
//            	
//            }
            else {
//            	if (position>mTracklist.children.size()){
//            		
//            	}
            	ChildXMLObject = (DPXMLObject) mTracklist.children.elementAt(position);
            	if (ChildXMLObject==null){
            		//TODO: Handle this
            	}
            	else {
    	    		if (ChildXMLObject.type == XMLObject.TRACK) {
    	    			//--- THIS ROW IS A TRACK ------------
    	    			ChildXMLTrack = (DPXMLTrack) mTracklist.children.get(position);
    	    			
						if (ChildXMLTrack.flyback == true)							
							holder.mImageView.setImageResource(R.drawable.go_back);
						else if ( (ChildXMLTrack.delayed == true) || (ChildXMLTrack.listened == false) )
							holder.mImageView.setImageDrawable((Drawable)(appMobi.GetCustomImage(FlyCastPlayer.IMAGE_UNKNOWN)));
							//Commented and modified by Parveen on April 03,2010.
							//holder.mImageView.setImageResource(R.drawable.unknown);							
						else if ((ChildXMLTrack.imageurl == null) || (ChildXMLTrack.imageurl.equals("")))							
							holder.mImageView.setImageDrawable((Drawable)(appMobi.GetCustomImage(FlyCastPlayer.IMAGE_NO_ARTWORK)));
							//Commented and modified by Parveen on April 03,2010.
							//holder.mImageView.setImageResource(R.drawable.noartwork);							
						else if (ChildXMLTrack.imageoriginaldownloaded == true) 
						{
    	    				holder.mImageView.setImageDrawable(ChildXMLTrack.imageoriginal);    	    				    	    				
						}
    	    			else 
    	    				//Commented and modified by Parveen on April 03,2010.
    	    				//holder.mImageView.setImageResource(R.drawable.loading);  //Default Image
    	    				holder.mImageView.setImageDrawable((Drawable)(appMobi.GetCustomImage(FlyCastPlayer.IMAGE_LOADING)));    	    				
    	    	}            	
    	        }
            }        	
        }
        catch(Exception e ){
        	if(Debug.isDebuggerConnected()) Log.d(TAG_IMAGEROW, "GetView Exception, " + e.getMessage());
        }
        return convertView;	
    }	//public View getView...
    
    public void setImagehandler(Handler imagehandler) {
		this.imagehandler = imagehandler;
	}

	public Handler getImagehandler() {
		return imagehandler;
	}

	private class ViewHolder {
    	ImageView mImageView;
    } 
    
}
