package dolda.jsvc.test;

import dolda.jsvc.*;
import dolda.jsvc.util.*;

public class Bootstrap {
    public static Responder responder() {
	return(new ErrorHandler(new TestResponder()));
    }
}
