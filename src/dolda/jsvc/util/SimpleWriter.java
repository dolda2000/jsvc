package dolda.jsvc.util;

import dolda.jsvc.*;
import java.io.*;

public abstract class SimpleWriter implements Responder {
    private String ctype;
    
    public SimpleWriter(String ctype) {
	this.ctype = ctype;
    }
    
    public SimpleWriter() {
	this("html");
    }
    
    public abstract void respond(Request req, PrintWriter out);
    
    public void respond(Request req) {
	req.outheaders().put("Content-Type", "text/" + ctype + "; charset=utf-8");
	PrintWriter out;
	try {
	    out = new PrintWriter(new OutputStreamWriter(req.output(), "UTF-8"));
	} catch(UnsupportedEncodingException e) {
	    throw(new Error(e));
	}
	respond(req, out);
	out.flush();
    }
}
