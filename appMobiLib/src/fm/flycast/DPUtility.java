package fm.flycast;

/**
 * Class to keep the utility function to be used in the
 * in the application. 
 * 
 * @author Administrator
 *
 */
public class DPUtility {
	
	
	/**
	  * Escape characters for text appearing as XML data, between tags.
	  * 
	  * <P>The following characters are replaced with corresponding character entities :
	  * <table border='1' cellpadding='3' cellspacing='0'>
	  * <tr><th> Character </th><th> Encoding </th></tr>
	  * <tr><td> < </td><td> &lt; </td></tr>
	  * <tr><td> > </td><td> &gt; </td></tr>
	  * <tr><td> & </td><td> &amp; </td></tr>
	  * <tr><td> " </td><td> &quot;</td></tr>
	  * <tr><td> ' </td><td> &#039;</td></tr>
	  * </table>
	  * 
	  * <P>Note that JSTL's {@code <c:out>} escapes the exact same set of 
	  * characters as this method. <span class='highlight'>That is, {@code <c:out>}
	  *  is good for escaping to produce valid XML, but not for producing safe 
	  *  HTML.</span>
	  */
public static String EscapeXML(String input){
		String retVal = input.replaceAll("&", "&amp;");
		retVal = retVal.replaceAll("<", "&lt;");
		retVal = retVal.replaceAll(">", "&gt;");
		retVal = retVal.replaceAll("\"", "&quot;");
		retVal = retVal.replaceAll("'", "&#039;");
		//retVal = input.replaceAll(" & ", "&amp;");
		return retVal;
	}
}
