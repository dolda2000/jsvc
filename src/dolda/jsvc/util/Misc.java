package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;
import java.io.*;

public class Misc {
    private static Map<Integer, String> stext = new HashMap<Integer, String>();
    
    static {
	stext.put(200, "OK");
	stext.put(300, "Multiple Choices");
	stext.put(301, "Permanently Moved");
	stext.put(302, "Temporarily Moved");
	stext.put(303, "See Other");
	stext.put(304, "Not Modified");
	stext.put(400, "Bad Request");
	stext.put(401, "Authentication Required");
	stext.put(403, "Access Forbidden");
	stext.put(404, "Resource Not Found");
	stext.put(500, "Server Error");
    }
    
    public static String statustext(int status) {
	String text;
	if((text = stext.get(status)) != null)
	    return(text);
	return("Server Flimsiness");
    }
    
    public static String stripslashes(String p, boolean beg, boolean end) {
	while(end && (p.length() > 0) && (p.charAt(p.length() - 1) == '/'))
	    p = p.substring(0, p.length() - 1);
	while(beg && (p.length() > 0) && (p.charAt(0) == '/'))
	    p = p.substring(1);
	return(p);
    }

    public static void cpstream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        while(true) {
	    int ret = in.read(buf, 0, buf.length);
            if(ret < 0)
                return;
	    out.write(buf, 0, ret);
        }
    }
    
    public static Responder stdroot(Responder root) {
	Responder ret = root;
	ret = new Rehandler(ret);
	ret = new ErrorHandler(ret);
	return(ret);
    }
}
