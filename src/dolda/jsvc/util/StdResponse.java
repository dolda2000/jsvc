package dolda.jsvc.util;

import dolda.jsvc.*;
import java.io.*;

public class StdResponse extends RequestRestart {
    private final int code;
    private final String title;
    
    public StdResponse(int code, String title, String message) {
	super(message);
	this.code = code;
	this.title = title;
    }
    
    public StdResponse(int code, String message) {
	this(code, "An error occurred", message);
    }
    
    public StdResponse(int code) {
	this(code, Misc.statustext(code));
    }
    
    public void respond(Request req) {
	req.status(code);
	req.outheaders().put("Content-Type", "text/html; charset=utf-8");
	PrintWriter out;
	out = new PrintWriter(new OutputStreamWriter(req.output(), Misc.utf8));
	out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
	out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-US\">");
	out.println("<head><title>" + title + "</title></head>");
	out.println("<body>");
	out.println("<h1>" + title + "</h1>");
	out.println(getMessage());
	out.println("</body>");
	out.println("</html>");
	out.flush();
    }
}
