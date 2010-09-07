package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;
import java.io.*;

public class Misc {
    public static final java.nio.charset.Charset utf8 = java.nio.charset.Charset.forName("UTF-8");
    public static final java.nio.charset.Charset ascii = java.nio.charset.Charset.forName("US-ASCII");
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

    static byte[] readall(InputStream in) throws IOException {
	byte[] buf = new byte[4096];
	int off = 0;
	while(true) {
	    if(off == buf.length) {
		byte[] n = new byte[buf.length * 2];
		System.arraycopy(buf, 0, n, 0, buf.length);
		buf = n;
	    }
	    int ret = in.read(buf, off, buf.length - off);
	    if(ret < 0) {
		byte[] n = new byte[off];
		System.arraycopy(buf, 0, n, 0, off);
		return(n);
	    }
	    off += ret;
	}
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
    
    public static int hex2int(char digit) {
	if((digit >= '0') && (digit <= '9'))
	    return(digit - '0');
	if((digit >= 'a') && (digit <= 'f'))
	    return(digit - 'a' + 10);
	if((digit >= 'A') && (digit <= 'F'))
	    return(digit - 'A' + 10);
	throw(new NumberFormatException("Invalid hex digit " + digit));
    }
    
    public static char int2hex(int nibble, boolean upper) {
	if((nibble >= 0) && (nibble <= 9))
	    return((char)('0' + nibble));
	if((nibble >= 10) && (nibble <= 15))
	    return((char)((upper?'A':'a') + nibble - 10));
	throw(new NumberFormatException("Invalid hex nibble " + nibble));
    }

    public static String htmlq(String in) {
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < in.length(); i++) {
	    char c = in.charAt(i);
	    if(c == '&')
		buf.append("&amp;");
	    else if(c == '<')
		buf.append("&lt;");
	    else if(c == '>')
		buf.append("&gt;");
	    else
		buf.append(c);
	}
	return(buf.toString());
    }
    
    public static String urlq(String in) {
	byte[] bytes = in.getBytes(utf8);
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < bytes.length; i++) {
	    byte b = bytes[i];
	    if((b < 32) || (b == ' ') || (b == '&') || (b == '?') || (b == '/') || (b == '=') || (b == '#') || (b == '%') || (b == '+') || (b >= 128)) {
		buf.append('%');
		buf.append(int2hex((b & 0xf0) >> 4, true));
		buf.append(int2hex(b & 0x0f, true));
	    } else {
		buf.append((char)b);
	    }
	}
	return(buf.toString());
    }
    
    public static boolean boolval(String val) {
	val = val.trim().toLowerCase();
	if(val.equals("1") || val.equals("on") || val.equals("true") || val.equals("yes") || val.equals("\u22a4"))
	    return(true);
	if(val.equals("0") || val.equals("off") || val.equals("false") || val.equals("no") || val.equals("\u22a5"))
	    return(false);
	throw(new IllegalArgumentException("value not recognized as boolean: " + val));
    }
    
    public static void eatws(PushbackReader in) throws IOException {
	int c;
	do {
	    c = in.read();
	    if(c < 0)
		return;
	} while(Character.isWhitespace(c));
	in.unread(c);
    }
}
