package dolda.jsvc;

import java.util.logging.*;
import java.lang.reflect.*;

public class ThreadContext extends ThreadGroup {
    private Logger logger = Logger.getLogger("dolda.jsvc.context");
    private ThreadGroup workers;
    private long reqs = 0;
    public final Responder root;
    
    public ThreadContext(ThreadGroup parent, String name, Class<?> bootclass) {
	super((parent == null)?(Thread.currentThread().getThreadGroup()):parent, name);
	workers = new ThreadGroup(this, "Worker threads") {
		public void uncaughtException(Thread t, Throwable e) {
		    logger.log(Level.SEVERE, "Worker thread terminated with an uncaught exception", e);
		}
	    };
	root = bootstrap(bootclass);
    }
    
    public void uncaughtException(Thread t, Throwable e) {
	logger.log(Level.SEVERE, "Service thread " + t.toString() + " terminated with an uncaught exception", e);
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
	return(new RequestThread(root, req, workers, "Worker thread " + reqs++));
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
	if(err[0] != null)
	    throw(new RuntimeException(err[0]));
	if(res[0] == null) {
	    logger.log(Level.SEVERE, "No responder returned in spite of no error having happened.");
	    throw(new NullPointerException("No responder returned in spite of no error having happened."));
	}
	return(res[0]);
    }
}
