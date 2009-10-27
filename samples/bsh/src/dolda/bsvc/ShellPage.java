package dolda.bsvc;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import java.io.*;
import bsh.Interpreter;

public class ShellPage extends SimpleWriter {
    private Console cons = new Console();
    private Interpreter ip = new Interpreter(cons);
    
    private static class Console implements bsh.ConsoleInterface {
	ByteArrayOutputStream obuf = new ByteArrayOutputStream();
	ByteArrayOutputStream ebuf = new ByteArrayOutputStream();
	Reader in = new StringReader("");
	PrintStream out;
	PrintStream err;
	{
	    try {
		out = new PrintStream(obuf, false, "UTF-8");
		err = new PrintStream(ebuf, false, "UTF-8");
	    } catch(UnsupportedEncodingException e) {
		throw(new Error(e));
	    }
	}
	
	public void error(Object msg) {
	    getErr().println(msg);
	}
	
	public void print(Object o) {
	    getOut().print(o);
	}
	
	public void println(Object o) {
	    getOut().println(o);
	}
	
	public PrintStream getOut() {
	    return(out);
	}

	public PrintStream getErr() {
	    return(err);
	}

	public Reader getIn() {
	    return(in);
	}
	
	public void reset() {
	    obuf.reset();
	    ebuf.reset();
	}
    }
    
    public void respond(Request req, PrintWriter out) {
	MultiMap<String, String> params = req.params();
	String cmd = params.get("cmd");
	
	out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
	out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-US\">");
	out.println("<head>");
	out.println("<title>Shell</title>");
	out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"css\" />");
	out.println("</head>");
	out.println("<body>");
	out.println("<h1>Shell</h1>");
	if((req.method() == "POST") && (cmd != null)) {
	    String eo, ee;
	    synchronized(cons) {
		cons.reset();
		Object resp;
		try {
		    ip.set("req", req);
		    resp = ip.eval(cmd);
		    out.println("<pre>");
		    out.println(Misc.htmlq((resp == null)?"(null)":(resp.toString())));
		    out.println("</pre>");
		} catch(bsh.EvalError exc) {
		    out.println("<h2>Evaluation error</h2>");
		    out.println("<pre>");
		    out.print(exc.toString());
		    out.println("</pre>");
		    if(exc instanceof bsh.TargetError) {
			bsh.TargetError te = (bsh.TargetError)exc;
			out.println("<h3>Target error</h3>");
			out.println("<pre>");
			te.getTarget().printStackTrace(out);
			out.println("</pre>");
		    }
		}
		eo = new String(cons.obuf.toByteArray(), Misc.utf8);
		ee = new String(cons.ebuf.toByteArray(), Misc.utf8);
	    }
	    if(eo.length() > 0) {
		out.println("<h2>Output</h2>");
		out.println("<pre>");
		out.println(Misc.htmlq(eo));
		out.println("</pre>");
	    }
	    if(ee.length() > 0) {
		out.println("<h2>Errors</h2>");
		out.println("<pre>");
		out.println(Misc.htmlq(ee));
		out.println("</pre>");
	    }
	}
	out.println("<form action=\"sh\" method=\"post\">");
	out.println("<textarea cols=\"80\" rows=\"5\" name=\"cmd\">");
	if(cmd != null)
	    out.print(cmd);
	out.println("</textarea>");
	out.println("<input type=\"submit\" value=\"Evaluate\" />");
	out.println("<input type=\"reset\" value=\"Reset\" />");
	out.println("</form>");
	out.println("</body>");
	out.println("</html>");
    }
}
