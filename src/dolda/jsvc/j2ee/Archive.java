package dolda.jsvc.j2ee;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;

public class Archive {
    private Properties props = defprops();
    private JarOutputStream zipout = null;
    private final OutputStream realout;

    public Archive(OutputStream out) {
	this.realout = out;
    }

    private void initzip() throws IOException {
	Manifest man = new Manifest();
	man.getMainAttributes().put(new Attributes.Name("Manifest-Version"), "1.0");
	man.getMainAttributes().put(new Attributes.Name("Created-By"), "jsvc");
	JarOutputStream zip = new JarOutputStream(realout, man);
	zip.putNextEntry(new ZipEntry("WEB-INF/"));
	zip.putNextEntry(new ZipEntry("WEB-INF/lib/"));
	this.zipout = zip;
    }

    private ZipOutputStream zip() throws IOException {
	if(zipout == null)
	    initzip();
	return(this.zipout);
    }
    
    public void putprop(String key, String val) {
	props.put(key, val);
    }

    public void loadprops(InputStream in) throws IOException {
	props.load(in);
    }

    public void jarprops(String[] jars, String propres) throws IOException {
	URL[] urls = new URL[jars.length];
	try {
	    for(int i = 0; i < jars.length; i++)
		urls[i] = new URL("file", "", jars[i]);
	} catch(MalformedURLException e) {
	    throw(new Error(e));
	}
	ClassLoader cl = new URLClassLoader(urls);
	InputStream in = cl.getResourceAsStream(propres);
	if(in != null) {
	    try {
		props.load(in);
	    } finally {
		in.close();
	    }
	}
    }

    private static Properties defprops() {
	Properties props = new Properties();
	props.put("jsvc.j2ee.webxml.coding", "UTF-8");
	return(props);
    }

    private static void cpstream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        while(true) {
	    int ret = in.read(buf, 0, buf.length);
            if(ret < 0)
                return;
	    out.write(buf, 0, ret);
        }
    }

    private static String subst(String ln, Properties props) {
	int p = 0;
	while((p = ln.indexOf("${", p)) >= 0) {
	    int p2 = ln.indexOf('}', p + 2);
	    String pn = ln.substring(p + 2, p2);
	    String pv = (String)props.get(pn);
	    if(pv == null)
		throw(new RuntimeException("Missing required property " + pn));
	    ln = ln.substring(0, p) + pv + ln.substring(p2 + 1);
	    p = p + pv.length();
	}
	return(ln);
    }

    private void writewebxml() throws IOException {
	zip().putNextEntry(new ZipEntry("WEB-INF/web.xml"));
	InputStream tmpl = Archive.class.getResourceAsStream("web.xml.template");
	String cs = (String)props.get("jsvc.j2ee.webxml.coding");
	try {
	    BufferedReader r = new BufferedReader(new InputStreamReader(tmpl, "US-ASCII"));
	    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(zip(), cs));
	    String ln;
	    while((ln = r.readLine()) != null) {
		w.write(subst(ln, props));
		w.write('\n');
	    }
	    w.flush();
	} finally {
	    tmpl.close();
	}
    }
    
    public void addcode(String name, InputStream in) throws IOException {
	zip().putNextEntry(new ZipEntry("WEB-INF/classes/" + name));
	cpstream(in, zip());
    }

    private static String basename(String fn) {
	int p = fn.lastIndexOf('/');
	if(p >= 0)
	    return(fn.substring(p + 1));
	return(fn);
    }

    public void addjars(String[] jars) throws IOException {
	jarprops(jars, "jsvc.properties");
	ZipOutputStream zip = zip();
	for(String jar : jars) {
	    zip.putNextEntry(new ZipEntry("WEB-INF/lib/" + basename(jar)));
	    InputStream jarin = new FileInputStream(jar);
	    try {
		cpstream(jarin, zip);
	    } finally {
		jarin.close();
	    }
	}
    }

    public void finish() throws IOException {
	zip().finish();
    }

    private static void usage(PrintStream out) {
	out.println("usage: dolda.jsvc.j2ee.Archive [-h] [-p PROPFILE] [-n DISPLAY-NAME] [(-c CODE-FILE)...] WAR-FILE JAR-FILE...");
    }
    
    public static void main(String[] args) throws IOException {
	PosixArgs opt = PosixArgs.getopt(args, "hp:n:c:");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	if(opt.rest.length < 2) {
	    usage(System.err);
	    System.exit(1);
	}
	String war = opt.rest[0];
	String[] jars = Arrays.copyOfRange(opt.rest, 1, opt.rest.length);
	
	OutputStream out = new FileOutputStream(war);
	try {
	    Archive ar = new Archive(out);
	
	    for(char c : opt.parsed()) {
		switch(c) {
		case 'p':
		    {
			InputStream in = new FileInputStream(opt.arg);
			try {
			    ar.loadprops(in);
			} finally {
			    in.close();
			}
		    }
		    break;
		case 'n':
		    ar.putprop("jsvc.j2ee.appname", opt.arg);
		    break;
		case 'c':
		    {
			InputStream in = new FileInputStream(opt.arg);
			try {
			    ar.addcode(basename(opt.arg), in);
			} finally {
			    in.close();
			}
		    }
		    break;
		case 'h':
		    usage(System.out);
		    return;
		}
	    }
	    
	    ar.addjars(jars);
	    ar.writewebxml();
	    
	    ar.finish();
	} finally {
	    out.close();
	}
    }
}
