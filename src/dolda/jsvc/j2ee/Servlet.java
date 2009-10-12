package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import javax.servlet.http.*;
import javax.servlet.*;

public class Servlet extends HttpServlet {
    private Responder root;
    private ThreadGroup workers;
    private long reqs = 0;

    public void init() throws ServletException {
	workers = new ThreadGroup("JSvc worker threads") {
		public void uncaughtException(Thread t, Throwable e) {
		    log("Worker thread terminated with an uncaught exception", e);
		}
	    };
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
	try {
	    Class<?> rc = Class.forName(clnm);
	    Method cm = rc.getMethod("responder");
	    Object resp = cm.invoke(null);
	    if(!(resp instanceof Responder))
		throw(new ServletException("JSvc bootstrapper did not return a responder"));
	    root = (Responder)resp;
	} catch(ClassNotFoundException e) {
	    throw(new ServletException("Invalid JSvc bootstrapper specified", e));
	} catch(NoSuchMethodException e) {
	    throw(new ServletException("Invalid JSvc bootstrapper specified", e));
	} catch(IllegalAccessException e) {
	    throw(new ServletException("Invalid JSvc bootstrapper specified", e));
	} catch(InvocationTargetException e) {
	    throw(new ServletException("JSvc bootstrapper failed", e));
	}
	ServletContext ctx = getServletContext();
	ctx.setAttribute("jsvc.starttime", System.currentTimeMillis());
    }
    
    public void destroy() {
	workers.interrupt();
	if(root instanceof ContextResponder)
	    ((ContextResponder)root).destroy();
    }
    
    public void service(HttpServletRequest req, HttpServletResponse resp) {
	try {
	    req.setCharacterEncoding("UTF-8");
	    resp.setCharacterEncoding("UTF-8");
	} catch(UnsupportedEncodingException e) {
	    throw(new Error(e));
	}
	long mynum = reqs++;
	Request rr = new J2eeRequest(getServletConfig(), req, resp);
	RequestThread w = new RequestThread(root, rr, workers, "Worker thread " + mynum);
	w.start();
	try {
	    w.join();
	} catch(InterruptedException e) {
	    w.interrupt();
	    return;
	}
    }
}
