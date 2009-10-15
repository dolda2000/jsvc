package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

public class J2eeContext implements ServerContext {
    private final ServletConfig sc;
    private final long ctime;
    private final Properties config;
    
    J2eeContext(ServletConfig sc) {
	this.sc = sc;
	this.ctime = System.currentTimeMillis();
	config = readconfig(sc);
    }
    
    private static void loadprops(Properties props, File pfile) {
	if(!pfile.exists())
	    return;
	try {
	    InputStream in = new FileInputStream(pfile);
	    try {
		props.load(in);
	    } finally {
		in.close();
	    }
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    private static Properties readconfig(ServletConfig sc) {
	/* This only works on Tomcat now, but that's because I've no
	 * idea how other J2EE servers work. I don't even know if they
	 * _have_ any reasonable place to put configuration and
	 * storage. */
	Properties cfg = new Properties();
	String basename = System.getProperty("catalina.base");
	if(basename != null) {
	    File base = new File(basename);
	    cfg.put("jsvc.storage", new File(new File(base, "work"), "jsvc").getPath());
	    File cdir = new File(base, "conf");
	    loadprops(cfg, new File(cdir, "jsvc.properties"));
	}
	return(cfg);
    }
    
    public long starttime() {
	return(ctime);
    }
    
    public Properties config() {
	return(config);
    }
    
    public ServletConfig j2eeconfig() {
	return(sc);
    }
}
