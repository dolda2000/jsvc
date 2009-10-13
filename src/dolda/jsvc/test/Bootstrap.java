package dolda.jsvc.test;

import dolda.jsvc.*;
import dolda.jsvc.util.*;

public class Bootstrap {
    public static Responder responder() {
	Multiplexer root = new Multiplexer();
	root.file("test", new TestResponder());
	root.file("", new StaticContent(Bootstrap.class, "static/index.html", false, "text/html"));
	root.file("css", new StaticContent(Bootstrap.class, "static/test.css", false, "text/css"));
	root.dir("foo", new StaticContent(Bootstrap.class, "static/foo", true, "text/plain; charset=utf-8"));
	return(Misc.stdroot(root));
    }
}
