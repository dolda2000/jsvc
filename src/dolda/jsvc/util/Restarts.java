package dolda.jsvc.util;

import dolda.jsvc.*;
import java.net.*;

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
}
