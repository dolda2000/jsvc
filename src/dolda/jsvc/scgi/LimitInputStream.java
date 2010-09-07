package dolda.jsvc.scgi;

import java.io.*;

public class LimitInputStream extends InputStream {
    private final InputStream bk;
    private final long limit;
    private long read;
    
    public LimitInputStream(InputStream bk, long limit) {
	this.bk = bk;
	this.limit = limit;
    }
    
    public void close() throws IOException {
	bk.close();
    }
    
    public int available() throws IOException {
	int av = bk.available();
	synchronized(this) {
	    if(av > limit - read)
		av = (int)(limit - read);
	    return(av);
	}
    }
    
    public int read() throws IOException {
	synchronized(this) {
	    if(read >= limit)
		return(-1);
	    int ret = bk.read();
	    if(ret >= 0)
		read++;
	    return(ret);
	}
    }
    
    public int read(byte[] b) throws IOException {
	return(read(b, 0, b.length));
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
	synchronized(this) {
	    if(read >= limit)
		return(-1);
	    if(len > limit - read)
		len = (int)(limit - read);
	    int ret = bk.read(b, off, len);
	    if(ret > 0)
		read += ret;
	    return(ret);
	}
    }
    
    public long skip(long n) throws IOException {
	synchronized(this) {
	    if(n > limit - read)
		n = limit - read;
	    long ret = bk.skip(n);
	    read += ret;
	    return(ret);
	}
    }
}
