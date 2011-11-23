package fm.flycast;

public class DPThreadLocalUrl {
	
	private static ThreadLocal<String> requestUrl = new ThreadLocal<String>();

	    
	    public static String getRequestUrl() {
	        return requestUrl.get();
	    }

	    public static void setRequestUrl(String url) {
	    	requestUrl.set(url);
	    }

}
