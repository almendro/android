package fm.flycast;

/**
 * Class to represent the exceptions which can be encountered in the webserver.
 * Since most of the work is being done independent of the UI device we need
 * this class to store the exception till we can send them back to the
 * client.
 * 
 * @author Administrator
 *
 */
public class DPException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 999L;

	/*
	 * Details error message which needs to be sent to the client.
	 * This should not be the actual exception message as that 
	 * will not be able to tell much to the client.
	 * For example number format exception is of no use to the client 
	 * but Station id is not number is useful.
	 * 
	 * IT SHOULD ALWAYS RETUN THE URL WE GOT TO THE CLIENT TO HELP HIM IN DEBUG.
	 * 
	 */
	private String _ErrMessage = "";
	
	public static final int GENERIC_ERR_CODE = -1;
	
	private int _ErrCode = -1;
	
		
	public String get_ErrMessage() {
		return _ErrMessage;
	}

	public int get_ErrCode() {
		return _ErrCode;
	}

	public DPException(DPException dpexc ){
		_ErrMessage=dpexc.get_ErrMessage();
		_ErrCode= dpexc.get_ErrCode();
	}
	
	public DPException(String eMsgg, int errCode){
		_ErrMessage = eMsgg;
		_ErrCode = errCode;
	}
	
	public DPException(String eMsgg){
		_ErrMessage = eMsgg;
		_ErrCode = GENERIC_ERR_CODE;
	}
	
	public DPException(Exception e){
		_ErrMessage = e.getMessage();
		_ErrCode = -1;
		
	}
	
	public String generateXML(){
		String retVal="<XML>";
		retVal += "<ResourceContainer name=\"EXCEPTION\">";
		retVal += "<Metadata/>";
		retVal += "<Resource>";
		retVal += "<key name=\"ErrorMessage\" value=\""+_ErrMessage+ "\" />";
		
		String URL = DPThreadLocalUrl.getRequestUrl();
		retVal += "<key name=\"ErrorCode\" value=\""+_ErrCode+"\" />";
		if( URL != null ){
			retVal += "<key name=\"Request\" value=\""+DPUtility.EscapeXML(URL)+"\" />";
		}
		
		retVal += "</Resource>";
		retVal += "</ResourceContainer>";
		retVal += "</XML>";
		
		return retVal;
	}
}
