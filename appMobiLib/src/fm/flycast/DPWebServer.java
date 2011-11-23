package fm.flycast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

/**
 * Main Web server class.
 * @author Sudeep
 *
 */
public class DPWebServer {
	private final ExecutorService pool;
	public String _path;
	ServerSocket serverSocket = null;
	
	public DPWebServer(String path, int port) {
		//Log.d("DPWebServer", " In DPWebServer Port " + port);
		_path = path;
        _port = port;
        pool = Executors.newCachedThreadPool();
    }
    
    public void activate() throws Exception{
    	_active = true;
    	//Log.d("DPWebServer", " In DPWebServer.activate() method ");
       
        try {
            serverSocket = new ServerSocket(_port);
        }
        catch (Exception e) {
            throw e;
        }
        
        new Thread() {
        	public void run() {
                //Log.d("DPWebServer", " created HTTP Flycast Thread Group ");
                while (_active) {
                    // Pass the socket to a new thread so that it can be dealt with
                    // while we can go and get ready to accept another connection.
                	try {
                		Socket socket = serverSocket.accept();
                        DPRequestThread requestThread = new DPRequestThread(_path, socket);
                        pool.execute(requestThread);
                	} catch(java.net.SocketException se) {
                		if("Interrupted system call".equals(se.getMessage())){/*eat it - we stopped it*/}
                	} catch (IOException e) {
						if(Debug.isDebuggerConnected()) {
							Log.d("[appMobi]", e.getMessage(), e);
						}
					}
                }
        		
        	}
        }.start();
    }    
    
    public synchronized void stop(){
        this._active = false;
        try {
        	if(this.serverSocket!=null){
        		this.serverSocket.close();
            	this.serverSocket = null;
        	}
        } catch (Exception e) {
            throw new RuntimeException("Error closing server", e);
        }
    }
    
    private int _port;
    public boolean _active = false;
}
