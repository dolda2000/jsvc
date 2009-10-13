package dolda.jsvc.test;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import java.io.*;

public class TestResponder extends SimpleWriter {
    public TestResponder() {
	super("plain");
    }
    
    public void respond(Request req, PrintWriter out) {
	out.println(req.url());
	out.println(req.path());
	out.println(req.inheaders());
	out.println(req.ctx().starttime());
	out.println(req.remoteaddr() + "<->" + req.localaddr());
    }
}
