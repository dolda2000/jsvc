package dolda.jsvc.util;

import dolda.jsvc.*;
import java.net.*;

public class Redirect extends RequestRestart {
    private URL abs;
    private String rel;
    
    protected Redirect() {
    }

    public Redirect(URL to) {
	this.abs = to;
	this.rel = null;
    }
    
    public Redirect(String to) {
	this.abs = null;
	this.rel = to;
    }
    
    public void respond(Request req) {
	req.status(303);
	req.outheaders().put("Location", target(req).toString());
    }
    
    protected URL target(Request req) {
	if(this.abs != null) {
	    return(this.abs);
	} else {
	    try {
		return(new URL(req.url(), this.rel));
	    } catch(MalformedURLException e) {
		throw(new RuntimeException("Bad relative URL: + " + this.rel, e));
	    }
	}
    }
}
