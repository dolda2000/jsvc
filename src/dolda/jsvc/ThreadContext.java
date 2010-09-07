package dolda.jsvc;

import dolda.jsvc.util.Misc;
import java.util.logging.*;
import java.lang.reflect.*;
import java.util.*;

public class ThreadContext extends ThreadGroup {
    private Logger logger = Logger.getLogger("dolda.jsvc.context");
    private ThreadGroup workers;
    private long reqs = 0;
    private final ServerContext ctx;
    public final Responder root;
    private int timelimit = 0;
    private boolean forcelimit = false;
    
    public ThreadContext(ThreadGroup parent, String name, ServerContext ctx, Class<?> bootclass) {
	super((parent == null)?(Thread.currentThread().getThreadGroup()):parent, name);
	this.ctx = ctx;
	workers = new ThreadGroup(this, "Worker threads") {
		public void uncaughtException(Thread t, Throwable e) {
		    logger.log(Level.SEVERE, "Worker thread terminated with an uncaught exception", e);
		}
	    };
	
	int tl;
	tl = Integer.parseInt(ctx.sysconfig("jsvc.timelimit", "0"));
	if((tl > 0) && ((timelimit == 0) || (tl < timelimit)))
	    timelimit = tl;
	tl = Integer.parseInt(ctx.libconfig("jsvc.timelimit", "0"));
	if((tl > 0) && ((timelimit == 0) || (tl < timelimit)))
	    timelimit = tl;
	forcelimit |= Misc.boolval(ctx.sysconfig("jsvc.forcelimit", "0"));
	forcelimit |= Misc.boolval(ctx.libconfig("jsvc.forcelimit", "0"));
	
	root = bootstrap(bootclass);
	
	if(timelimit > 0)
	    (new WatchDog()).start();
    }
    
    private class WatchDog extends Thread {
	private Map<RequestThread, State> state = new WeakHashMap<RequestThread, State>();
	
	private class State {
	    String st = "running";
	    long lastkill;
	}
	
	private WatchDog() {
	    super(ThreadContext.this, "Worker watchdog");
	    setDaemon(true);
	}
	
	@SuppressWarnings("deprecation")
	private long ckthread(long now, RequestThread rt) {
	    State st = state.get(rt);
	    if(st == null) {
		st = new State();
		state.put(rt, st);
	    }
	    if(st.st == "running") {
		if(now - rt.stime() > timelimit) {
		    rt.interrupt();
		    st.st = "interrupted";
		    st.lastkill = now;
		    return(5000);
		} else {
		    return(timelimit - (now - rt.stime()));
		}
	    } else if((st.st == "interrupted") || (st.st == "killed")) {
		if(st.st == "killed")
		    logger.log(Level.WARNING, "Thread " + rt + " refused to die; killing again");
		if(now - st.lastkill > 5000) {
		    rt.stop();
		    st.st = "killed";
		    st.lastkill = now;
		} else {
		    return(5000 - (now - st.lastkill));
		}
	    }
	    return(timelimit);
	}

	public void run() {
	    try {
		while(true) {
		    long next = timelimit;
		    long now = System.currentTimeMillis();
		    Thread[] w = new Thread[workers.activeCount() + 5];
		    int num = workers.enumerate(w);
		    for(int i = 0; i < num; i++) {
			if(w[i] instanceof RequestThread){
			    RequestThread rt = (RequestThread)w[i];
			    if(rt.stime() > 0) {
				long n = ckthread(now, rt);
				if(n < next)
				    next = n;
			    }
			}
		    }
		    Thread.sleep(next);
		}
	    } catch(InterruptedException e) {
	    }
	}
    }
    
    public void uncaughtException(Thread t, Throwable e) {
	logger.log(Level.SEVERE, "Service thread " + t.toString() + " terminated with an uncaught exception", e);
    }
    
    public ServerContext server() {
	return(ctx);
    }
    
    public void shutdown() {
	if(root instanceof ContextResponder)
	    ((ContextResponder)root).destroy();
	try {
	    long last = 0;
	    while(true) {
		long now = System.currentTimeMillis();
		if(now - last > 10000) {
		    interrupt();
		    last = now;
		}
		Thread[] th = new Thread[1];
		if(enumerate(th) < 1)
		    break;
		th[0].join(10000);
	    }
	} catch(InterruptedException e) {
	    logger.log(Level.WARNING, "Interrupted while trying to shut down all service threads. Some may remain.", e);
	}
	destroy();
    }
    
    public RequestThread respond(Request req) {
	return(ctx.worker(root, req, workers, "Worker thread " + reqs++));
    }
    
    private Responder bootstrap(final Class<?> bootclass) {
	final Throwable[] err = new Throwable[1];
	final Responder[] res = new Responder[1];
	Thread boot = new Thread(this, "JSvc boot thread") {
		public void run() {
		    try {
			Method cm = bootclass.getMethod("responder");
			Object resp = cm.invoke(null);
			if(!(resp instanceof Responder))
			    throw(new ClassCastException("JSvc bootstrapper did not return a responder"));
			res[0] = (Responder)resp;
		    } catch(NoSuchMethodException e) {
			logger.log(Level.SEVERE, "Invalid JSvc bootstrapper specified", e);
			err[0] = e;
		    } catch(IllegalAccessException e) {
			logger.log(Level.SEVERE, "Invalid JSvc bootstrapper specified", e);
			err[0] = e;
		    } catch(InvocationTargetException e) {
			logger.log(Level.SEVERE, "JSvc bootstrapper failed", e);
			err[0] = e;
		    }
		}
	    };
	boot.start();
	try {
	    boot.join();
	} catch(InterruptedException e) {
	    logger.log(Level.WARNING, "Interrupted during bootstrapping", e);
	    boot.interrupt();
	    Thread.currentThread().interrupt();
	}
	if(err[0] != null) {
	    destroy();
	    throw(new RuntimeException(err[0]));
	}
	if(res[0] == null) {
	    destroy();
	    logger.log(Level.SEVERE, "No responder returned in spite of no error having happened.");
	    throw(new NullPointerException("No responder returned in spite of no error having happened."));
	}
	return(res[0]);
    }

    public static ThreadContext current() {
	for(ThreadGroup tg = Thread.currentThread().getThreadGroup(); tg != null; tg = tg.getParent()) {
	    if(tg instanceof ThreadContext)
		return((ThreadContext)tg);
	}
	return(null);
    }
    
    public static class CreateException extends Exception {
	public CreateException(String message) {
	    super(message);
	}

	public CreateException(String message, Throwable cause) {
	    super(message, cause);
	}
    }

    public static ThreadContext create(ServerContext ctx, ClassLoader cl) throws CreateException {
	String nm = "JSvc Service";
	if(ctx.name() != null)
	    nm = "JSvc Service for " + ctx.name();
	
	String clnm = ctx.libconfig("jsvc.bootstrap", null);
	if(clnm == null)
	    throw(new CreateException("No JSvc bootstrapper specified"));
	Class<?> bc;
	try {
	    bc = cl.loadClass(clnm);
	} catch(ClassNotFoundException e) {
	    throw(new CreateException("Invalid JSvc bootstrapper specified", e));
	}
	return(new ThreadContext(null, nm, ctx, bc));
    }
}
