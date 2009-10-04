package dolda.jsvc.util;

import java.util.*;

public class HeaderTreeMap extends WrappedMultiMap<String, String> {
    private static final Comparator<String> cicmp = new Comparator<String>() {
	public int compare(String a, String b){
	    return(a.toLowerCase().compareTo(b.toLowerCase()));
	}
    };
    
    public HeaderTreeMap() {
	super(new TreeMap<String, Collection<String>>(cicmp));
    }
}
