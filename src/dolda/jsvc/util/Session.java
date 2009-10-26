package dolda.jsvc.util;

import dolda.jsvc.*;
import java.util.*;
import java.security.SecureRandom;

public class Session implements java.io.Serializable {
    private static final Map<String, Session> sessions = new HashMap<String, Session>();
    private static final SecureRandom prng;
    private static long lastclean = 0;
    private final Map<Object, Object> props = new HashMap<Object, Object>();
    private long ctime = System.currentTimeMillis(), atime = ctime, etime = 86400 * 1000;
    private Collection<Listener> ll = new HashSet<Listener>();
    
    static {
	try {
	    prng = SecureRandom.getInstance("SHA1PRNG");
	} catch(java.security.NoSuchAlgorithmException e) {
	    throw(new Error(e));
	}
    }
    
    public static interface Listener {
	public void expire(Session sess);
    }
    
    public void listen(Listener l) {
	synchronized(ll) {
	    ll.add(l);
	}
    }
    
    public Object get(Object key, Object def) {
	synchronized(props) {
	    if(props.containsKey(key))
		return(props.get(key));
	    else
		return(def);
	}
    }
    
    public Object put(Object key, Object val) {
	synchronized(props) {
	    return(props.put(key, val));
	}
    }
    
    private void expire() {
	synchronized(ll) {
	    for(Listener l : ll)
		l.expire(this);
	}
    }
    
    public static int num() {
	synchronized(sessions) {
	    return(sessions.size());
	}
    }

    private static String newid() {
	byte[] rawid = new byte[16];
	prng.nextBytes(rawid);
	StringBuilder buf = new StringBuilder();
	for(byte b : rawid) {
	    buf.append(Misc.int2hex((b & 0xf0) >> 4, false));
	    buf.append(Misc.int2hex(b & 0x0f, false));
	}
	return(buf.toString());
    }

    private static Session create(Request req) {
	Session sess = new Session();
	long etime = 0;
	int ct;
	ct = Integer.parseInt(req.ctx().libconfig("jsvc.session.expire", "0"));
	if(ct > 0)
	    sess.etime = ct;
	ct = Integer.parseInt(req.ctx().sysconfig("jsvc.session.expire", "0"));
	if(ct > 0)
	    sess.etime = ct;
	return(sess);
    }
    
    private static void clean() {
	long now = System.currentTimeMillis();
	synchronized(sessions) {
	    for(Iterator<Session> i = sessions.values().iterator(); i.hasNext();) {
		Session sess = i.next();
		if(now > sess.atime + sess.etime) {
		    i.remove();
		    sess.expire();
		}
	    }
	}
    }

    public static Session get(Request req) {
	long now = System.currentTimeMillis();
	if(now - lastclean > 3600 * 1000) {
	    clean();
	    lastclean = now;
	}
	
	MultiMap<String, Cookie> cookies = Cookie.get(req);
	Cookie sc = cookies.get("jsvc-session");
	Session sess = null;
	synchronized(sessions) {
	    if(sc != null)
		sess = sessions.get(sc.value);
	    if(sess == null) {
		String id = newid();
		sess = create(req);
		sessions.put(id, sess);
		sc = new Cookie("jsvc-session", id);
		sc.expires = new Date(System.currentTimeMillis() + (86400L * 365L * 1000L));
		sc.path = req.ctx().sysconfig("jsvc.session.path", req.rooturl().getPath());
		String pd = req.ctx().sysconfig("jsvc.session.domain", null);
		if(pd != null)
		    sc.domain = pd;
		sc.addto(req);
	    }
	}
	return(sess);
    }
    
    public static Session get() {
	return(get(RequestThread.request()));
    }
}
