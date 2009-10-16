package dolda.jsvc;

import java.io.*;
import java.net.URL;
import java.net.SocketAddress;
import java.util.Map;

public class RequestWrap implements Request {
    private final Request bk;
    
    public RequestWrap(Request req) {
	this.bk = req;
    }
    
    public URL url() {return(bk.url());}
    public URL rooturl() {return(bk.rooturl());}
    public String method() {return(bk.method());}
    public String path() {return(bk.path());}
    public InputStream input() {return(bk.input());}
    public MultiMap<String, String> inheaders() {return(bk.inheaders());}
    public MultiMap<String, String> params() {return(bk.params());}
    public OutputStream output() {return(bk.output());}
    public void status(int code) {bk.status(code);}
    public void status(int code, String message) {bk.status(code, message);}
    public MultiMap<String, String> outheaders() {return(bk.outheaders());}
    public ServerContext ctx() {return(bk.ctx());}
    public SocketAddress remoteaddr() {return(bk.remoteaddr());}
    public SocketAddress localaddr() {return(bk.localaddr());}

    public Request orig() {
	return(bk);
    }
    
    public static Request chpath(Request req, String path) {
	class PathWrap extends RequestWrap {
	    private final String path;
	    
	    public PathWrap(Request req, String path) {
		super(req);
		this.path = path;
	    }
	    
	    public String path() {
		return(path);
	    }
	}
	if(req instanceof PathWrap)
	    return(new PathWrap(((PathWrap)req).orig(), path));
	return(new PathWrap(req, path));
    }
}
