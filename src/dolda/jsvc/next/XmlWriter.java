package dolda.jsvc.next;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import dolda.jsvc.util.Misc;

public class XmlWriter {
    private Map<String, String> nsnames = new HashMap<String, String>();
    public final Document doc;
    private int nsser = 1;
    
    public XmlWriter(Document doc) {
	this.doc = doc;
    }
    
    public void setnsname(String uri, String name) {
	nsnames.put(uri, name);
    }

    private String nsname(String uri) {
	String ret;
	if(nsnames.containsKey(uri))
	    return(nsnames.get(uri));
	do {
	    ret = "n" + (nsser++);
	} while(nsnames.containsValue(ret));
	nsnames.put(uri, ret);
	return(ret);
    }
    
    protected void findallnsnames() {
	Node n = doc;
	while(true) {
	    String ns = n.getNamespaceURI();
	    if(ns != null)
		nsname(ns);
	    if(n.getFirstChild() != null) {
		n = n.getFirstChild();
	    } else if(n.getNextSibling() != null) {
		n = n.getNextSibling();
	    } else {
		for(n = n.getParentNode(); n != null; n = n.getParentNode()) {
		    if(n.getNextSibling() != null) {
			n = n.getNextSibling();
			break;
		    }
		}
		if(n == null)
		    break;
	    }
	}
    }
    
    protected boolean prebreak(ColumnWriter out, Element el) {
	return(false);
    }

    protected int indent(ColumnWriter out, Element el) {
	return(-1);
    }

    protected boolean postbreak(ColumnWriter out, Element el) {
	return(false);
    }

    protected boolean asempty(ColumnWriter out, Element el) {
	return(true);
    }

    protected void attribute(ColumnWriter out, String nm, String val, int indent) throws IOException {
	char qt = '\"';
	if((val.indexOf("\"") >= 0) && (val.indexOf('\'') < 0))
	    qt = '\'';
	out.write(" " + nm + "=" + qt);
	for(int i = 0; i < val.length(); i++) {
	    char c = val.charAt(i);
	    if(c == '<')
		out.write("&lt;");
	    else if(c == '>')
		out.write("&gt;");
	    else if(c == '&')
		out.write("&amp;");
	    else if(c == qt)
		out.write((c == '\'')?"&apos;":"&quot;");
	    else
		out.write(c);
	}
	out.write(qt);
    }
    
    protected void attribute(ColumnWriter out, Attr attr, int indent) throws IOException {
	String ns = attr.getNamespaceURI();
	if(ns == null)
	    attribute(out, attr.getName(), attr.getValue(), indent);
	else
	    attribute(out, nsname(ns) + ":" + attr.getName(), attr.getValue(), indent);
    }

    protected void element(ColumnWriter out, Element el, int indent) throws IOException {
	if(prebreak(out, el))
	    out.indent(indent);
	
	String tagname = el.getTagName();
	String ns = nsname(el.getNamespaceURI());
	if(ns != null)
	    tagname = ns + ":" + tagname;
	out.write("<" + tagname);
	NamedNodeMap attrs = el.getAttributes();
	int acol = out.col + 1;
	if(attrs != null) {
	    for(int i = 0; i < attrs.getLength(); i++) {
		Attr attr = (Attr)attrs.item(i);
		attribute(out, attr, acol);
	    }
	}
	if(el == doc.getDocumentElement()) {
	    for(Map.Entry<String, String> nd : nsnames.entrySet()) {
		String nm = nd.getValue();
		if(nm == null)
		    attribute(out, "xmlns", nd.getKey(), acol);
		else
		    attribute(out, "xmlns:" + nm, nd.getKey(), acol);
	    }
	}
	
	if((el.getFirstChild() == null) && asempty(out, el)) {
	    out.write(" />");
	} else {
	    out.write(">");
	    int inner = indent(out, el);
	    if(inner >= 0) {
		out.indent(indent + inner);
	    }
	    
	    for(Node ch = el.getFirstChild(); ch != null; ch = ch.getNextSibling())
		node(out, ch, (inner >= 0)?(indent + inner):indent);
	    
	    if(inner >= 0)
		out.indent(indent);
	    out.write("</" + tagname + ">");
	}
	
	if(postbreak(out, el))
	    out.write('\n');
    }
    
    protected void text(ColumnWriter out, String s, int indent) throws IOException {
	out.write(Misc.htmlq(s));
    }

    protected void text(ColumnWriter out, Text txt, int indent) throws IOException {
	String s = txt.getData();
	text(out, s, indent);
    }

    protected void comment(ColumnWriter out, Comment c, int indent) throws IOException {
	out.write("<!--");
	String s = c.getData();
	text(out, s, indent);
	out.write("-->");
    }

    protected void node(ColumnWriter out, Node n, int indent) throws IOException {
	if(n instanceof Element) {
	    Element el = (Element)n;
	    element(out, el, indent);
	} else if(n instanceof Text) {
	    Text txt = (Text)n;
	    text(out, txt, indent);
	} else if(n instanceof Comment) {
	    Comment c = (Comment)n;
	    comment(out, c, indent);
	} else {
	    throw(new RuntimeException(String.format("Unknown DOM node encountered (%s)", n.getClass())));
	}
    }
    
    protected void doctype(ColumnWriter out, DocumentType dt) throws IOException {
	out.write(String.format("<!DOCTYPE %s PUBLIC \"%s\" \"%s\">\n", dt.getName(), dt.getPublicId(), dt.getSystemId()));
    }

    public void write(Writer out) throws IOException {
	findallnsnames();
	ColumnWriter col = new ColumnWriter(out);
	DocumentType t = doc.getDoctype();
	if(t != null)
	    doctype(col, t);
	node(col, doc.getDocumentElement(), 0);
    }

    public void write(OutputStream out) throws IOException {
	/* The OutputStreamWriter may need to be flushed to clear any
	 * internal buffers it may have, but it would be a pity to
	 * force-flush the underlying stream just because of that. */
	class FlushGuard extends FilterOutputStream {
	    FlushGuard(OutputStream out) {
		super(out);
	    }
	    
	    public void flush() {}
	}
	Writer w = new OutputStreamWriter(new FlushGuard(out), Misc.utf8);
	w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
	write(w);
	w.flush();
    }
}
