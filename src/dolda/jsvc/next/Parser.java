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

    public class State {
	public final Document doc = domimp.createDocument(null, "dummy", null);
	public final PeekReader in;
	
	private State(Reader in) {
	    this.in = new PeekReader(in);
	}
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
    
    protected Attr makeattr(Document doc, Element el, String name, String val) {
	Attr a = doc.createAttributeNS(el.getNamespaceURI(), name);
	a.setValue(val);
	return(a);
    }

    protected Attr makeattr(Document doc, Element el, String name) {
	return(doc.createAttributeNS(el.getNamespaceURI(), name));
    }

    protected String name(State s) throws IOException {
	StringBuilder buf = new StringBuilder();
	while(true) {
	    int c = s.in.peek();
	    if(c < 0) {
		break;
	    } else if(namechar((char)c)) {
		buf.append((char)s.in.read());
	    } else {
		break;
	    }
	}
	if(buf.length() == 0)
	    throw(new ParseException("Expected name, got `" + printable(s.in.peek()) + "'"));
	return(buf.toString());
    }
    
    protected String entity(State s) throws IOException {
	int c = s.in.read();
	if(c != '&')
	    throw(new ParseException("Expected `&' while reading entity, got `" + printable(c) + "'"));
	String nm = name(s);
	c = s.in.read();
	if(c != ';')
	    throw(new ParseException("Expected `;' while reading entity, got `" + printable(c) + "'"));
	return(entity(nm));
    }

    protected Attr attribute(State s, Element el) throws IOException {
	String nm = name(s);
	s.in.peek(true);
	int c = s.in.read();
	if(c != '=')
	    throw(new ParseException("Expected `=' while reading attribute, got `" + printable(c) + "'"));
	s.in.peek(true);
	int qt = s.in.read();
	if((qt != '"') && (qt != '\''))
	    throw(new ParseException("Expected double or single quote while reading attribute, got `" + printable(qt) + "'"));
	StringBuilder buf = new StringBuilder();
	while(true) {
	    c = s.in.peek();
	    if(c < 0) {
		throw(new ParseException("Unexpected end-of-file while reading attribute value"));
	    } else if(c == qt) {
		s.in.read();
		break;
	    } else if(c == '&') {
		buf.append(entity(s));
	    } else {
		buf.append((char)s.in.read());
	    }
	}
	return(makeattr(s.doc, el, nm, buf.toString()));
    }
    
    protected Element element(State s) throws IOException {
	Element n = makenode(s.doc, name(s));
	while(true) {
	    int c = s.in.peek(true);
	    if(c < 0) {
		throw(new ParseException("Unexpected end-of-file while parsing start tag"));
	    } else if(c == '>') {
		s.in.read();
		break;
	    } else if(c == '/') {
		s.in.read();
		s.in.peek(true);
		c = s.in.read();
		if(c != '>')
		    throw(new ParseException("Unexpected character `" + printable(c) + "' encountered in end of empty tag"));
		return(n);
	    } else if(namechar((char)c)) {
		n.setAttributeNodeNS(attribute(s, n));
	    } else {
		throw(new ParseException("Unexpected character `" + printable(c) + "' encountered in start tag"));
	    }
	}
	while(true) {
	    int c = s.in.peek();
	    if(c < 0) {
		break;
	    } else if(c == '<') {
		s.in.read();
		c = s.in.peek(true);
		if(c == '/') {
		    s.in.read();
		    s.in.peek(true);
		    String nm = name(s);
		    if(!nm.equals(n.getTagName()))
			throw(new ParseException("Unexpected end tag for `" + nm + "' while parsing `" + n.getTagName() + "'"));
		    if(s.in.peek(true) != '>')
			throw(new ParseException("Expected `>' while reading end tag, got `" + printable(c) + "'"));
		    s.in.read();
		    break;
		} else {
		    n.appendChild(stag(s));
		}
	    } else {
		n.appendChild(text(s));
	    }
	}
	return(n);
    }
    
    protected Comment comment(State s) throws IOException {
	if((s.in.read() != '!') ||
	   (s.in.read() != '-') ||
	   (s.in.read() != '-'))
	    throw(new ParseException("Illegal start of comment"));
	StringBuilder buf = new StringBuilder();
	while(true) {
	    int c = s.in.peek();
	    if(c < 0) {
		throw(new ParseException("Unexpected end-of-file while parsing comment"));
	    } else if(c == '-') {
		s.in.read();
		if(s.in.peek() == '-') {
		    s.in.read();
		    if(s.in.peek() == '>') {
			s.in.read();
			break;
		    } else {
			buf.append("--");
		    }
		} else {
		    buf.append("-");
		}
	    } else {
		buf.append((char)s.in.read());
	    }
	}
	return(s.doc.createComment(buf.toString()));
    }

    protected Node stag(State s) throws IOException {
	int c = s.in.peek(true);
	if(c < 0) {
	    throw(new ParseException("Unexpected end-of-file while parsing tag type"));
	} else if(c == '!') {
	    return(comment(s));
	} else {
	    return(element(s));
	}
    }

    protected Text text(State s) throws IOException {
	StringBuilder buf = new StringBuilder();
	while(true) {
	    int c = s.in.peek();
	    if(c < 0) {
		break;
	    } else if(c == '<') {
		break;
	    } else if(c == '&') {
		buf.append(entity(s));
	    } else {
		buf.append((char)s.in.read());
	    }
	}
	return(s.doc.createTextNode(buf.toString()));
    }

    public DocumentFragment parse(Reader in) throws IOException {
	State s = new State(in);
	DocumentFragment frag = s.doc.createDocumentFragment();
	while(true) {
	    int c = s.in.peek();
	    if(c < 0) {
		return(frag);
	    } else if(c == '<') {
		s.in.read();
		frag.appendChild(stag(s));
	    } else {
		frag.appendChild(text(s));
	    }
	}
    }

    private static String printable(int c) {
	if(c < 0)
	    return("EOF");
	if(c < 32)
	    return(String.format("\\%03o", (int)c));
	return(Character.toString((char)c));
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
