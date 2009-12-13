package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;
import java.text.*;
import java.io.*;

public class Cookie {
    private final static Map<Request, MultiMap<String, Cookie>> cache = new WeakHashMap<Request, MultiMap<String, Cookie>>();
    public final static DateFormat datefmt;
    static {
	datefmt = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH);
	datefmt.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }
    public final String name;
    public String value;
    public Date expires;
    public String domain, path;
    public boolean secure;
    
    public Cookie(String name, String value, Date expires, String domain, String path, boolean secure) {
	if(!Http.istoken(name))
	    throw(new RuntimeException("Invalid cookie name: `" + name + "'"));
	this.name = name;
	this.value = value;
	this.expires = expires;
	this.domain = domain;
	this.path = path;
	this.secure = secure;
    }
    
    public Cookie(String name) {
	this(name, null, null, null, null, false);
    }
    
    public Cookie(String name, String value) {
	this(name, value, null, null, null, false);
    }
    
    public String format() {
	StringBuilder buf = new StringBuilder();
	buf.append(Http.tokenquote(name));
	buf.append('=');
	buf.append(Http.tokenquote(value));
	if(domain != null)
	    buf.append("; Domain=" + Http.tokenquote(domain));
	if(path != null)
	    buf.append("; Path=" + Http.tokenquote(path));
	if(expires != null)
	    buf.append("; Expires=" + Http.tokenquote(datefmt.format(expires)));
	if(secure)
	    buf.append("; Secure");
	return(buf.toString());
    }

    public void addto(Request req) {
	req.outheaders().add("Set-Cookie", format());
    }
    
    public static MultiMap<String, Cookie> parse(Request req) {
	MultiMap<String, Cookie> ret = new WrappedMultiMap<String, Cookie>(new TreeMap<String, Collection<Cookie>>());
	for(String in : req.inheaders().values("Cookie")) {
	    try {
		PushbackReader r = new PushbackReader(new StringReader(in));
		Cookie c = null;
		while(true) {
		    String k = Http.tokenunquote(r);
		    Misc.eatws(r);
		    if((k == null) || (r.read() != '='))
			throw(new Http.EncodingException("Illegal cookie header format"));
		    String v = Http.tokenunquote(r);
		    if(k.equals("$Version")) {
			if(Integer.parseInt(v) != 1)
			    throw(new Http.EncodingException("Unknown cookie format version"));
		    } else if(k.equals("$Path")) {
			if(c != null)
			    c.path = v;
		    } else if(k.equals("$Domain")) {
			if(c != null)
			    c.domain = v;
		    } else {
			c = new Cookie(k, v);
			ret.add(k, c);
		    }
		    Misc.eatws(r);
		    int sep = r.read();
		    if(sep < 0)
			break;
		    if(sep != ';')
			throw(new Http.EncodingException("Illegal cookie header format"));
		}
	    } catch(IOException e) {
		throw(new Error(e));
	    }
	}
	return(ret);
    }
    
    public static MultiMap<String, Cookie> get(Request req) {
	synchronized(cache) {
	    MultiMap<String, Cookie> ret = cache.get(req);
	    if(ret == null) {
		ret = parse(req);
		cache.put(req, ret);
	    }
	    return(ret);
	}
    }

    public String toString() {
	StringBuilder buf = new StringBuilder();
	buf.append("Cookie(");
	buf.append(format());
	buf.append(")");
	return(buf.toString());
    }
}
