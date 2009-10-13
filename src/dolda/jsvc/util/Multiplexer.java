package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;

public class Multiplexer implements Responder {
    private Responder def;
    private Collection<Matcher> matchers = new LinkedList<Matcher>();

    public static interface Matcher {
	public boolean match(Request req);
    }
    
    public Multiplexer(Responder def) {
	this.def = def;
    }
    
    public Multiplexer() {
	this(new Responder() {
		public void respond(Request req) {
		    throw(Restarts.stdresponse(404, "Resource not found", "The resource you requested could not be found on this server."));
		}
	    });
    }
    
    public void file(final String path, final Responder responder) {
	add(new Matcher() {
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
	add(new Matcher() {
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
    
    public void add(Matcher m) {
	matchers.add(m);
    }
    
    public void respond(Request req) {
	for(Matcher m : matchers) {
	    if(m.match(req))
		return;
	}
	def.respond(req);
    }
}
