package dolda.jsvc.util;

import java.io.*;
import java.util.*;
import java.net.*;
import dolda.jsvc.*;

public class JarContext implements ServerContext {
    private final long ctime;
    private final String name;
    public final ClassLoader loader;
    protected final Properties sysconfig, libconfig;
    
    private static String mangle(File f) {
	String ret = f.getName();
	int p = ret.lastIndexOf('.');
	if(p > 0)
	    ret = ret.substring(0, p);
	for(f = f.getParentFile(); f != null; f = f.getParentFile())
	    ret = f.getName() + "/" + ret;
	return(ret);
    }

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

    public Class<?> findboot() {
	String clnm = libconfig("jsvc.bootstrap", null);
	if(clnm == null)
	    return(null);
	Class<?> bc;
	try {
	    bc = loader.loadClass(clnm);
	} catch(ClassNotFoundException e) {
	    return(null);
	}
	return(bc);
    }

    public JarContext(ClassLoader cl, String name) {
	this.ctime = System.currentTimeMillis();
	this.name = name;
	this.loader = cl;
	sysconfig = new Properties();
	libconfig = new Properties();
	
	loadconfig();
    }
    
    private static URL makingmewanttokilljavac(File jar) {
	try {
	    return(jar.toURI().toURL());
	} catch(MalformedURLException e) {
	    throw(new RuntimeException(e));
	}
    }

    public JarContext(File jar) {
	this(new URLClassLoader(new URL[] {makingmewanttokilljavac(jar)}, JarContext.class.getClassLoader()), mangle(jar));
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
