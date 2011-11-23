package fm.flycast;

/**
 * Class to keep the state of the track as in DFA. 
 * Intially there are two states only 
 * Track added represented by bit 1
 * Track buffered represented by bit 2.
 * 
 * 
 * @author Administrator
 *
 */
public class DPTrackState {

	/**
	 * Variable to keep track of the state changes of the track 
	 * at the Device proxy. 
	 * 
	 */
	private int flag = 0;
	
	/**
	 * variable to keep the information sent to the client 
	 * 
	 */
	private int flagClient = 0;
	
	public int GetTrackState(){
		return flag;
	}
	
	/**
	 * Set the track added state. It will throw exception if 
	 * track state has already been set either by track added 
	 * or track buffered.
	 * @throws Exception
	 */
	public void setAddTrackState() throws Exception{
		
		if( flag > 3 ){
			throw new Exception("Invalid state " + flag);
		}
	
		//Some state already exists.
		if( flag != 0 )
		{
			//Is it the state of the Add ?
			if( flag == 3){//No this is in buffered state.
				throw new Exception("Trying to set added State to track for already buffered track.");
			}
			//If we reach here then track has already been marked as added.
			if( flag == 1){//Currently only for debug throw exception.
				System.err.println("Trying to set added State to track for already added track.");
				//throw new Exception("Trying to set added State to track for already added track.");
				return;
			}
		}
		else
		{//Mark the track as added.
			flag = flag | 1;
		}
	}
	
	/**
	 * Set the track in the buffered state. Will throw the exception 
	 * if the track has not been set previously in the added state 
	 * or the buffered state has already been set.
	 * @throws Exception
	 */
	public void setBufferedTrackState() throws Exception{
		
		if( flag > 3 ){
			throw new Exception("Invalid state " + flag);
		}
		//Some state already exists.
		if( flag == 3){//Already buffered state.
			//throw new Exception("Trying to set buffered State to track for already buffered track.");
			System.err.println("Trying to set buffered State to track for already buffered track.");
			return;
		}
		if( flag == 0){//Add state has not been set for the track.
			throw new Exception("Trying to set buffered State to track for which has not been added yet.");
		}
		
		if( flag == 1){
			flag = flag | 2;
		}
	}

	/**
	 * Throws exception if the track add state has not been sent or 
	 * track has already been sent to the client.
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean AddStateSentToClient() throws Exception {
		if((flag & 1) != 1 ){
			throw new Exception("Track has not been added till now.");
		}
		
		if( (flagClient & 1) == 1){
			System.err.println("Track has already been sent to client.");
			//throw new Exception("Track has already been sent to client.");
			return true;
		}
		
		flagClient = (flagClient | 1 );
		
		return false;
	}
	
	/**
	 * Throws exception if the track buffered state has not been sent or 
	 * track has already been sent to the client.
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean BufferedStateSentToClient() throws Exception {
		if((flag & 3) != 3 ){
			throw new Exception("Track has not been added buffered till now.");
		}
		
		if( (flagClient & 2) == 2){
			System.err.println("Track has already been sent to client.");
			//throw new Exception("Track has already been sent to client.");
			return true;
		}
		
		flagClient = (flagClient | 2 );
		
		return false;
	}
}
