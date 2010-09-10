package dolda.jsvc.util;

import java.io.*;
import java.util.*;
import dolda.jsvc.*;

public class SimpleContext implements ServerContext {
    private final long ctime;
    private final String name;
    public final ClassLoader loader;
    protected final Properties sysconfig, libconfig;
    
    private void loadconfig() {
	try {
	    InputStream pi = loader.getResourceAsStream("jsvc.properties");
	    if(pi != null) {
		try {
		    libconfig.load(pi);
		} finally {
		    pi.close();
		}
	    }
	} catch(IOException e) {
	    throw(new Error(e));
	}
    }

    public SimpleContext(ClassLoader cl, String name) {
	this.ctime = System.currentTimeMillis();
	this.name = name;
	this.loader = cl;
	sysconfig = new Properties();
	libconfig = new Properties();
	
	loadconfig();
    }
    
    public long starttime() {
	return(ctime);
    }
    
    public String name() {
	return(name);
    }

    public String sysconfig(String key, String def) {
	return(sysconfig.getProperty(key, def));
    }
    
    public String libconfig(String key, String def) {
	return(libconfig.getProperty(key, def));
    }
    
    public RequestThread worker(Responder root, Request req, ThreadGroup tg, String name) {
	return(new RequestThread(root, req, tg, name));
    }
}
