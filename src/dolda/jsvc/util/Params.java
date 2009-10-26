package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.CharacterCodingException;

public class Params {
    public static class EncodingException extends ClientError {
	public EncodingException(String msg) {
	    super("Invalid parameter encoding", msg);
	}
    }
    
    public static MultiMap<String, String> urlparams(Reader in) throws IOException {
	try {
	    MultiMap<String, String> ret = new WrappedMultiMap<String, String>(new TreeMap<String, Collection<String>>());
	    String st = "key";
	    String key = null; /* Java is stupid. */
	    MixedBuffer buf = new MixedBuffer();
	    while(true) {
		int c = in.read();
		if(st == "key") {
		    if(c == '%') {
			try {
			    int d1 = in.read();
			    int d2 = in.read();
			    buf.append((byte)((Misc.hex2int((char)d1) << 4) | Misc.hex2int((char)d2)));
			} catch(NumberFormatException e) {
			    throw(new EncodingException("Invalid character escape"));
			}
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
			try {
			    int d1 = in.read();
			    int d2 = in.read();
			    buf.append((byte)((Misc.hex2int((char)d1) << 4) | Misc.hex2int((char)d2)));
			} catch(NumberFormatException e) {
			    throw(new EncodingException("Invalid character escape"));
			}
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

    public static MultiMap<String, String> urlparams(String q) {
	try {
	    return(urlparams(new StringReader(q)));
	} catch(IOException e) {
	    /* This will, of course, never ever once happen, but do
	     * you think Javac cares? */
	    throw(new Error(e));
	}
    }

    public static MultiMap<String, String> urlparams(URL url) {
	String q = url.getQuery();
	if(q == null)
	    q = "";
	return(urlparams(q));
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
    
    public static MultiMap<String, String> postparams(Request req) {
	if(req.method() != "POST")
	    return(null);
	String ctype = req.inheaders().get("Content-Type");
	if(ctype == null)
	    return(null);
	ctype = ctype.toLowerCase();
	if(ctype.equals("application/x-www-form-urlencoded")) {
	    byte[] data;
	    try {
		return(urlparams(new InputStreamReader(req.input(), "UTF-8")));
	    } catch(IOException e) {
		return(null);
	    }
	}
	return(null);
    }
    
    public static MultiMap<String, String> stdparams(Request req) {
	MultiMap<String, String> params = Params.urlparams(req);
	if(req.method() == "POST") {
	    MultiMap<String, String> pp = Params.postparams(req);
	    if(pp != null)
		params.putAll(pp);
	}
	return(params);
    }
}
