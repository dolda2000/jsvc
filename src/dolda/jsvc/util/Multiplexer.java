package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;

public class Multiplexer implements Responder {
    private Responder def;
    private Collection<Sub> subs = new LinkedList<Sub>();

    private static interface Sub {
	boolean match(Request req);
    }
    
    public Multiplexer(Responder def) {
	this.def = def;
    }
    
    public Multiplexer() {
	this(new SimpleWriter("html") {
		public void respond(Request req, java.io.PrintWriter out) {
		    req.status(404);
		    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		    out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
		    out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-US\">");
		    out.println("<head><title>Resource not found</title></head>");
		    out.println("<body>");
		    out.println("<h1>Resource not found</h1>");
		    out.println("The resource you requested could not be found on this server.");
		    out.println("</body>");
		    out.println("</html>");
		}
	    });
    }
    
    public void file(final String path, final Responder responder) {
	subs.add(new Sub() {
		public boolean match(Request req) {
		    if(req.path().equals(path)) {
			responder.respond(req);
			return(true);
		    }
		    return(false);
		}
	    });
    }

    public void dir(String path, final Responder responder) {
	final String fp = Misc.stripslashes(path, true, true);
	subs.add(new Sub() {
		public boolean match(Request req) {
		    if(req.path().equals(fp)) {
			throw(Restarts.redirect(fp + "/"));
		    } else if(req.path().startsWith(fp + "/")) {
			responder.respond(RequestWrap.chpath(req, req.path().substring(fp.length() + 1)));
			return(true);
		    }
		    return(false);
		}
	    });
    }
    
    public void respond(Request req) {
	for(Sub s : subs) {
	    if(s.match(req))
		return;
	}
	def.respond(req);
    }
}
