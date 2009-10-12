package dolda.jsvc.util;

import dolda.jsvc.*;

public class Rehandler implements Responder {
    private Responder next;

    public Rehandler(Responder next) {
	this.next = next;
    }

    public void respond(Request req) {
	Responder cur = next;
	while(true) {
	    try {
		cur.respond(req);
	    } catch(RequestRestart t) {
		if(req instanceof ResettableRequest) {
		    ResettableRequest rr = (ResettableRequest)req;
		    if(rr.canreset())
			rr.reset();
		}
		cur = t;
		continue;
	    }
	    return;
	}
    }
}
