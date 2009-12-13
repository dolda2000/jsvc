package dolda.jsvc.util;

import java.util.*;
import java.text.*;
import java.io.*;

public class Http {
    public final static DateFormat datefmt;
    public final static String tspecials = "()<>@,;:\\\"/[]?={} ";
    static {
	datefmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
	datefmt.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }
    
    public static class EncodingException extends ClientError {
	public EncodingException(String msg) {
	    super("Invalid header encoding", msg);
	}
    }
    
    public static String fmtdate(Date d) {
	return(datefmt.format(d));
    }
    
    public static Date parsedate(String str) throws ParseException {
	return(datefmt.parse(str));
    }
    
    public static boolean istoken(String str) {
	for(int i = 0; i < str.length(); i++) {
	    char c = str.charAt(i);
	    if(c < 32)
		return(false);
	    if(c >= 127)
		return(false);
	    if(tspecials.indexOf(c) >= 0)
		return(false);
	}
	return(true);
    }
    
    public static String tokenquote(String str) {
	if(istoken(str))
	    return(str);
	StringBuilder buf = new StringBuilder();
	buf.append("\"");
	for(int i = 0; i < str.length(); i++) {
	    char c = str.charAt(i);
	    if(((c < 32) && (c != 9)) || (c >= 127))
		throw(new RuntimeException("Invalid character in HTTP quoted-string: `" + c + "'"));
	    if((c == '"') || (c == '\\')) {
		buf.append('\\');
		buf.append(c);
	    } else {
		buf.append(c);
	    }
	}
	buf.append("\"");
	return(buf.toString());
    }
    
    public static String tokenunquote(PushbackReader in) throws IOException {
	StringBuilder buf = new StringBuilder();
	String st = "eatws";
	int c = in.read();
	while(true) {
	    if(st == "eatws") {
		if(Character.isWhitespace((char)c))
		    c = in.read();
		else
		    st = "token";
	    } else if(st == "token") {
		if(c == '"') {
		    st = "quoted";
		    c = in.read();
		} else if((c < 0) || Character.isWhitespace((char)c) || (tspecials.indexOf((char)c) >= 0)) {
		    if(c >= 0)
			in.unread(c);
		    if(buf.length() == 0)
			return(null);
		    return(buf.toString());
		} else if((c < 32) || (c >= 127)) {
		    throw(new EncodingException("Invalid characters in header"));
		} else {
		    buf.append((char)c);
		    c = in.read();
		}
	    } else if(st == "quoted") {
		if(c < 0) {
		    throw(new EncodingException("Unterminated quoted-string"));
		} else if((c < 32) && !Character.isWhitespace((char)c)) {
		    throw(new EncodingException("Invalid characters in header"));
		} else if(c == '"') {
		    return(buf.toString());
		} else if(c == '\\') {
		    st = "q1";
		    c = in.read();
		} else {
		    buf.append((char)c);
		    c = in.read();
		}
	    } else if(st == "q1") {
		if(c < 0) {
		    throw(new EncodingException("Unterminated quoted-string"));
		} else if(c > 127) {
		    throw(new EncodingException("Invalid characters in header"));
		} else {
		    buf.append((char)c);
		    c = in.read();
		    st = "quoted";
		}
	    }
	}
    }
}
