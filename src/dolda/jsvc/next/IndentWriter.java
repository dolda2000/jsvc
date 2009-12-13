package dolda.jsvc.next;

import java.io.*;
import org.w3c.dom.*;

public class IndentWriter extends XmlWriter {
    public int collimit = 80;
    
    public IndentWriter(Document doc) {
	super(doc);
    }
    
    private static boolean onlytext(Element el) {
	for(Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
	    if(!(n instanceof Text))
		return(false);
	}
	return(true);
    }
    
    protected boolean prebreak(ColumnWriter out, Element el) {
	if(el.getFirstChild() == null)
	    return(false);
	if(onlytext(el))
	    return(false);
	return(true);
    }
    
    protected int indent(ColumnWriter out, Element el) {
	if(onlytext(el))
	    return(-1);
	return(2);
    }

    protected boolean postbreak(ColumnWriter out, Element el) {
	if(out.col > collimit)
	    return(true);
	return(!onlytext(el));
    }

    protected void attribute(ColumnWriter out, String nm, String val, int indent) throws IOException {
	if(out.col > indent) {
	    if(nm.length() + val.length() + 4 > collimit)
		out.indent(indent);
	}
	super.attribute(out, nm, val, indent);
    }
    
    public static void main(String[] args) throws Exception {
	Html barda = Html.xhtml11("Barda");
	barda.addcss("/slen.css", "Test");
	barda.insert("body", barda.el("h1", barda.text("Mast")));
	barda.finalise();
	barda.validate();
	XmlWriter w = new IndentWriter(barda.doc);
	w.setnsname(Html.ns, null);
	w.write(System.out);
	System.out.flush();
    }
}
