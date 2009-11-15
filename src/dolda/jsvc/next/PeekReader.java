package dolda.jsvc.next;

import java.io.*;

public class PeekReader extends Reader {
    private final Reader back;
    private boolean p = false;
    private int la;
	
    public PeekReader(Reader back) {
	this.back = back;
    }
	
    public void close() throws IOException {
	back.close();
    }
	
    public int read() throws IOException {
	if(p) {
	    p = false;
	    return(la);
	} else {
	    return(back.read());
	}
    }
	
    public int read(char[] b, int off, int len) throws IOException {
	int r = 0;
	while(r < len) {
	    int c = read();
	    if(c < 0)
		return(r);
	    b[off + r++] = (char)c;
	}
	return(r);
    }
	
    public boolean ready() throws IOException {
	if(p)
	    return(true);
	return(back.ready());
    }
    
    protected boolean whitespace(char c) {
	return(Character.isWhitespace(c));
    }

    public int peek(boolean skipws) throws IOException {
	while(!p || (skipws && (la >= 0) && whitespace((char)la))) {
	    la = back.read();
	    p = true;
	}
	return(la);
    }
    
    public int peek() throws IOException {
	return(peek(false));
    }
}
