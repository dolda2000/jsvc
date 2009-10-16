package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import javax.servlet.*;
import java.util.*;
import java.io.*;

public abstract class J2eeContext implements ServerContext {
    private final ServletConfig sc;
    private final long ctime;
    protected final Properties sysconfig, libconfig;
    
    protected J2eeContext(ServletConfig sc) {
	this.sc = sc;
	this.ctime = System.currentTimeMillis();
	sysconfig = new Properties();
	libconfig = new Properties();
    }
    
    static J2eeContext create(ServletConfig sc) {
	if(TomcatContext.tomcatp(sc))
	    return(new TomcatContext(sc));
	return(new StandardContext(sc));
    }
    
    public long starttime() {
	return(ctime);
    }
    
    public String sysconfig(String key, String def) {
	return(sysconfig.getProperty(key, def));
    }
    
    public String libconfig(String key, String def) {
	return(libconfig.getProperty(key, def));
    }
    
    void loadconfig(InputStream in) throws IOException {
	libconfig.load(in);
    }
    
    public ServletConfig j2eeconfig() {
	return(sc);
    }
}
