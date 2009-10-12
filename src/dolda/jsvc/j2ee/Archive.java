package dolda.jsvc.j2ee;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;

public class Archive {
    private static void usage(PrintStream out) {
	out.println("usage: dolda.jsvc.j2ee.Archive [-h] [-p PROPFILE] [-n DISPLAY-NAME] WAR-FILE JAR-FILE...");
    }
    
    private static void jarprops(String[] jars, String propres, Properties props) throws IOException {
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

    private static void writewebxml(Properties props, OutputStream out) throws IOException {
	InputStream tmpl = Archive.class.getResourceAsStream("web.xml.template");
	String cs = (String)props.get("jsvc.j2ee.webxml.coding");
	try {
	    BufferedReader r = new BufferedReader(new InputStreamReader(tmpl, "US-ASCII"));
	    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out, cs));
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
    
    public static void makewar(String[] jars, Properties props, OutputStream out) throws IOException {
	Manifest man = new Manifest();
	man.getMainAttributes().put(new Attributes.Name("Manifest-Version"), "1.0");
	man.getMainAttributes().put(new Attributes.Name("Created-By"), "jsvc");
	JarOutputStream zip = new JarOutputStream(out, man);
	zip.putNextEntry(new ZipEntry("WEB-INF/"));
	zip.putNextEntry(new ZipEntry("WEB-INF/lib/"));
	for(String jar : jars) {
	    String bn = jar;
	    int p = bn.lastIndexOf('/');
	    if(p >= 0)
		bn = bn.substring(p + 1);
	    zip.putNextEntry(new ZipEntry("WEB-INF/lib/" + bn));
	    InputStream jarin = new FileInputStream(jar);
	    try {
		cpstream(jarin, zip);
	    } finally {
		jarin.close();
	    }
	}
	zip.putNextEntry(new ZipEntry("WEB-INF/web.xml"));
	writewebxml(props, zip);
	zip.finish();
    }

    public static void main(String[] args) throws IOException {
	PosixArgs opt = PosixArgs.getopt(args, "hp:n:");
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
	
	Properties props = defprops();
	jarprops(jars, "/jsvc.properties", props);
	
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'p':
		{
		    InputStream in = new FileInputStream(opt.arg);
		    try {
			props.load(in);
		    } finally {
			in.close();
		    }
		}
		break;
	    case 'n':
		props.put("jsvc.j2ee.appname", opt.arg);
		break;
	    case 'h':
		usage(System.out);
		return;
	    }
	}
	
	OutputStream out = new FileOutputStream(war);
	try {
	    makewar(jars, props, out);
	} finally {
	    out.close();
	}
    }
}
