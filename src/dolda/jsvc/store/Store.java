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
    
    private Store(Package pkg, File root) {
	this.pkg = pkg;
	String nm = pkg.getName();
	File base = root;
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
		s = new Store(pkg, root);
		interned.put(pkg, s);
	    }
	}
	return(s);
    }
}
