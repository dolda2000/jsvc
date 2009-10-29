package dolda.jsvc.util;

import dolda.jsvc.*;
import java.lang.reflect.*;

public class PerSession implements Responder {
    private final Class<?> rcl;
    private final Class<?> dcl;
    
    public PerSession(Class<?> rcl, Class<?> dcl) {
	this.rcl = rcl;
	this.dcl = dcl;
    }
    
    public PerSession(Class<?> rcl) {
	this(rcl, null);
    }
    
    private Object makedata(Session sess) {
	try {
	    try {
		return(dcl.getConstructor(Session.class).newInstance(sess));
	    } catch(NoSuchMethodException e) {
	    }
	    try {
		return(dcl.getConstructor().newInstance());
	    } catch(NoSuchMethodException e) {
	    }
	} catch(InstantiationException e) {
	    throw(new RuntimeException(e));
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException(e));
	} catch(InvocationTargetException e) {
	    throw(new RuntimeException(e));
	}
	throw(new RuntimeException("Found no way to create an instance of " + dcl.getName()));
    }

    private Object getdata(Session sess) {
	Object d = sess.get(dcl, null);
	if(d == null) {
	    d = makedata(sess);
	    sess.put(dcl, d);
	}
	return(d);
    }

    private Responder create(Session sess) {
	try {
	    if(dcl != null) {
		try {
		    return((Responder)rcl.getMethod("responder", dcl).invoke(null, getdata(sess)));
		} catch(NoSuchMethodException e) {
		}
	    }
	    try {
		return((Responder)rcl.getMethod("responder", Session.class).invoke(null, sess));
	    } catch(NoSuchMethodException e) {
	    }
	    try {
		return((Responder)rcl.getMethod("responder").invoke(null));
	    } catch(NoSuchMethodException e) {
	    }
	    if(dcl != null) {
		try {
		    return((Responder)rcl.getConstructor(dcl).newInstance(getdata(sess)));
		} catch(NoSuchMethodException e) {
		}
	    }
	    try {
		return((Responder)rcl.getConstructor(Session.class).newInstance(sess));
	    } catch(NoSuchMethodException e) {
	    }
	    try {
		return((Responder)rcl.getConstructor().newInstance());
	    } catch(NoSuchMethodException e) {
	    }
	} catch(InstantiationException e) {
	    throw(new RuntimeException(e));
	} catch(IllegalAccessException e) {
	    throw(new RuntimeException(e));
	} catch(InvocationTargetException e) {
	    throw(new RuntimeException(e));
	}
	throw(new RuntimeException("Found no way to create a responder from the class " + rcl.getName()));
    }

    public void respond(Request req) {
	Session sess = Session.get(req);
	Responder resp;
	synchronized(this) {
	    resp = (Responder)sess.get(rcl, null);
	    if(resp == null) {
		resp = create(sess);
		sess.put(rcl, resp);
		if(resp instanceof ContextResponder) {
		    final ContextResponder cr = (ContextResponder)resp;
		    sess.listen(new Session.Listener() {
			    public void destroy(Session sess) {
				cr.destroy();
			    }
			});
		}
	    }
	}
	resp.respond(req);
    }
}
