package dolda.jsvc.next;

import org.w3c.dom.*;
import org.w3c.dom.bootstrap.*;

public class DomUtil {
    private static final DOMImplementation domimp;
    
    static {
	DOMImplementationRegistry reg;
	try {
	    reg = DOMImplementationRegistry.newInstance();
	} catch(Exception e) {
	    throw(new Error(e));
	}
	DOMImplementation di = reg.getDOMImplementation("");
	if(di == null)
	    throw(new RuntimeException("Could not get a DOM implemenation"));
	domimp = di;
    }
    
    public static Document document(String ns, String root, String doctype, String pubid, String sysid) {
	if(doctype == null)
	    return(domimp.createDocument(ns, root, null));
	else
	    return(domimp.createDocument(ns, root, domimp.createDocumentType(doctype, pubid, sysid)));
    }
    
    public static Document document(String ns, String root) {
	return(document(ns, root, null, null, null));
    }
    
    public static Element insertel(Node p, String nm) {
	Document doc;
	if(p instanceof Document)
	    doc = (Document)p;
	else
	    doc = p.getOwnerDocument();
	Element el = doc.createElementNS(p.getNamespaceURI(), nm);
	p.appendChild(el);
	return(el);
    }
    
    public static Text inserttext(Node p, String text) {
	Document doc;
	if(p instanceof Document)
	    doc = (Document)p;
	else
	    doc = p.getOwnerDocument();
	Text t = doc.createTextNode(text);
	p.appendChild(t);
	return(t);
    }
}
