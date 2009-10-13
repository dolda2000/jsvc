package dolda.jsvc.util;

import java.util.*;
import java.text.*;

public class Http {
    public final static DateFormat datefmt;
    static {
	datefmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
	datefmt.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }
    
    public static String fmtdate(Date d) {
	return(datefmt.format(d));
    }
    
    public static Date parsedate(String str) throws ParseException {
	return(datefmt.parse(str));
    }
}
