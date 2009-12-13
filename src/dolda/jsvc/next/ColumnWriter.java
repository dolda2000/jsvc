package dolda.jsvc.next;

import java.io.*;

public class ColumnWriter extends FilterWriter {
    public int line, col;
    public boolean start = true;
    
    public ColumnWriter(Writer out) {
	super(out);
	line = col = 0;
    }
    
    private void hc(int c) {
	if(c == '\n') {
	    col = 0;
	    line++;
	    start = true;
	} else if(c == '\t') {
	    col = (col - (col % 8)) + 8;
	} else {
	    col++;
	}
	if(!Character.isWhitespace(c))
	    start = false;
    }

    public void write(int c) throws IOException {
	super.write(c);
	hc(c);
    }
    
    public void write(String s, int off, int len) throws IOException {
	super.write(s, off, len);
	for(int i = 0; i < s.length(); i++)
	    hc(s.charAt(i));
    }
    
    public void write(char[] b, int off, int len) throws IOException {
	super.write(b, off, len);
	for(int i = 0; i < len; i++, off++)
	    hc(b[off]);
    }
    
    public void indent(int level) throws IOException {
	if(!start)
	    write('\n');
	while(col < level)
	    write(' ');
    }
}
