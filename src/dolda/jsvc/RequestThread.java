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
	try {
	    req.output().close();
	} catch(java.io.IOException e) {
	    throw(new RuntimeException(e));
	}
    }
    
    public static ServerContext context() {
	return(((RequestThread)Thread.currentThread()).req.ctx());
    }
    
    public static Request request() {
	return(((RequestThread)Thread.currentThread()).req);
    }
}
