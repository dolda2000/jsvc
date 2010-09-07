package dolda.jsvc.scgi;

import java.io.*;
import java.net.*;
import java.util.*;
import dolda.jsvc.*;
import dolda.jsvc.util.*;
import dolda.jsvc.j2ee.PosixArgs;

public class DirServer extends Server {
    private final Map<File, DSContext> contexts = new HashMap<File, DSContext>();
    private final File datroot;
    
    public DirServer(ServerSocket sk, File datroot) {
	super(sk);
	this.datroot = datroot;
    }

    private DSContext context(File file) throws ThreadContext.CreateException {
	synchronized(contexts) {
	    DSContext ctx = contexts.get(file);
	    if(ctx != null) {
		if(ctx.mtime < file.lastModified()) {
		    ctx.tg.destroy();
		    contexts.remove(file);
		    ctx = null;
		}
	    }
	    if(ctx == null) {
		ctx = new DSContext(file, datroot);
		contexts.put(file, ctx);
	    }
	    return(ctx);
	}
    }

    public void handle(Map<String, String> head, Socket sk) throws Exception {
	String filename = head.get("SCRIPT_FILENAME");
	if(filename == null)
	    throw(new Exception("Request for DirServer must contain SCRIPT_FILENAME"));
	File file = new File(filename);
	if(!file.exists() || !file.canRead())
	    throw(new Exception("Cannot access the requested JSvc file " + file.toString()));
	DSContext ctx = context(file);
	Request req = new ScgiRequest(sk, head);
	RequestThread w = ctx.tg.respond(req);
	w.start();
    }

    private static void usage(PrintStream out) {
	out.println("usage: dolda.jsvc.scgi.DirServer [-h] [-e CHARSET] [-d DATADIR] PORT");
    }
    
    public static void main(String[] args) {
	PosixArgs opt = PosixArgs.getopt(args, "h");
	if(opt == null) {
	    usage(System.err);
	    System.exit(1);
	}
	String charset = null;
	File datroot = null;
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'e':
		charset = opt.arg;
		break;
	    case 'd':
		datroot = new File(opt.arg);
		if(!datroot.exists() || !datroot.isDirectory()) {
		    System.err.println(opt.arg + ": no such directory");
		    System.exit(1);
		}
		break;
	    case 'h':
		usage(System.out);
		return;
	    }
	}
	if(opt.rest.length < 1) {
	    usage(System.err);
	    System.exit(1);
	}
	if(datroot == null) {
	    datroot = new File(System.getProperty("user.home"), ".jsvc");
	    if(!datroot.exists() || !datroot.isDirectory())
		datroot = null;
	}
	int port = Integer.parseInt(opt.rest[0]);
	ServerSocket sk;
	try {
	    sk = new ServerSocket(port);
	} catch(IOException e) {
	    System.err.println("could not bind to port " + port + ": " + e.getMessage());
	    System.exit(1);
	    return; /* Because javac is stupid. :-/ */
	}
	DirServer s = new DirServer(sk, datroot);
	if(charset != null)
	    s.headcs = charset;
	
	new Thread(s, "SCGI server thread").start();
    }
}
