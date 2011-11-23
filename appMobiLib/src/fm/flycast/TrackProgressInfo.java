package fm.flycast;

import android.os.Parcel;
import android.os.Parcelable;

    public final class TrackProgressInfo implements Parcelable {
    	public int downloadPercent=0;
    	public int playedPercent=0;
    	public Boolean isLive=false;
    	public Boolean isPlaying=false;

        public static final Parcelable.Creator<TrackProgressInfo> CREATOR = new Parcelable.Creator<TrackProgressInfo>() {
            public TrackProgressInfo createFromParcel(Parcel in) {
                return new TrackProgressInfo(in);
            }

            public TrackProgressInfo[] newArray(int size) {
                return new TrackProgressInfo[size];
            }
        };

        public TrackProgressInfo() {
        }

        private TrackProgressInfo(Parcel in) {
            readFromParcel(in);
        }


        public void readFromParcel(Parcel in) {
        	downloadPercent = in.readInt();
            playedPercent = in.readInt();
            isLive = (Boolean)in.readValue(null);
            isPlaying = (Boolean)in.readValue(null);
        }

		//@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		//@Override
		public void writeToParcel(Parcel out, int flags) {
            out.writeInt(downloadPercent);
            out.writeInt(playedPercent);
            out.writeValue(isLive);
            out.writeValue(isPlaying);
		}
    	
    }