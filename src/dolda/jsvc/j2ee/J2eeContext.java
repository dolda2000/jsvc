package dolda.jsvc.j2ee;

import dolda.jsvc.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class J2eeContext implements ServerContext {
    private ServletConfig cfg;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    
    J2eeContext(ServletConfig cfg, HttpServletRequest req, HttpServletResponse resp) {
	this.cfg = cfg;
	this.req = req;
	this.resp = resp;
    }
    
    public String rootpath() {
	return(req.getContextPath());
    }
}
