package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import java.io.*;
import javax.servlet.http.*;

public class Servlet extends HttpServlet {
    private Responder root;
    
    public void init() {
	
    }
    
    public void service(HttpServletRequest req, HttpServletResponse resp) {
	try {
	    req.setCharacterEncoding("UTF-8");
	    resp.setCharacterEncoding("UTF-8");
	} catch(UnsupportedEncodingException e) {
	    throw(new Error(e));
	}
	Request rr = new J2eeRequest(getServletConfig(), req, resp);
	root.respond(rr);
    }
}
