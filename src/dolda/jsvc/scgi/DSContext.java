package dolda.jsvc.scgi;

import java.io.*;
import java.net.*;
import dolda.jsvc.*;
import dolda.jsvc.util.*;

public class DSContext extends SimpleContext {
    public final long mtime;
    public final ThreadContext tg;
    private final Environment env;

    private static String mangle(File f) {
	String ret = f.getName();
	int p = ret.lastIndexOf('.');
	if(p > 0)
	    ret = ret.substring(0, p);
	for(f = f.getParentFile(); f != null; f = f.getParentFile())
	    ret = f.getName() + "/" + ret;
	return(ret);
    }

    private static URL makingmewanttokilljavac(File jar) {
	try {
	    return(jar.toURI().toURL());
	} catch(MalformedURLException e) {
	    throw(new RuntimeException(e));
	}
    }

    public DSContext(File jar, Environment env) throws ThreadContext.CreateException {
	super(URLClassLoader.newInstance(new URL[] {makingmewanttokilljavac(jar)}, env.libloader()), mangle(jar));
	this.mtime = jar.lastModified();
	this.env = env;
	loadconfig();
	this.tg = ThreadContext.create(this, loader);
    }
    
    private void loadconfig() {
	sysconfig.putAll(env.sysconfig);
    }
    
    public RequestThread worker(Responder root, Request req, ThreadGroup tg, String name) {
	java.net.Socket sk = ((ScgiRequest)req).sk;
	if(req.path().equals("")) {
	    return(new ScgiReqThread(new RootRedirect(""), req, tg, name, sk));
	} else {
	    return(new ScgiReqThread(root, RequestWrap.chpath(req, req.path().substring(1)), tg, name, sk));
	}
    }
}
