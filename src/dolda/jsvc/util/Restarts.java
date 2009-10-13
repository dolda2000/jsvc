package dolda.jsvc.util;

import dolda.jsvc.*;
import java.net.*;
import java.io.*;

public class Restarts {
    public static RequestRestart redirect(final URL to) {
	return(new RequestRestart() {
		public void respond(Request req) {
		    req.status(303);
		    req.outheaders().put("Location", to.toString());
		}
	    });
    }

    public static RequestRestart redirect(final String path) {
	return(new RequestRestart() {
		public void respond(Request req) {
		    req.status(303);
		    URL url;
		    try {
			url = new URL(req.url(), path);
		    } catch(MalformedURLException e) {
			throw(new RuntimeException("Bad relative URL: + " + path, e));
		    }
		    req.outheaders().put("Location", url.toString());
		}
	    });
    }

    public static RequestRestart redirectctx(final String path) {
	return(new RequestRestart() {
		public void respond(Request req) {
		    req.status(303);
		    URL url;
		    String rel = req.ctx().rootpath() + "/" + Misc.stripslashes(path, true, false);
		    try {
			url = new URL(req.url(), rel);
		    } catch(MalformedURLException e) {
			throw(new RuntimeException("Bad relative URL: + " + rel, e));
		    }
		    req.outheaders().put("Location", url.toString());
		}
	    });
    }
    
    public static RequestRestart stdresponse(final int code, final String title, final String message) {
	return(new RequestRestart() {
		public void respond(Request req) {
		    req.status(code);
		    req.outheaders().put("content-type", "text/html; charset=us-ascii");
		    PrintWriter out;
		    try {
			out = new PrintWriter(new OutputStreamWriter(req.output(), "US-ASCII"));
		    } catch(UnsupportedEncodingException e) {
			throw(new Error(e));
		    }
		    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		    out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
		    out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-US\">");
		    out.println("<head><title>" + title + "</title></head>");
		    out.println("<body>");
		    out.println("<h1>" + title + "</h1>");
		    out.println(message);
		    out.println("</body>");
		    out.println("</html>");
		    out.flush();
		}
	    });
    }
    
    public static RequestRestart stdresponse(int code, String message) {
	return(stdresponse(code, "An error occurred", message));
    }

    public static RequestRestart stdresponse(int code) {
	return(stdresponse(code, "An error occurred", Misc.statustext(code)));
    }
}
