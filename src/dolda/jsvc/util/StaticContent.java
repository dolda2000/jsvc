package dolda.jsvc.util;

import dolda.jsvc.*;
import java.io.*;
import java.util.*;

public class StaticContent implements Responder {
    private final Class<?> base;
    private final String resname;
    private final boolean dir;
    private final String mimetype;
    
    public StaticContent(Class<?> base, String resname, boolean dir, String mimetype) {
	this.base = base;
	this.resname = resname;
	this.dir = dir;
	this.mimetype = mimetype;
    }
    
    public StaticContent(String resname, boolean dir, String mimetype) {
	this(null, resname, dir, mimetype);
    }
    
    public void respond(Request req) {
	String nm;
	if(dir) {
	    nm = resname + "/" + req.path();
	} else {
	    nm = resname;
	}
	InputStream in;
	if(base == null) {
	    in = StaticContent.class.getClassLoader().getResourceAsStream(nm);
	} else {
	    in = base.getResourceAsStream(nm);
	}
	if(in == null)
	    throw(Restarts.stdresponse(404));
	String ims = req.inheaders().get("If-Modified-Since");
	Date mtime = new Date((req.ctx().starttime() / 1000) * 1000);
	if(ims != null) {
	    Date d;
	    try {
		d = Http.parsedate(ims);
	    } catch(java.text.ParseException e) {
		throw(Restarts.stdresponse(400));
	    }
	    if(mtime.compareTo(d) <= 0) {
		req.status(304);
		req.outheaders().put("Content-Length", "0");
		return;
	    }
	}
	try {
	    try {
		req.outheaders().put("Content-Type", mimetype);
		req.outheaders().put("Last-Modified", Http.fmtdate(mtime));
		Misc.cpstream(in, req.output());
	    } finally {
		in.close();
	    }
	} catch(IOException e) {
	    throw(new Error(e));
	}
    }
}
