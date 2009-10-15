package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import dolda.jsvc.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class J2eeContext implements ServerContext {
    private ServletConfig cfg;
    private long ctime;
    
    J2eeContext(ServletConfig cfg) {
	this.cfg = cfg;
	this.ctime = System.currentTimeMillis();
    }
    
    public long starttime() {
	return(ctime);
    }
}
