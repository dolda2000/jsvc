package dolda.jsvc.scgi;

import java.io.*;
import java.net.*;
import java.util.*;
import dolda.jsvc.*;
import dolda.jsvc.util.*;

public class ScgiRequest extends ResponseBuffer {
    final Socket sk;
    private final Map<String, String> environ;
    private final InputStream in;
    private final String method, path;
    private final URL url, context;
    private MultiMap<String, String> params = null;
    private MultiMap<String, String> inhead = new HeaderTreeMap();
    
    public ScgiRequest(Socket sk, Map<String, String> environ) throws IOException {
	this.sk = sk;
	this.environ = environ;
	for(Map.Entry<String, String> var : environ.entrySet()) {
	    String k = var.getKey();
	    if((k.length() > 5) && k.substring(0, 5).equals("HTTP_")) {
		StringBuilder buf = new StringBuilder();
		boolean f = true;
		for(int i = 5; i < k.length(); i++) {
		    char c = k.charAt(i);
		    if(c == '_') {
			buf.append('-');
			f = true;
		    } else if(f) {
			buf.append(Character.toUpperCase(c));
			f = false;
		    } else {
			buf.append(Character.toLowerCase(c));
		    }
		}
		inhead.add(buf.toString(), var.getValue());
	    }
	}
	long len;
	{
	    String h = environ.get("CONTENT_LENGTH");
	    if(h == null) {
		len = 0;
	    } else {
		try {
		    len = Long.parseLong(h);
		} catch(NumberFormatException e) {
		    throw(new InvalidRequestException("Invalid Content-Length header: " + h));
		}
	    }
	}
	this.in = new LimitInputStream(sk.getInputStream(), len);
	path = environ.get("PATH_INFO");
	if(path == null)
	    throw(new InvalidRequestException("Missing PATH_INFO"));
	{
	    String tmp = environ.get("REQUEST_METHOD");
	    if(tmp == null)
		throw(new InvalidRequestException("Missing REQUEST_METHOD"));
	    method = tmp.toUpperCase().intern();
	}
	{
	    /* Ewwww, this is disgusting! */
	    String scheme = "http";
	    if(environ.get("HTTPS") != null)
		scheme = "https";
	    int port = -1;
	    String host = environ.get("HTTP_HOST");
	    if((host == null) || (host.length() < 1)) {
		if((host = environ.get("SERVER_NAME")) == null)
		    throw(new InvalidRequestException("Both HTTP_HOST and SERVER name are missing"));
		String portnum = environ.get("SERVER_PORT");
		if(portnum == null)
		    throw(new InvalidRequestException("Missing SERVER_PORT"));
		try {
		    port = Integer.parseInt(portnum);
		} catch(NumberFormatException e) {
		    throw(new InvalidRequestException("Bad SERVER_PORT: " + portnum));
		}
		if((port == 80) && scheme.equals("http"))
		    port = -1;
		else if((port == 443) && scheme.equals("https"))
		    port = -1;
	    } else {
		int p;
		if((host.charAt(0) == '[') && ((p = host.indexOf(']', 1)) > 1)) {
		    String newhost = host.substring(1, p);
		    if((p = host.indexOf(':', p + 1)) >= 0) {
			try {
			    port = Integer.parseInt(host.substring(p + 1));
			} catch(NumberFormatException e) {}
		    }
		    host = newhost;
		} else if((p = host.indexOf(':')) >= 0) {
		    try {
			port = Integer.parseInt(host.substring(p + 1));
			host = host.substring(0, p);
		    } catch(NumberFormatException e) {}
		}
	    }
	    String nm = environ.get("SCRIPT_NAME");
	    if(nm == null)
		throw(new InvalidRequestException("Missing SCRIPT_NAME"));
	    String q = environ.get("QUERY_STRING");
	    if(q != null)
		q = "?" + q;
	    else
		q = "";
	    try {
		url = new URL(scheme, host, port, nm + path + q);
		if(nm.charAt(nm.length() - 1) != '/')
		    nm += "/";	        /* XXX? */
		context = new URL(scheme, host, port, nm);
	    } catch(MalformedURLException e) {
		throw(new Error(e));
	    }
	}
    }
    
    public MultiMap<String, String> inheaders() {
	return(inhead);
    }

    public ServerContext ctx() {
	return(ThreadContext.current().server());
    }
    
    public InputStream input() {
	return(in);
    }
    
    public URL url() {
	return(url);
    }
    
    public URL rooturl() {
	return(context);
    }
    
    public String path() {
	return(path);
    }
    
    public String method() {
	return(method);
    }
    
    public MultiMap<String, String> params() {
	if(params == null)
	    params = Params.stdparams(this);
	return(params);
    }

    public SocketAddress localaddr() {
	String portnum = environ.get("SERVER_PORT");
	int port = -1;
	try {
	    if(portnum != null)
		port = Integer.parseInt(portnum);
	} catch(NumberFormatException e) {}
	if(port < 0)
	    return(null);	/* XXX? */
	String addr;
	addr = environ.get("X_ASH_SERVER_ADDRESS");
	if(addr == null)
	    return(new InetSocketAddress(port)); /* XXX? */
	else
	    return(new InetSocketAddress(addr, port));
    }

    public SocketAddress remoteaddr() {
	String addr;
	String portnum;
	addr = environ.get("REMOTE_ADDR");
	portnum = environ.get("X_ASH_PORT");
	int port = -1;
	try {
	    if(portnum != null)
		port = Integer.parseInt(portnum);
	} catch(NumberFormatException e) {}
	if((addr != null) && (port >= 0))
	    return(new InetSocketAddress(addr, port));
	return(null);		        /* XXX? */
    }
    
    private void checkstring(String s) {
	for(int i = 0; i < s.length(); i++) {
	    char c = s.charAt(i);
	    if((c < 32) || (c >= 128))
		throw(new RuntimeException("Invalid header string: " + s));
	}
    }

    protected void backflush() throws IOException {
	Writer out = new OutputStreamWriter(realoutput(), Misc.ascii);
	out.write(String.format("Status: %d %s\n", respcode, resptext));
	for(Map.Entry<String, String> e : outheaders().entrySet()) {
	    String k = e.getKey();
	    String v = e.getValue();
	    checkstring(k);
	    checkstring(v);
	    out.write(String.format("%s: %s\n", k, v));
	}
	out.write("\n");
	out.flush();
    }
    
    protected OutputStream realoutput() {
	try {
	    return(sk.getOutputStream());
	} catch(IOException e) {
	    /* It is not obvious why this would happen, so I'll wait
	     * until I know whatever might happen to try and implement
	     * meaningful behavior. */
	    throw(new RuntimeException(e));
	}
    }
}
