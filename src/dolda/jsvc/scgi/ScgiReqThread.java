package dolda.jsvc.scgi;

import java.io.*;
import java.net.*;
import dolda.jsvc.*;

public class ScgiReqThread extends RequestThread {
    protected final Socket sk;
    
    public ScgiReqThread(Responder root, Request req, ThreadGroup tg, String name, Socket sk) {
	super(root, req, tg, name);
	this.sk = sk;
    }
    
    public void run() {
	try {
	    super.run();
	} finally {
	    try {
		sk.close();
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
    }
}
