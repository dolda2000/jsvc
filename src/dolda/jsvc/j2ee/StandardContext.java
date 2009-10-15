package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import javax.servlet.*;

public class StandardContext extends J2eeContext {
    StandardContext(ServletConfig cfg) {
	super(cfg);
    }
    
    public String name() {
	return(null);
    }
}
