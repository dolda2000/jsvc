package dolda.jsvc;

public class RequestThread extends Thread {
    private Request req;
    private Responder resp;
    
    public RequestThread(Responder resp, Request req, ThreadGroup th, String name) {
	super(th, name);
	this.resp = resp;
	this.req = req;
    }
    
    public void run() {
	resp.respond(req);
    }
    
    public static Request request() {
	return(((RequestThread)Thread.currentThread()).req);
    }
}
