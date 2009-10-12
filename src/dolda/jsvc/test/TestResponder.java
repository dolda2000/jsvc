package dolda.jsvc.test;

import dolda.jsvc.*;
import java.io.*;

public class TestResponder implements Responder {
    public void respond(Request req) {
	req.outheaders().put("Content-Type", "text/html; charset=utf-8");
	PrintWriter out;
	try {
	    out = new PrintWriter(new OutputStreamWriter(req.output(), "UTF-8"));
	} catch(UnsupportedEncodingException e) {
	    throw(new Error(e));
	}
	try {
	    out.println("<html>");
	    out.println("<head><title>Barda</title></head>");
	    out.println("<body>");
	    out.println("<h1>Barda</h1>");
	    out.println("Bardslen.");
	    out.println("</body>");
	    out.println("</html>");
	} finally {
	    out.close();
	}
    }
}
