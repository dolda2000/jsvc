package dolda.jsvc.next;

import java.io.*;
import org.w3c.dom.*;

public class XHtmlWriter extends IndentWriter {
    public XHtmlWriter(Document doc) {
	super(doc);
	setnsname(Html.ns, null);
    }
    
    protected int indent(ColumnWriter out, Element el) {
	if(Html.ns.equals(el.getNamespaceURI())) {
	    if(el.getTagName().equals("pre"))
		return(-1);
	    if(el.getTagName().equals("textarea"))
		return(-1);
	}
	return(super.indent(out, el));
    }
}
