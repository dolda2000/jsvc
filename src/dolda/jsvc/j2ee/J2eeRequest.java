package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class J2eeRequest extends ResponseBuffer {
    private ServletConfig cfg;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private String method, path;
    private URL url;
    private Map<?, ?> props = new HashMap();
    
    public J2eeRequest(ServletConfig cfg, HttpServletRequest req, HttpServletResponse resp) {
	this.cfg = cfg;
	this.req = req;
	this.resp = resp;
	{
	    String host = req.getHeader("Host");
	    if((host == null) || (host.length() < 1))
		host = req.getLocalAddr();
	    String pi = req.getPathInfo();
	    if(pi == null)
		pi = "";
	    String q = req.getQueryString();
	    if(q != null)
		q = "?" + q;
	    else
		q = "";
	    try {
		url = new URL(req.getScheme(), host, req.getServerPort(), req.getContextPath() + req.getServletPath() + pi + q);
	    } catch(MalformedURLException e) {
		throw(new Error(e));
	    }
	}
	method = req.getMethod().toUpperCase().intern();
	path = req.getPathInfo();
	while((path.length() > 0) && (path.charAt(0) == '/'))
	    path = path.substring(1);
    }
    
    public Map<?, ?> props() {
	return(props);
    }
    
    public URL url() {
	return(url);
    }
    
    public String method() {
	return(method);
    }
    
    public String path() {
	return(path);
    }

    public InputStream input() {
	try {
	    return(req.getInputStream());
	} catch(IOException e) {
	    /* It is not obvious why this would happen, so I'll wait
	     * until I know whatever might happen to try and implement
	     * meaningful behavior. */
	    throw(new RuntimeException(e));
	}
    }

    public MultiMap<String, String> inheaders() {
	MultiMap<String, String> h = new HeaderTreeMap();
	Enumeration ki = req.getHeaderNames();
	if(ki != null) {
	    while(ki.hasMoreElements()) {
		String k = (String)ki.nextElement();
		Enumeration vi = req.getHeaders(k);
		if(vi != null) {
		    while(vi.hasMoreElements()) {
			String v = (String)vi.nextElement();
			h.add(k, v);
		    }
		}
	    }
	}
	return(h);
    }
    
    public MultiMap<String, String> params() {
	return(null);
    }
    
    protected void backflush() {
	for(String key : outheaders().keySet()) {
	    boolean first = true;
	    for(String val : outheaders().values(key)) {
		if(first) {
		    resp.setHeader(key, val);
		    first = false;
		} else {
		    resp.addHeader(key, val);
		}
	    }
	}
    }
    
    protected OutputStream realoutput() {
	try {
	    return(resp.getOutputStream());
	} catch(IOException e) {
	    /* It is not obvious why this would happen, so I'll wait
	     * until I know whatever might happen to try and implement
	     * meaningful behavior. */
	    throw(new RuntimeException(e));
	}
    }
}
