import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import celsius.tools.TextFile;
import celsius.tools.Parser;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class inspiretst2 {

    public final static String EOP = String.valueOf((char) 12);   // EndOfPage signal
    public final static String filesep = System.getProperty("file.separator");  // EndOfLine signal
    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public static ZipFile ZF;
    public static TextFile out;
    public static String md;
    public static String bd; // basedir
    
    public static String inspireRecordJSON(String rec) {
      String tmp2 = TextFile.ReadOutURL("https://inspirehep.net/record/"+rec+"?of=recjson&ot=comment,title,system_number,abstract,authors,doi,primary_report_number,publication_info,physical_description,number_of_citations,thesaurus_terms");
      return tmp2;
    }



    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String inspirebase=null;
        if (inspirebase==null) inspirebase="http://inspirehep.net/";
        System.out.println(webToolsHEP.getInspireNoOfCitations2(inspirebase,"719715"));
    }
}
