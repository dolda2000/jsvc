package dolda.jsvc;

import java.util.*;

public class ContextParam<T> {
    private boolean bound;
    private T value;
    private final Object id = new Object();
    private Map<ThreadContext, T> perctx = new WeakHashMap<ThreadContext, T>();
    private Map<Thread, T> perthr = new WeakHashMap<Thread, T>();
	
    public ContextParam(T def) {
	this.value = def;
	this.bound = true;
    }
	
    public ContextParam() {
	this.bound = false;
    }
	
    public synchronized T get() {
	Thread th = Thread.currentThread();
	if(perthr.containsKey(th))
	    return(perthr.get(th));
	ThreadContext ctx = ThreadContext.current();
	if(perctx.containsKey(ctx))
	    return(perctx.get(ctx));
	if(!bound)
	    throw(new IllegalStateException("No value is bound to this parameter."));
	return(value);
    }
	
    public synchronized T ctxset(T val) {
	ThreadContext ctx = ThreadContext.current();
	return(perctx.put(ctx, val));
    }
    
    public static Responder let(final Responder next, Object... params) {
	final Map<ContextParam, Object> values = new HashMap<ContextParam, Object>();
	if((params.length % 2) != 0)
	    throw(new IllegalArgumentException("SvcConfig.let takes only an even number of parameters"));
	for(int i = 0; i < params.length; i += 2)
	    values.put((ContextParam)params[i], params[i + 1]);
	
	return(new Responder() {
		/* This can very well actually be set to something
		 * of the wrong type, but since the result would,
		 * obviously, be a ClassCastException either way,
		 * this way is at least the more convenient. */
		@SuppressWarnings("unchecked")
		public void respond(Request req) {
		    final Map<ContextParam, Object> old = new HashMap<ContextParam, Object>();
		    Thread th = Thread.currentThread();
		    for(Map.Entry<ContextParam, Object> val : values.entrySet()) {
			ContextParam p = val.getKey();
			synchronized(p) {
			    if(p.perthr.containsKey(th))
				old.put(p, p.perthr.get(th));
			    p.perthr.put(th, val.getValue());
			}
		    }
		    try {
			next.respond(req);
		    } finally {
			for(Map.Entry<ContextParam, Object> val : values.entrySet()) {
			    ContextParam p = val.getKey();
			    synchronized(p) {
				if(old.containsKey(p)) {
				    p.perthr.put(th, old.get(p));
				} else {
				    p.perthr.remove(th);
				}
			    }
			}
		    }
		}
	    });
    }
}
