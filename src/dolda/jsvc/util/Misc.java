package dolda.jsvc.util;

import java.util.*;

public class Misc {
    private static Map<Integer, String> stext = new HashMap<Integer, String>();
    
    static {
	stext.put(200, "OK");
    }
    
    public static String statustext(int status) {
	String text;
	if((text = stext.get(status)) != null)
	    return(text);
	return("Unknown Response");
    }
}
