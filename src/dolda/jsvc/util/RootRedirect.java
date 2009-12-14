package dolda.jsvc.util;

import dolda.jsvc.*;
import java.net.*;

public class RootRedirect extends Redirect {
    private final String to;
    
    public RootRedirect(String to) {
	this.to = to;
    }
    
    protected URL target(Request req) {
	try {
	    return(new URL(req.rooturl(), Misc.stripslashes(to, true, false)));
	} catch(MalformedURLException e) {
	    throw(new RuntimeException("Bad relative URL: + " + to, e));
	}
    }
}
