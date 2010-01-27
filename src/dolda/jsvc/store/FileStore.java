package dolda.jsvc.store;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import java.io.*;
import java.util.*;
import java.security.*;

class FileStore extends Store {
    private final java.io.File base;
    private static final int smbuflimit;
    private static int txserial = 0;
    
    static {
	int res; /* Java is stupid, as usual... */
	try {
	    String p = System.getProperty("dolda.jsvc.store.smallbuf");
	    if(p != null)
		res = Integer.parseInt(p);
	    else
		res = 65536;
	} catch(SecurityException e) {
	    res = 65536;
	}
	smbuflimit = res;
    }

    private FileStore(Package pkg, java.io.File root) {
	super(pkg);
	String nm = pkg.getName();
	java.io.File base = root;
	int p = 0;
	int p2;
	while((p2 = nm.indexOf('.', p)) >= 0) {
	    base = new java.io.File(base, nm.substring(p, p2));
	    p = p2 + 1;
	}
	this.base = new java.io.File(base, nm.substring(p));
	AccessController.doPrivileged(new PrivilegedAction<Object>() {
		public Object run() {
		    if(!FileStore.this.base.exists()) {
			if(!FileStore.this.base.mkdirs())
			    throw(new RuntimeException("Could not create store directory (Java won't tell me why)"));
		    }
		    return(null);
		}
	    });
    }

    private static String mangle(String in) {
	byte[] bytes = in.getBytes(Misc.utf8);
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < bytes.length; i++) {
	    byte b = bytes[i];
	    if(((b >= '0') && (b <= '9')) || ((b >= 'A') && (b <= 'Z')) || ((b >= 'a') && (b <= 'z'))) {
		buf.append((char)b);
	    } else {
		buf.append('_');
		buf.append(Misc.int2hex((b & 0xf0) >> 4, true));
		buf.append(Misc.int2hex(b & 0x0f, true));
	    }
	}
	return(buf.toString());
    }
    
    private static String demangle(String in) {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	for(int i = 0; i < in.length(); i++) {
	    char c = in.charAt(i);
	    if(c == '_') {
		char d1 = in.charAt(i + 1);
		char d2 = in.charAt(i + 2);
		i += 2;
		buf.write((byte)((Misc.hex2int(d1) << 4) | Misc.hex2int(d2)));
	    } else {
		if(c >= 256)
		    throw(new RuntimeException("Invalid filename in store"));
		buf.write(c);
	    }
	}
	byte[] bytes = buf.toByteArray();
	return(new String(bytes, Misc.utf8));
    }
    
    private class RFile implements File {
	private final java.io.File fs;
	private final String name;
	
	private class TXStream extends OutputStream {
	    private FileOutputStream buf = null;
	    private java.io.File tmpfile;
	    private boolean closed = false;
	    
	    private void init() throws IOException {
		try {
		    buf = AccessController.doPrivileged(new PrivilegedExceptionAction<FileOutputStream>() {
			    public FileOutputStream run() throws IOException {
				synchronized(RFile.class) {
				    int serial = txserial++;
				    tmpfile = new java.io.File(fs.getPath() + ".new." + txserial);
				    if(tmpfile.exists()) {
					if(!tmpfile.delete())
					    throw(new IOException("Could not delete previous temporary file (Java won't tell my why)"));
				    }
				    return(new FileOutputStream(tmpfile));
				}
			    }
			});
		} catch(PrivilegedActionException e) {
		    throw((IOException)e.getCause());
		}
	    }

	    public void write(byte[] b, int off, int len) throws IOException {
		if(closed)
		    throw(new IOException("This file has already been committed"));
		if(buf == null)
		    init();
		buf.write(b, off, len);
	    }

	    public void write(byte[] b) throws IOException {
		if(closed)
		    throw(new IOException("This file has already been committed"));
		if(buf == null)
		    init();
		buf.write(b);
	    }

	    public void write(int b) throws IOException {
		if(closed)
		    throw(new IOException("This file has already been committed"));
		if(buf == null)
		    init();
		buf.write(b);
	    }
	    
	    public void flush() throws IOException {
		if(buf == null)
		    init();
		buf.flush();
	    }
	    
	    public void close() throws IOException {
		flush();
		closed = true;
		buf.close();
		try {
		    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
			    public Object run() throws IOException {
				if(!tmpfile.renameTo(fs)) {
				    fs.delete();
				    if(!tmpfile.renameTo(fs))
					throw(new IOException("Could not replace previous file contents with new (Java won't tell me why)"));
				}
				return(null);
			    }
			});
		} catch(PrivilegedActionException e) {
		    throw((IOException)e.getCause());
		}
	    }
	}

	private RFile(java.io.File fs, String name) {
	    this.fs = fs;
	    this.name = name;
	}
	
	public String name() {
	    return(name);
	}
	
	public InputStream read() {
	    return(AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
			public InputStream run() {
			    try {
				return(new FileInputStream(fs));
			    } catch(FileNotFoundException e) {
				return(null);
			    }
			}
		    }));
	}
	
	public OutputStream store() {
	    return(new TXStream());
	}
	
	public long mtime() {
	    return(AccessController.doPrivileged(new PrivilegedAction<Long>() {
			public Long run() {
			    return(fs.lastModified());
			}
		    }));
	}
	
	public void remove() {
	    AccessController.doPrivileged(new PrivilegedAction<Object>() {
		    public InputStream run() {
			if(!fs.delete())
			    throw(new RuntimeException("Could not delete the file " + fs.getPath() + " (Java won't tell me why)"));
			return(null);
		    }
		});
	}
    }

    public File get(String name) {
	return(new RFile(new java.io.File(base, mangle(name)), name));
    }
    
    public Iterator<File> iterator() {
	final java.io.File[] ls = base.listFiles();
	return(new Iterator<File>() {
		private int i = 0;
		private File cur = null;
		
		public boolean hasNext() {
		    return(i < ls.length);
		}
		
		public File next() {
		    java.io.File f = ls[i++];
		    cur = new RFile(f, demangle(f.getName()));
		    return(cur);
		}
		
		public void remove() {
		    if(cur == null)
			throw(new IllegalStateException());
		    cur.remove();
		    cur = null;
		}
	    });
    }
    
    public static void register() {
	Store.register("file", new Factory() {
		public Store create(String rootname, Package pkg) {
		    return(new FileStore(pkg, new java.io.File(rootname)));
		}
	    });
    }
}
