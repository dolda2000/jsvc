package dolda.jsvc.next;

import java.util.*;
import org.w3c.dom.*;

public class DocBuffer {
    public final Document doc;
    private final Map<String, Node> cursors = new HashMap<String, Node>();
    public static final String ns = "jsvc:next:buffer";
    
    public DocBuffer(String ns, String root, String doctype, String pubid, String sysid) {
	doc = DomUtil.document(ns, root, doctype, pubid, sysid);
    }

    public DocBuffer(String ns, String root) {
	this(ns, root, null, null, null);
    }
    
    private Node findcursor(Node c, String name) {
	if(c instanceof Element) {
	    Element el = (Element)c;
	    if(el.getNamespaceURI().equals(ns) && el.getTagName().equals("cursor") && el.getAttributeNS(ns, "name").equals(name))
		return(c);
	}
	for(Node n = c.getFirstChild(); n != null; n = n.getNextSibling()) {
	    Node r = findcursor(n, name);
	    if(r != null)
		return(r);
	}
	return(null);
    }

    private Node cursor(String name) {
	Node n;
	if((n = cursors.get(name)) != null) {
	    return(n);
	}
	if((n = findcursor(doc, name)) == null)
	    return(null);
	cursors.put(name, n);
	return(n);
    }
    
    public void insert(String cursor, Node n) {
	Node c = cursor(cursor);
	if(c == null)
	    throw(new RuntimeException("No such cursor: `" + cursor + "'"));
	c.getParentNode().insertBefore(c, doc.importNode(n, true));
    }
    
    public Element makecursor(String name) {
	Element el = doc.createElementNS(ns, "cursor");
	Attr a = doc.createAttributeNS(ns, "name");
	a.setValue(name);
	el.setAttributeNodeNS(a);
	return(el);
    }

    public Element el(String ns, String nm, Node contents, String... attrs) {
	Element el = doc.createElementNS(ns, nm);
	if(contents != null)
	    el.appendChild(contents);
	for(String attr : attrs) {
	    int p = attr.indexOf('=');
	    el.setAttribute(attr.substring(0, p), attr.substring(p + 1));
	}
	return(el);
    }
    
    public Text text(String text) {
	return(doc.createTextNode(text));
    }
}
