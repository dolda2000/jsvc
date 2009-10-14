package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.CharacterCodingException;

public class Params {
    public static class EncodingException extends RequestRestart {
	public EncodingException(String msg) {
	    super(msg);
	}
	
	public void respond(Request req) {
	    throw(Restarts.stdresponse(400, "Invalid parameter encoding", getMessage()));
	}
    }
    
    public static MultiMap<String, String> urlparams(String q) {
	try {
	    MultiMap<String, String> ret = new WrappedMultiMap<String, String>(new TreeMap<String, Collection<String>>());
	    String st = "key";
	    String key = null; /* Java is stupid. */
	    MixedBuffer buf = new MixedBuffer();
	    int i = 0;
	    while(true) {
		int c = (i >= q.length())?-1:(q.charAt(i++));
		if(st == "key") {
		    if(c == '%') {
			if(q.length() - i < 2)
			    throw(new EncodingException("Invalid character escape"));
			try {
			    buf.append((byte)((Misc.hex2int(q.charAt(i)) << 4) | Misc.hex2int(q.charAt(i + 1))));
			} catch(NumberFormatException e) {
			    throw(new EncodingException("Invalid character escape"));
			}
			i += 2;
		    } else if(c == '=') {
			key = buf.convert();
			buf = new MixedBuffer();
			st = "val";
		    } else if(c == '&') {
			ret.add(buf.convert(), "");
			buf = new MixedBuffer();
		    } else if(c == -1) {
			if(buf.size() == 0) {
			    break;
			} else {
			    ret.add(buf.convert(), "");
			    buf = new MixedBuffer();
			}
		    } else {
			buf.append((char)c);
		    }
		} else if(st == "val") {
		    if(c == '%') {
			if(q.length() - i < 2)
			    throw(new EncodingException("Invalid character escape"));
			try {
			    buf.append((byte)((Misc.hex2int(q.charAt(i)) << 4) | Misc.hex2int(q.charAt(i + 1))));
			} catch(NumberFormatException e) {
			    throw(new EncodingException("Invalid character escape"));
			}
			i += 2;
		    } else if((c == '&') || (c == -1)) {
			ret.add(key, buf.convert());
			buf = new MixedBuffer();
			st = "key";
		    } else if(c == '+') {
			buf.append(' ');
		    } else {
			buf.append((char)c);
		    }
		}
	    }
	    return(ret);
	} catch(CharacterCodingException e) {
	    throw(new EncodingException("Escaped parameter text is not proper UTF-8"));
	}
    }

    public static MultiMap<String, String> urlparams(URL url) {
	return(urlparams(url.getQuery()));
    }

    public static MultiMap<String, String> urlparams(Request req) {
	return(urlparams(req.url()));
    }
    
    public static String encquery(Map<String, String> pars) {
	StringBuilder buf = new StringBuilder();
	boolean f = true;
	for(Map.Entry<String, String> par : pars.entrySet()) {
	    if(!f)
		buf.append('&');
	    buf.append(Misc.urlq(par.getKey()));
	    buf.append('=');
	    buf.append(Misc.urlq(par.getValue()));
	    f = false;
	}
	return(buf.toString());
    }
}
