package dolda.jsvc.scgi;

import java.io.*;
import java.net.*;
import java.util.*;

public class Environment {
    public final File root;
    public final Properties sysconfig = new Properties();
    private ClassLoader lib = null;
    
    public Environment(File root) {
	this.root = root;
	if(root != null)
	    loadconfig();
    }
    
    private static File defroot() {
	File root = new File(System.getProperty("user.home"), ".jsvc");
	if(root.exists() && root.isDirectory())
	    return(root);
	return(null);
    }

    public Environment() {
	this(defroot());
    }
    
    private void loadconfig() {
	File conf = new File(root, "jsvc.properties");
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
	File lib = new File(root, "lib");
	if(lib.exists() && lib.isDirectory()) {
	    List<URL> jars = new ArrayList<URL>();
	    for(File f : lib.listFiles()) {
		if(f.isDirectory())
		    continue;
		if(!f.canRead())
		    continue;
		String nm = f.getName();
		if((nm.length() < 4) || !nm.substring(nm.length() - 4).equals(".jar"))
		    continue;
		try {
		    jars.add(f.toURI().toURL());
		} catch(MalformedURLException e) {
		    throw(new Error(e));
		}
	    }
	    this.lib = URLClassLoader.newInstance(jars.toArray(new URL[0]), Environment.class.getClassLoader());
	}
    }
    
    public ClassLoader libloader() {
	if(this.lib == null)
	    return(Environment.class.getClassLoader());
	return(this.lib);
    }
    
    public void initvm() {
	if(root == null)
	    return;
	File logging = new File(root, "logging.properties");
	if(logging.exists() && logging.canRead()) {
	    try {
		InputStream in = new FileInputStream(logging);
		try {
		    java.util.logging.LogManager.getLogManager().readConfiguration(in);
		} finally {
		    in.close();
		}
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
    }
}
