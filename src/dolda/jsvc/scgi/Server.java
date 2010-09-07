package dolda.jsvc.scgi;

import java.util.logging.*;
import java.io.*;
import java.net.*;
import java.util.*;

public abstract class Server implements Runnable {
    private final ServerSocket sk;
    private final Logger logger = Logger.getLogger("dolda.jsvc.scgi");
    public String headcs = "UTF-8";
    
    public Server(ServerSocket sk) {
	this.sk = sk;
    }
    
    private static int readnslen(InputStream in) throws IOException {
	int ret = 0;
	while(true) {
	    int c = in.read();
	    if(c == ':')
		return(ret);
	    else if((c >= '0') && (c <= '9'))
		ret = (ret * 10) + (c - '0');
	    else
		throw(new InvalidRequestException("Malformed netstring length"));
	}
    }
    
    private static byte[] readns(InputStream in) throws IOException {
	byte[] buf = new byte[readnslen(in)];
	int off = 0;
	while(off < buf.length) {
	    int ret = in.read(buf, off, buf.length - off);
	    if(ret < 0)
		throw(new InvalidRequestException("Unexpected EOS in netstring"));
	    off += ret;
	}
	if(in.read() != ',')
	    throw(new InvalidRequestException("Unterminated netstring"));
	return(buf);
    }

    private Map<String, String> readhead(InputStream in) throws IOException {
	byte[] rawhead = readns(in);
	String head = new String(rawhead, headcs);
	Map<String, String> ret = new HashMap<String, String>();
	int p = 0;
	while(true) {
	    int p2 = head.indexOf(0, p);
	    if(p2 < 0) {
		if(p == head.length())
		    return(ret);
		throw(new InvalidRequestException("Malformed headers"));
	    }
	    String key = head.substring(p, p2);
	    int p3 = head.indexOf(0, p2 + 1);
	    if(p3 < 0)
		throw(new InvalidRequestException("Malformed headers"));
	    String val = head.substring(p2 + 1, p3);
	    ret.put(key, val);
	    p = p3 + 1;
	}
    }

    private boolean checkhead(Map<String, String> head) {
	if(!head.containsKey("SCGI") || !head.get("SCGI").equals("1"))
	    return(false);
	return(true);
    }

    protected abstract void handle(Map<String, String> head, Socket sk) throws Exception;

    private void serve(Socket sk) {
	try {
	    try {
		InputStream in = sk.getInputStream();
		Map<String, String> head = readhead(in);
		if(!checkhead(head))
		    return;
		try {
		    handle(head, sk);
		} catch(Exception e) {
		    logger.log(Level.WARNING, "Could not handle request", e);
		    return;
		}
		sk = null;
	    } finally {
		if(sk != null)
		    sk.close();
	    }
	} catch(IOException e) {
	    logger.log(Level.WARNING, "I/O error encountered while serving SCGI request", e);
	}
    }

    public void run() {
	try {
	    try {
		while(true) {
		    Socket nsk = sk.accept();
		    serve(nsk);
		}
	    } finally {
		sk.close();
	    }
	} catch(IOException e) {
	    logger.log(Level.SEVERE, "SCGI server encountered I/O error", e);
	}
    }
}
