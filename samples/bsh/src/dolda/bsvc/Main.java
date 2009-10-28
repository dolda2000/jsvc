package dolda.bsvc;

import dolda.jsvc.*;
import dolda.jsvc.util.*;

public class Main {
    public static Responder responder() {
	Multiplexer root = new Multiplexer();
	root.file("sh", new PerSession(ShellPage.class));
	root.file("css", new StaticContent(Main.class, "static/base.css", "text/css"));
	return(Misc.stdroot(root));
    }
}
