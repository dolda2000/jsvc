package dolda.bsvc;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import dolda.jsvc.next.*;
import org.w3c.dom.*;
import java.io.*;
import bsh.Interpreter;

public class ShellPage implements Responder {
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
    
    public void respond(Request req) {
	MultiMap<String, String> params = req.params();
	String cmd = params.get("cmd");
	
	Html buf = Html.xhtml11("Shell");
	buf.addcss("css", null);
	buf.insert("body", buf.el("h1", buf.text("Shell")));
	
	if((req.method() == "POST") && (cmd != null)) {
	    String eo, ee;
	    synchronized(cons) {
		cons.reset();
		Object resp;
		try {
		    ip.set("req", req);
		    resp = ip.eval(cmd);
		    buf.insert("body", buf.el("pre", buf.text((resp == null)?"(null)":(resp.toString()))));
		} catch(bsh.EvalError exc) {
		    buf.insert("body", buf.el("h2", buf.text("Evaluation error")));
		    buf.insert("body", buf.el("pre", buf.text(exc.toString())));
		    if(exc instanceof bsh.TargetError) {
			bsh.TargetError te = (bsh.TargetError)exc;
			buf.insert("body", buf.el("h3", buf.text("Target error")));
			StringWriter sbuf = new StringWriter();
			te.getTarget().printStackTrace(new PrintWriter(sbuf));
			buf.insert("body", buf.el("pre", buf.text(sbuf.toString())));
		    }
		}
		eo = new String(cons.obuf.toByteArray(), Misc.utf8);
		ee = new String(cons.ebuf.toByteArray(), Misc.utf8);
	    }
	    if(eo.length() > 0) {
		buf.insert("body", buf.el("h2", buf.text("Output")));
		buf.insert("body", buf.el("pre", buf.text(eo)));
	    }
	    if(ee.length() > 0) {
		buf.insert("body", buf.el("h2", buf.text("Errors")));
		buf.insert("body", buf.el("pre", buf.text(ee)));
	    }
	}
	
	Element form;
	buf.insert("body", buf.el("form", form = buf.el("p", null), "action=sh", "method=post"));
	form.appendChild(buf.el("textarea", buf.text(cmd), "cols=80", "rows=5", "name=cmd"));
	form.appendChild(buf.el("input", null, "type=submit", "value=Evaluate"));
	form.appendChild(buf.el("input", null, "type=reset", "value=Reset"));
	try {
	    buf.output(req);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }
}
