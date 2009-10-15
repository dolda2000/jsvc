package dolda.jsvc.store;

import dolda.jsvc.*;
import dolda.jsvc.util.Misc;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

public class Store {
    private static Map<Package, Store> interned = new WeakHashMap<Package, Store>();
    private final Package pkg;
    private final File base;
    
    private Store(Package pkg, CodeSource src, File root) {
	this.pkg = pkg;
	String nm = pkg.getName();
	File base = root;
	if(src != null) {
	    try {
		MessageDigest fdig = MessageDigest.getInstance("MD5");
		for(Certificate cert : src.getCertificates()) {
		    MessageDigest cdig = MessageDigest.getInstance("MD5");
		    cdig.update(cert.getEncoded());
		    fdig.update(cdig.digest());
		}
		byte[] fp = fdig.digest();
		StringBuilder buf = new StringBuilder();
		for(byte b : fp) {
		    buf.append(Misc.int2hex((b & 0xf0) >> 4, true));
		    buf.append(Misc.int2hex(b & 0x0f, true));
		}
		base = new File(base, buf.toString());
	    } catch(NoSuchAlgorithmException e) {
		throw(new Error(e));
	    } catch(java.security.cert.CertificateEncodingException e) {
		throw(new Error(e));
	    }
	}
	int p = 0;
	int p2;
	while((p2 = nm.indexOf('.', p)) >= 0) {
	    base = new File(base, nm.substring(p, p2));
	    p = p2 + 1;
	}
	this.base = new File(base, nm.substring(p));
    }
    
    private static File getstoreroot() {
	ThreadContext ctx = ThreadContext.current();
	if(ctx == null)
	    throw(new RuntimeException("Not running in jsvc context"));
	String bn = ctx.server().config("jsvc.storage");
	if(bn == null)
	    throw(new RuntimeException("No storage root has been configured"));
	return(new File(bn));
    }

    public static Store forclass(final Class<?> cl) {
	Package pkg = cl.getPackage();
	File root = getstoreroot();
	Store s;
	synchronized(interned) {
	    s = interned.get(pkg);
	    if(s == null) {
		ProtectionDomain dom;
		dom = AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {
			public ProtectionDomain run() {
			    try {
				return(cl.getProtectionDomain());
			    } catch(SecurityException e) {
				return(null);
			    }
			}
		    });
		if(dom != null)
		    s = new Store(pkg, dom.getCodeSource(), root);
		else
		    s = new Store(pkg, null, root);
		interned.put(pkg, s);
	    }
	}
	return(s);
    }
}
