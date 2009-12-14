package dolda.jsvc.util;

import dolda.jsvc.*;

public class ClientError extends StdResponse {
    public ClientError(String title, String msg) {
	super(400, title, msg);
    }
    
    public ClientError(String msg) {
	this("Invalid request", msg);
    }
}
