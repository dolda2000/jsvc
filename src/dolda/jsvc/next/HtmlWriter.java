package dolda.jsvc.next;

import java.io.*;
import org.w3c.dom.*;

public class HtmlWriter extends XHtmlWriter {
    public HtmlWriter(Document doc) {
	super(doc);
    }
    
    protected boolean asempty(ColumnWriter out, Element el) {
	if(!super.asempty(out, el))
	    return(false);
	String n = el.getTagName();
	if(n.equals("br") || n.equals("hr") || n.equals("img") ||
	   n.equals("input"))
	    return(true);
	return(false);
    }
    
    protected void attribute(ColumnWriter out, Attr attr, int indent) throws IOException {
	if(attr.getNamespaceURI() != null)
	    throw(new RuntimeException("HTML does not support non-null-NS attributes (" + attr.getNamespaceURI() + " encountered)"));
	super.attribute(out, attr, indent);
    }

    protected void element(ColumnWriter out, Element el, int indent) throws IOException {
	if(!el.getNamespaceURI().equals(Html.ns))
	    throw(new RuntimeException("HTML does not support non-HTML elements (namespace " + el.getNamespaceURI() + " encountered)"));
	super.element(out, el, indent);
    }

    public void write(Writer out) throws IOException {
	DocumentType dt = doc.getDoctype();
	if(dt == null)
	    throw(new RuntimeException("Writing HTML requires an HTML document"));
	if(!dt.getName().equals("html"))
	    throw(new RuntimeException("Writing HTML requires an HTML document, not `" + dt.getName() + "'"));
	String pubid = dt.getPublicId();
	if(pubid.equals("-//W3C//DTD XHTML 1.1//EN")) {
	} else {
	    throw(new RuntimeException("Unimplemented HTML doctype `" + pubid));
	}
	super.write(out);
    }
}
