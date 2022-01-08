import java.io.IOException;
import atlantis.tools.*;

/*
 * tester.java
 *
 * Created on 13. Oktober 2007, 20:52
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author cnsaeman
 */
public class extractepub {

    public final static String EOP = String.valueOf((char) 12);   // EndOfPage signal
    public final static String filesep = System.getProperty("file.separator");  // EndOfLine signal
    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public static TextFile out;

    public static void safeWrite(String k,String v) throws IOException {
        if ((v!=null) && (v.length()>0)) {
            v=Parser.decodeHTML(v);
            v=Parser.cutTags(v);
            out.putString(k+":"+Parser.replace(v,linesep," "));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            EPUBFile ef=new EPUBFile(args[0]);
            out = new TextFile(args[1], false);
            if (ef.metaData.size()>0) {
                out.putString("$$metadata");
                out.putString("type:eBook");
                safeWrite("title",ef.metaData.get("dc:title").get("value"));
                safeWrite("date",ef.metaData.get("dc:date").get("value"));
                safeWrite("publisher",ef.metaData.get("dc:publisher").get("value"));
                safeWrite("language",ef.metaData.get("dc:language").get("value"));
                safeWrite("subject",ef.metaData.get("dc:subject").get("value"));
                safeWrite("abstract",ef.metaData.get("dc:description").get("value"));
                safeWrite("identifier",ef.metaData.get("dc:identifier").get("value"));
                safeWrite("authors",ef.metaData.get("dc:creator").get("value"));
                out.putString("metadata$$");
            }
            out.putString(ef.containedText());
            out.close();
        } catch (Exception ex) {
	    ex.printStackTrace();
            System.out.println(ex.toString());
        }
    }
}
