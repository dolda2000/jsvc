package dolda.jsvc;

public abstract class RequestRestart extends RuntimeException implements Responder {
    public RequestRestart() {
	super("Unhandled restart for code that should be running inside a Rehandler");
    }
    
    public RequestRestart(String msg) {
	super(msg);
    }
    
    public RequestRestart(Throwable t) {
	super("Unhandled restart for code that should be running inside a Rehandler", t);
    }
    
    public RequestRestart(String msg, Throwable t) {
	super(msg, t);
    }
    
    public abstract void respond(Request req);
}
