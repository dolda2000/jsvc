package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import javax.servlet.http.*;
import javax.servlet.*;

public class Servlet extends HttpServlet {
    private ThreadContext tg;

    public void init(ServletConfig cfg) throws ServletException {
	Properties sprop = new Properties();
	try {
	    InputStream pi = Servlet.class.getClassLoader().getResourceAsStream("jsvc.properties");
	    try {
		sprop.load(pi);
	    } finally {
		pi.close();
	    }
	} catch(IOException e) {
	    throw(new Error(e));
	}
	String clnm = (String)sprop.get("jsvc.bootstrap");
	if(clnm == null)
	    throw(new ServletException("No JSvc bootstrapper specified"));
	Class<?> bc;
	try {
	    bc = Class.forName(clnm);
	} catch(ClassNotFoundException e) {
	    throw(new ServletException("Invalid JSvc bootstrapper specified", e));
	}
	tg = new ThreadContext(null, "JSvc service", new J2eeContext(cfg), bc);
    }
    
    public void destroy() {
	tg.shutdown();
    }
    
    public void service(HttpServletRequest req, HttpServletResponse resp) {
	try {
	    req.setCharacterEncoding("UTF-8");
	    resp.setCharacterEncoding("UTF-8");
	} catch(UnsupportedEncodingException e) {
	    throw(new Error(e));
	}
	Request rr = new J2eeRequest(getServletConfig(), req, resp);
	RequestThread w = tg.respond(rr);
	w.start();
	try {
	    w.join();
	} catch(InterruptedException e) {
	    w.interrupt();
	    return;
	}
    }
}
