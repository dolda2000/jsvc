package dolda.jsvc.store;

import dolda.jsvc.*;
import dolda.jsvc.util.Misc;
import java.io.*;
import java.util.*;

public abstract class Store implements Iterable<File> {
    private static Map<String, Factory> kinds = new TreeMap<String, Factory>();
    private static Map<Package, Store> interned = new WeakHashMap<Package, Store>();
    protected final Package pkg;
    
    protected Store(Package pkg) {
	this.pkg = pkg;
    }
    
    public abstract File get(String name);
    
    public static interface Factory {
	public Store create(String root, Package pkg);
    }

    private static String getstoreroot() {
	ThreadContext ctx = ThreadContext.current();
	if(ctx == null)
	    throw(new RuntimeException("Not running in jsvc context"));
	String bn = ctx.server().config("jsvc.storage");
	if(bn == null)
	    throw(new RuntimeException("No storage root has been configured"));
	return(bn);
    }
    
    public static Store forclass(final Class<?> cl) {
	Package pkg = cl.getPackage();
	Store s;
	synchronized(interned) {
	    s = interned.get(pkg);
	    if(s == null) {
		String root = getstoreroot();
		int p = root.indexOf(':');
		if(p < 0)
		    throw(new RuntimeException("Invalid store specification: " + root));
		String kind = root.substring(0, p);
		root = root.substring(p + 1);
		Factory fac;
		synchronized(kinds) {
		    fac = kinds.get(kind);
		    if(fac == null)
			throw(new RuntimeException("No such store kind: " + kind));
		}
		s = fac.create(root, pkg);
		interned.put(pkg, s);
	    }
	}
	return(s);
    }
    
    public static void register(String kind, Factory fac) {
	synchronized(kinds) {
	    if(!kinds.containsKey(kind))
		kinds.put(kind, fac);
	    else
		throw(new RuntimeException("Store of type " + kind + " already exists"));
	}
    }
    
    static {
	FileStore.register();
    }
}
