package dolda.jsvc.next;

import org.w3c.dom.*;
import org.w3c.dom.bootstrap.*;
import org.w3c.dom.ls.*;
import javax.xml.validation.*;
import java.io.*;

public class DomUtil {
    private static final DOMImplementation domimp;
    private static final SchemaFactory xsdfac;
    
    static {
	xsdfac = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
	xsdfac.setResourceResolver(new LSResourceResolver() {
		public LSInput resolveResource(String type, String ns, String pubid, String sysid, String base) {
		    if(sysid.indexOf('/') >= 0) {
			InputStream in = getcatalog(sysid.substring(sysid.lastIndexOf('/') + 1));
			if(in != null) {
			    LSInput ret = new LSInputAdapter(pubid, sysid, base);
			    ret.setByteStream(in);
			    ret.setEncoding("us-ascii");
			    return(ret);
			}
		    }
		    throw(new RuntimeException(String.format("Will not load external resources (for %s); please fix catalog.", sysid)));
		}
	    });
    }
    
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
    
    public static class LSInputAdapter implements LSInput {
	private String pubid, sysid, baseuri, encoding = null, data = null;
	private boolean cert = false;
	private InputStream bs = null;
	private Reader cs = null;
	
	public LSInputAdapter(String pubid, String sysid, String baseuri) {
	    this.pubid = pubid;
	    this.sysid = sysid;
	    this.baseuri = baseuri;
	}
	
	public String getBaseURI() {return(baseuri);}
	public String getPublicId() {return(pubid);}
	public String getSystemId() {return(sysid);}
	public void setBaseURI(String baseuri) {this.baseuri = baseuri;}
	public void setPublicId(String pubid) {this.pubid = pubid;}
	public void setSystemId(String sysid) {this.sysid = sysid;}

	public InputStream getByteStream() {return(bs);}
	public boolean getCertifiedText() {return(cert);}
	public Reader getCharacterStream() {return(cs);}
	public String getEncoding() {return(encoding);}
	public String getStringData() {return(data);}
	public void setByteStream(InputStream bs) {this.bs = bs;}
	public void setCertifiedText(boolean cert) {this.cert = cert;}
	public void setCharacterStream(Reader cs) {this.cs = cs;}
	public void setEncoding(String encoding) {this.encoding = encoding;}
	public void setStringData(String data) {this.data = data;}
    }
    
    private static InputStream getcatalog(String name) {
	if(name.indexOf('/') >= 0)
	    throw(new RuntimeException("Illegal catalog resource name `" + name + "'"));
	return(DomUtil.class.getResourceAsStream("catalog/" + name));
    }

    public static Schema loadxsd(String name) {
	InputStream in = getcatalog(name);
	if(in == null)
	    throw(new RuntimeException("Could not find schema `" + name + "'"));
	try {
	    return(xsdfac.newSchema(new javax.xml.transform.stream.StreamSource(in)));
	} catch(org.xml.sax.SAXException e) {
	    throw(new RuntimeException(e));
	}
    }
}
