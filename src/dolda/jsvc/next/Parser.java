package dolda.jsvc.next;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.*;

public class Parser {
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

    private static boolean namechar(char c) {
	return((c == ':') || (c == '_') || (c == '$') || (c == '.') || (c == '-') || ((c >= '0') && (c <= '9')) || ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')));
    }

    protected String entity(String name) {
	if(name.equals("amp"))
	    return("&");
	if(name.equals("lt"))
	    return("<");
	if(name.equals("gt"))
	    return(">");
	if(name.equals("apos"))
	    return("'");
	if(name.equals("quot"))
	    return("\"");
	return(null);
    }
    
    protected Element makenode(Document doc, String name) {
	return(doc.createElementNS(null, name));
    }
    
    protected Attr makeattr(Document doc, Element el, String name) {
	return(doc.createAttributeNS(el.getNamespaceURI(), name));
    }

    public DocumentFragment parse(Reader in) throws IOException {
	Stack<Node> stack = new Stack<Node>();
	Document doc = domimp.createDocument(null, "dummy", null);
	DocumentFragment frag = doc.createDocumentFragment();
	stack.push(frag);
	String st = "content";
	int c = in.read();
	StringBuilder buf = new StringBuilder();
	StringBuilder ebuf = new StringBuilder();
	char atype = 0;
	int cdashcnt = 0;
	while(true) {
	    if(st == "content") {
		if(c == '<') {
		    st = "tag";
		    c = in.read();
		} else if(c < 0) {
		    if(stack.peek() == frag)
			return(frag);
		    else
			throw(new ParseException("Unexpected end-of-file while parsing non-root element"));
		} else {
		    st = "text";
		}
	    } else if(st == "tag") {
		if(Character.isWhitespace((char)c)) {
		    c = in.read();
		} else if(c == '!') {
		    cdashcnt = 0;
		    c = in.read();
		    st = "comment";
		} else if(namechar((char)c)) {
		    st = "stag";
		} else if(c == '/') {
		    c = in.read();
		    st = "etag";
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing tag"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in tag name"));
		}
	    } else if(st == "stag") {
		boolean flush = false;
		if(namechar((char)c)) {
		    buf.append((char)c);
		    c = in.read();
		} else if(c == '>') {
		    flush = true;
		} else if(Character.isWhitespace((char)c)) {
		    flush = true;
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing tag name"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in tag name"));
		}
		if(flush) {
		    Element n = makenode(doc, buf.toString());
		    buf = new StringBuilder();
		    stack.peek().appendChild(n);
		    stack.push(n);
		    st = "attr";
		}
	    } else if(st == "comment") {
		if(c == '-') {
		    cdashcnt++;
		    c = in.read();
		} else if((c == '>') && (cdashcnt == 4)) {
		    stack.peek().appendChild(doc.createComment(buf.toString()));
		    buf = new StringBuilder();
		    st = "content";
		    c = in.read();
		} else if(cdashcnt >= 2) {
		    if(cdashcnt > 2)
			cdashcnt = 2;
		    buf.append((char)c);
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing comment"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in comment"));
		}
	    } else if(st == "attr") {
		if(namechar((char)c)) {
		    st = "aname";
		} else if(c == '>') {
		    st = "content";
		    c = in.read();
		} else if(c == '/') {
		    st = "stagend";
		    c = in.read();
		} else if(Character.isWhitespace((char)c)) {
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing attributes"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered inside tag"));
		}
	    } else if(st == "stagend") {
		if(c == '>') {
		    stack.pop();
		    c = in.read();
		    st = "content";
		} else if(Character.isWhitespace((char)c)) {
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file at end of empty tag"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered at and of empty tag"));
		}
	    } else if(st == "aname") {
		if(namechar((char)c)) {
		    buf.append((char)c);
		    c = in.read();
		} else if(Character.isWhitespace((char)c)) {
		    c = in.read();
		} else if(c == '=') {
		    Element el = (Element)stack.peek();
		    Attr attr = makeattr(doc, el, buf.toString());
		    el.setAttributeNodeNS(attr);
		    buf = new StringBuilder();
		    stack.push(attr);
		    st = "avalstart";
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing attribute name"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in attribute name"));
		}
	    } else if(st == "avalstart") {
		if((c == '\'') || (c == '"')) {
		    atype = (char)c;
		    c = in.read();
		    st = "aval";
		} else if(Character.isWhitespace((char)c)) {
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing attribute value"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in attribute value"));
		}
	    } else if(st == "aval") {
		if(c == atype) {
		    c = in.read();
		    Attr a = (Attr)stack.pop();
		    a.setValue(buf.toString());
		    buf = new StringBuilder();
		    st = "attr";
		} else if(c == '&') {
		    c = in.read();
		    st = "aent";
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing attribute value"));
		} else {
		    buf.append((char)c);
		    c = in.read();
		}
	    } else if(st == "etag") {
		if(namechar((char)c)) {
		    buf.append((char)c);
		    c = in.read();
		} else if(c == '>') {
		    String nm = buf.toString();
		    buf = new StringBuilder();
		    Node n = stack.pop();
		    if(n instanceof DocumentFragment)
			throw(new ParseException("Unexpected end tag for `" + nm + "' while parsing root content"));
		    Element el = (Element)n;
		    if(!nm.equals(el.getTagName()))
			throw(new ParseException("Unexpected end tag for `" + nm + "' while parsing `" + el.getTagName() + "'"));
		    c = in.read();
		    st = "content";
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing end tag"));
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in end tag"));
		}
	    } else if(st == "text") {
		boolean flush = false;
		if(c == '&') {
		    st = "ent";
		    c = in.read();
		} else if(c == '<') {
		    flush = true;
		    st = "content";
		} else if(c < 0) {
		    flush = true;
		    st = "content";
		} else {
		    buf.append((char)c);
		    c = in.read();
		}
		if(flush) {
		    Text n = doc.createTextNode(buf.toString());
		    buf = new StringBuilder();
		    stack.peek().appendChild(n);
		}
	    } else if(st == "ent") {
		if(c == ';') {
		    String ename = ebuf.toString();
		    ebuf = new StringBuilder();
		    String rep = entity(ename);
		    if(rep == null)
			throw(new ParseException("Unknown entity `" + ename + "' encountered"));
		    buf.append(rep);
		    st = "text";
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing entity name"));
		} else if(namechar((char)c)) {
		    ebuf.append((char)c);
		    c = in.read();
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in entity name"));
		}
	    } else if(st == "aent") {
		if(c == ';') {
		    String ename = ebuf.toString();
		    ebuf = new StringBuilder();
		    String rep = entity(ename);
		    if(rep == null)
			throw(new ParseException("Unknown entity `" + ename + "' encountered"));
		    buf.append(rep);
		    st = "aval";
		    c = in.read();
		} else if(c < 0) {
		    throw(new ParseException("Unexpected end-of-file while parsing entity name"));
		} else if(namechar((char)c)) {
		    ebuf.append((char)c);
		    c = in.read();
		} else {
		    throw(new ParseException("Unexpected character `" + printable((char)c) + "' encountered in entity name"));
		}
	    } else {
		throw(new Error("BUG: Typoed state " + st));
	    }
	}
    }
    
    private static String printable(char c) {
	if(c < 32)
	    return(String.format("\\%03o", (int)c));
	return(Character.toString(c));
    }

    public static void main(String[] args) throws Exception {
	Parser p = new Parser();
	DocumentFragment f = p.parse(new FileReader(args[0]));
	javax.xml.transform.TransformerFactory fac = javax.xml.transform.TransformerFactory.newInstance();
	fac.setAttribute("indent-number", 2);
	javax.xml.transform.Transformer t = fac.newTransformer();
	t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
	t.transform(new javax.xml.transform.dom.DOMSource(f), new javax.xml.transform.stream.StreamResult(System.out));
	System.out.println(t.getClass());
    }
}
