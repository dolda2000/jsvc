package dolda.jsvc.next;

import org.w3c.dom.*;
import org.w3c.dom.ls.*;
import javax.xml.validation.*;
import java.net.*;
import java.io.*;

public class Html extends DocBuffer {
    public static final String ns = "http://www.w3.org/1999/xhtml";
    private static final Schema schema = DomUtil.loadxsd("xhtml1-strict.xsd");

    private Html(String pubid, String sysid) {
	super(ns, "html", "html", pubid, sysid);
    }

    public static Html xhtml11(String title) {
	Html buf = new Html("-//W3C//DTD XHTML 1.1//EN", "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd");
	Node html = buf.doc.getDocumentElement();
	Node head = DomUtil.insertel(html, "head");
	head.appendChild(buf.makecursor("head"));
	Node tit = DomUtil.insertel(head, "title");
	DomUtil.inserttext(tit, title);
	Node body = DomUtil.insertel(html, "body");
	body.appendChild(buf.makecursor("body"));
	return(buf);
    }
    
    public Element el(String name, Node contents, String... attrs) {
	return(el(ns, name, contents, attrs));
    }
    
    public Element csslink(String href, String name) {
	Element el = el("link", null, "rel=stylesheet", "type=text/css");
	if(name != null)
	    el.setAttribute("title", name);
	el.setAttribute("href", href);
	return(el);
    }
    
    public void addcss(String href, String name) {
	insert("head", csslink(href, name));
    }
    
    public void validate() {
	Validator val = schema.newValidator();
	try {
	    val.validate(new javax.xml.transform.dom.DOMSource(doc));
	} catch(org.xml.sax.SAXException e) {
	    throw(new RuntimeException(e));
	} catch(java.io.IOException e) {
	    /* Should never happen. */
	    throw(new Error(e));
	}
    }
}
