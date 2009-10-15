package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import javax.servlet.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;

public class TomcatContext extends J2eeContext {
    private final String name;
    
    TomcatContext(ServletConfig sc) {
	super(sc);
	ServletContext ctx = j2eeconfig().getServletContext();
	Class<?> cclass = ctx.getClass();
	try {
	    Method cpm = cclass.getMethod("getContextPath");
	    name = Misc.stripslashes((String)cpm.invoke(ctx), true, true);
	} catch(NoSuchMethodException e) {
	    throw(new RuntimeException("Could not fetch context path from Tomcat", e));
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException("Could not fetch context path from Tomcat", e));
	} catch(InvocationTargetException e) {
	    throw(new RuntimeException("Could not fetch context path from Tomcat", e));
	}
	readconfig();
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

    private void readconfig() {
	String basename = System.getProperty("catalina.base");
	File base = new File(basename);
	config.put("jsvc.storage", "file:" + new File(new File(base, "work"), "jsvc").getPath());
	File cdir = new File(base, "conf");
	loadprops(config, new File(cdir, "jsvc.properties"));
    }
    
    public static boolean tomcatp(ServletConfig sc) {
	ServletContext ctx = sc.getServletContext();
	if(ctx.getClass().getName().equals("org.apache.catalina.core.ApplicationContextFacade"))
	    return(true);
	return(false);
    }
    
    public String name() {
	return(name);
    }
}
