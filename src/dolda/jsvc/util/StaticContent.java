package dolda.jsvc.util;

import dolda.jsvc.*;
import java.io.*;
import java.util.*;

public class StaticContent implements Responder {
    private final Class<?> base;
    private final String resname;
    private final boolean dir;
    private final String mimetype;
    
    public StaticContent(Class<?> base, String resname, String mimetype) {
	this.base = base;
	this.dir = ((this.resname = resname).charAt(resname.length() - 1) == '/');
	this.mimetype = mimetype;
    }
    
    public StaticContent(String resname, String mimetype) {
	this(null, resname, mimetype);
    }
    
    public void respond(Request req) {
	String nm;
	if(dir)
	    nm = resname + req.path();
	else
	    nm = resname;
	InputStream in;
	if(base == null) {
	    in = StaticContent.class.getClassLoader().getResourceAsStream(nm);
	} else {
	    in = base.getResourceAsStream(nm);
	}
	if(in == null)
	    throw(Restarts.stdresponse(404));
	Cache.checkmtime(req, req.ctx().starttime());
	try {
	    try {
		req.outheaders().put("Content-Type", mimetype);
		Misc.cpstream(in, req.output());
	    } finally {
		in.close();
	    }
	} catch(IOException e) {
	    throw(new Error(e));
	}
    }
}
