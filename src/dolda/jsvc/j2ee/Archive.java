package dolda.jsvc.j2ee;

import dolda.jsvc.util.*;
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

    public void jarprops(File[] jars, String propres) throws IOException {
	URL[] urls = new URL[jars.length];
	try {
	    for(int i = 0; i < jars.length; i++)
		urls[i] = new URL("file", "", jars[i].toString());
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

    private static class MissingPropException extends RuntimeException {
	public final String prop;
	
	private MissingPropException(String prop) {
	    super("Missing required property " + prop);
	    this.prop = prop;
	}
    }

    private static String subst(String ln, Properties props) {
	int p = 0;
	while((p = ln.indexOf("${", p)) >= 0) {
	    int p2 = ln.indexOf('}', p + 2);
	    String pn = ln.substring(p + 2, p2);
	    String pv = (String)props.get(pn);
	    if(pv == null)
		throw(new MissingPropException(pn));
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
	Misc.cpstream(in, zip());
    }

    public void addjars(File[] jars) throws IOException {
	jarprops(jars, "jsvc.properties");
	ZipOutputStream zip = zip();
	for(File jar : jars) {
	    zip.putNextEntry(new ZipEntry("WEB-INF/lib/" + jar.getName()));
	    InputStream jarin = new FileInputStream(jar);
	    try {
		Misc.cpstream(jarin, zip);
	    } finally {
		jarin.close();
	    }
	}
    }

    public void finish() throws IOException {
	zip().finish();
    }
    
    public static class AntTask extends org.apache.tools.ant.Task {
	private org.apache.tools.ant.types.FileSet jars, code;
	private File props, outfile;
	private String appname;
	
	private static File[] getfiles(org.apache.tools.ant.types.FileSet fs) {
	    org.apache.tools.ant.DirectoryScanner ds = fs.getDirectoryScanner();
	    ds.scan();
	    String[] nms = ds.getIncludedFiles();
	    File[] ret = new File[nms.length];
	    for(int i = 0; i < nms.length; i++)
		ret[i] = new File(ds.getBasedir(), nms[i]);
	    return(ret);
	}

	private void rebuild(File[] jars, File[] code) throws IOException {
	    OutputStream out = new FileOutputStream(outfile);
	    
	    System.out.println("Building " + outfile);
	    
	    try {
		Archive ar = new Archive(out);
		if(appname != null)
		    ar.putprop("jsvc.j2ee.appname", appname);
		if(props != null) {
		    InputStream in = new FileInputStream(props);
		    try {
			ar.loadprops(in);
		    } finally {
			in.close();
		    }
		}
		
		for(File f : code) {
		    InputStream in = new FileInputStream(f);
		    try {
			ar.addcode(f.getName(), in);
		    } finally {
			in.close();
		    }
		}
		
		ar.addjars(jars);
		ar.writewebxml();
		
		ar.finish();
	    } catch(MissingPropException e) {
		throw(new org.apache.tools.ant.BuildException(e.getMessage(), e));
	    } finally {
		out.close();
	    }
	}
	
	public void execute() {
	    File[] jars = (this.jars != null)?getfiles(this.jars):new File[0];
	    File[] code = (this.code != null)?getfiles(this.code):new File[0];
	    if(jars.length < 1)
		throw(new org.apache.tools.ant.BuildException("Must have at least one JAR file", getLocation()));
	    if(outfile == null)
		throw(new org.apache.tools.ant.BuildException("No output file specified", getLocation()));
	    
	    Collection<File> deps = new LinkedList<File>();
	    deps.addAll(Arrays.asList(jars));
	    deps.addAll(Arrays.asList(code));
	    if(props != null)
		deps.add(props);
	    
	    boolean rebuild = false;
	    for(File dep : deps) {
		if(!dep.exists())
		    throw(new org.apache.tools.ant.BuildException(dep + " does not exist", getLocation()));
		if(dep.lastModified() > outfile.lastModified()) {
		    rebuild = true;
		    break;
		}
	    }
	    
	    if(rebuild) {
		try {
		    rebuild(jars, code);
		} catch(IOException e) {
		    throw(new org.apache.tools.ant.BuildException(e));
		}
	    }
	}

	public void addJars(org.apache.tools.ant.types.FileSet path) {
	    this.jars = path;
	}
	
	public void setJars(org.apache.tools.ant.types.FileSet path) {
	    this.jars = path;
	}
	
	public void setCode(org.apache.tools.ant.types.FileSet path) {
	    this.code = path;
	}
	
	public void setPropfile(File propfile) {
	    this.props = propfile;
	}
	
	public void setDestfile(File outfile) {
	    this.outfile = outfile;
	}
	
	public void setAppname(String name) {
	    this.appname = name;
	}
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
	File[] jars = new File[opt.rest.length - 1];
	for(int i = 1; i < opt.rest.length; i++)
	    jars[i - 1] = new File(opt.rest[i]);
	
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
			File f = new File(opt.arg);
			InputStream in = new FileInputStream(f);
			try {
			    ar.addcode(f.getName(), in);
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
