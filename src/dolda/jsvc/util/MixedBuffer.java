package dolda.jsvc.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class MixedBuffer {
    private ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private Writer conv;
    private Charset cs;
	
    public MixedBuffer(Charset cs) {
	this.cs = cs;
	conv = new OutputStreamWriter(buf, cs);
    }
    
    public MixedBuffer() {
	this(Misc.utf8);
    }
    
    public void append(byte b) {
	buf.write(b);
    }
    
    public void append(char c) {
	try {
	    conv.write(c);
	    conv.flush();
	} catch(IOException e) {
	    throw(new Error(e));
	}
    }
    
    public String convert() throws java.nio.charset.CharacterCodingException {
	CharsetDecoder dec = cs.newDecoder();
	ByteBuffer in = ByteBuffer.wrap(buf.toByteArray());
	CharBuffer out = dec.decode(in);
	return(out.toString());
    }
    
    public int size() {
	return(buf.size());
    }
}
