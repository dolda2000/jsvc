package dolda.jsvc;

import java.util.*;

public class SvcConfig {
    public static class Param<T> {
	private T value;
	private final Object id = new Object();
	
	public Param(T def) {
	    this.value = def;
	}
	
	@SuppressWarnings("unchecked")
	public T get() {
	    if(Thread.currentThread() instanceof RequestThread) {
		Map<Object, Object> props = RequestThread.request().props();
		if(props.containsKey(id)) {
		    /* This can very well actually be set to something
		     * of the wrong type, but since the result would,
		     * obviously, be a ClassCastException either way,
		     * this way is at least the more convenient. */
		    return((T)props.get(id));
		}
	    }
	    return(value);
	}
    }
    
    public static Responder let(final Responder next, Object... params) {
	final Map<Param, Object> values = new HashMap<Param, Object>();
	if((params.length % 2) != 0)
	    throw(new IllegalArgumentException("SvcConfig.let takes only an even number of parameters"));
	for(int i = 0; i < params.length; i += 2)
	    values.put((Param)params[i], params[i + 1]);
	return(new Responder() {
		public void respond(Request req) {
		    final Map<Param, Object> old = new HashMap<Param, Object>();
		    {
			Map<Object, Object> props = req.props();
			for(Map.Entry<Param, Object> val : values.entrySet()) {
			    Param p = val.getKey();
			    if(props.containsKey(p.id))
				old.put(p, props.get(p.id));
			    props.put(p.id, val.getValue());
			}
		    }
		    try {
			next.respond(req);
		    } finally {
			Map<Object, Object> props = req.props();
			for(Map.Entry<Param, Object> val : values.entrySet()) {
			    Param p = val.getKey();
			    if(old.containsKey(p)) {
				props.put(p.id, old.get(p));
			    } else {
				props.remove(p.id);
			    }
			}
		    }
		}
	    });
    }
}
