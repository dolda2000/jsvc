package dolda.bsvc;

import java.io.*;

public class Console implements bsh.ConsoleInterface {
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
