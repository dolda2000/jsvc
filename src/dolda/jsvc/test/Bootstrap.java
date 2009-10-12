package dolda.jsvc.test;

import dolda.jsvc.*;

public class Bootstrap {
    public static Responder responder() {
	return(new TestResponder());
    }
}
