package dolda.jsvc.scgi;

import java.io.*;
import dolda.jsvc.*;
import dolda.jsvc.util.*;

public class DSContext extends JarContext {
    public final long mtime;
    private final File datroot;
    public final ThreadContext tg;

    public DSContext(File jar, File datroot) throws ThreadContext.CreateException {
	super(jar);
	this.mtime = jar.lastModified();
	this.datroot = datroot;
	loadconfig();
	this.tg = ThreadContext.create(this, loader);
    }
    
    private void loadconfig() {
	if(datroot != null) {
	    File sroot = new File(new File(datroot, "store"), name());
	    sysconfig.put("jsvc.storage", "file:" + sroot.getPath());
	    File conf = new File(datroot, "jsvc.properties");
	    if(conf.exists()) {
		try {
		    InputStream in = new FileInputStream(conf);
		    try {
			sysconfig.load(in);
		    } finally {
			in.close();
		    }
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }
	}
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
