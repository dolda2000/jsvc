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
	    String ns = el.getNamespaceURI();
	    if((ns != null) && ns.equals(DocBuffer.ns) && el.getTagName().equals("cursor") && el.getAttributeNS(null, "name").equals(name))
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
	if(n.getOwnerDocument() != doc)
	    n = doc.importNode(n, true);
	c.getParentNode().insertBefore(n, c);
    }
    
    public Element makecursor(String name) {
	Element el = doc.createElementNS(ns, "cursor");
	Attr a = doc.createAttributeNS(null, "name");
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
	if(text == null)
	    return(null);
	return(doc.createTextNode(text));
    }
    
    public Node asnode(Object o) {
	if(o instanceof Node) {
	    Node n = (Node)o;
	    if(n.getOwnerDocument() != doc)
		return(doc.importNode(n, true));
	    return(n);
	}
	if(o instanceof String)
	    return(text((String)o));
	throw(new RuntimeException("Cannot convert a " + o.getClass().getName() + " to a DOM node"));
    }
    
    public void finalise() {
	Node n = doc;
	while(true) {
	    Node nx;
	    if(n.getFirstChild() != null) {
		nx = n.getFirstChild();
	    } else if(n.getNextSibling() != null) {
		nx = n.getNextSibling();
	    } else {
		for(nx = n.getParentNode(); nx != null; nx = nx.getParentNode()) {
		    if(nx.getNextSibling() != null) {
			nx = nx.getNextSibling();
			break;
		    }
		}
	    }
	    String ns = n.getNamespaceURI();
	    if((ns != null) && ns.equals(DocBuffer.ns))
		n.getParentNode().removeChild(n);
	    if(nx == null)
		break;
	    else
		n = nx;
	}
    }
}
