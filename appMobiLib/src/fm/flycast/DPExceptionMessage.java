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

public class DPExceptionMessage extends DPMessageObject{
	/*
	 * Details error message which needs to be sent to the client.
	 * This should not be the actual exception message as that 
	 * will not be able to tell much to the client.
	 * For example number format exception is of no use to the client 
	 * but Station id is not number is useful.
	 * 
	 */
	private String _ErrMessage = "";
	
	public static final int GENERIC_ERR_CODE = -1;
	
	private int _ErrCode = -1;
	
		
	public String get_ErrMessage() {
		return _ErrMessage;
	}

	public void set_ErrMessage(String errMessage) {
		_ErrMessage = errMessage;
	}

	public int get_ErrCode() {
		return _ErrCode;
	}

	public void set_ErrCode(int errCode) {
		_ErrCode = errCode;
	}
	
	public DPExceptionMessage(String eMsgg, int errCode){
		_ErrMessage = eMsgg;
		_ErrCode = errCode;
	}
	
	public DPExceptionMessage(String eMsgg){
		_ErrMessage = eMsgg;
		_ErrCode = DPExceptionMessage.GENERIC_ERR_CODE;
	}
	
	

}
