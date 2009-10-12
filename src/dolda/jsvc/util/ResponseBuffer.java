package dolda.jsvc.util;

import dolda.jsvc.*;
import java.io.*;
import java.util.*;

public abstract class ResponseBuffer implements ResettableRequest {
    private boolean flushed = false;
    private int respcode = -1;
    private String resptext = null;
    private OutputStream out = null, wrapout = null;
    private MultiMap<String, String> headers;
    
    public ResponseBuffer() {
	init();
    }

    private void init() {
	ckflush();
	wrapout = null;
	respcode = -1;
	headers = new HeaderTreeMap() {
		protected void modified() {
		    ckflush();
		}
	    };
    }
    
    private void ckflush() {
	if(flushed)
	    throw(new IllegalStateException("Response has been flushed; header information cannot be modified"));
    }
    
    private void flush() {
	if(flushed)
	    return;
	if(respcode < 0) {
	    respcode = 200;
	    resptext = "OK";
	}
	backflush();
	out = realoutput();
	flushed = true;
    }

    private class FlushStream extends OutputStream {
	private FlushStream() {
	}
	
	public void flush() throws IOException {
	    ResponseBuffer.this.flush();
	    out.flush();
	}
	
	public void close() throws IOException {
	    flush();
	    out.close();
	}
	
	public void write(int b) throws IOException {
	    ResponseBuffer.this.flush();
	    out.write(b);
	}
	
	public void write(byte[] b) throws IOException {
	    write(b, 0, b.length);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
	    ResponseBuffer.this.flush();
	    out.write(b, off, len);
	}
    }

    public OutputStream output() {
	if(wrapout == null)
	    wrapout = new BufferedOutputStream(new FlushStream(), 16384);
	return(wrapout);
    }

    public void status(int code) {
	status(code, Misc.statustext(code));
    }
    
    public void status(int code, String text) {
	ckflush();
	respcode = code;
	resptext = text;
    }
    
    public MultiMap<String, String> outheaders() {
	return(headers);
    }
    
    public boolean canreset() {
	return(!flushed);
    }
    
    public void reset() {
	init();
    }
    
    protected abstract void backflush();
    protected abstract OutputStream realoutput();
}
