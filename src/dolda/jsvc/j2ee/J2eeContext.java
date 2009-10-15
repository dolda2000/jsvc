package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import javax.servlet.*;
import java.util.*;
import java.io.*;

public abstract class J2eeContext implements ServerContext {
    private final ServletConfig sc;
    private final long ctime;
    protected final Properties config;
    
    protected J2eeContext(ServletConfig sc) {
	this.sc = sc;
	this.ctime = System.currentTimeMillis();
	config = new Properties();
    }
    
    static J2eeContext create(ServletConfig sc) {
	if(TomcatContext.tomcatp(sc))
	    return(new TomcatContext(sc));
	return(new StandardContext(sc));
    }
    
    public long starttime() {
	return(ctime);
    }
    
    public String config(String key) {
	return((String)config.get(key));
    }
    
    public ServletConfig j2eeconfig() {
	return(sc);
    }
}
