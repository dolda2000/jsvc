package dolda.jsvc.util;

import dolda.jsvc.*;

public class ClientError extends RequestRestart {
    private final String title;
    
    public ClientError(String title, String msg) {
	super(msg);
	this.title = title;
    }
    
    public ClientError(String msg) {
	this("Invalid request", msg);
    }
    
    public void respond(Request req) {
	throw(Restarts.stdresponse(400, title, getMessage()));
    }
}
