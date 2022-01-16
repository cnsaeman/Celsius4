
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipFile;
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
import java.util.ArrayList;

public class epubfile {

    public final static String EOP = String.valueOf((char) 12);   // EndOfPage signal
    public final static String filesep = System.getProperty("file.separator");  // EndOfLine signal
    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public String filename;
    public int error;

    public ZipFile ZF;
    private String rootfile;
    private String relbase;
    public ArrayList<XMLentry> MetaData;
    private ArrayList<XMLentry> Manifest;
    private ArrayList<XMLentry> Spine;

    private ArrayList<String> fheader;
    private ArrayList<String> fcontent;
    private ArrayList<String> ffooter;
    private boolean readIn;
    
    public epubfile(String fn) {
        readIn=false;
        filename=fn;
        fheader=new ArrayList<String>();
        fcontent=new ArrayList<String>();
        ffooter=new ArrayList<String>();
        try {
            ZF = new ZipFile(fn);
            readInHeader();
            ZF.close();
        } catch (IOException ex) {
            error=255; return;
        }
    }
    
    private void readInHeader() throws IOException {
        String tmp = FileContent("mimetype").trim().toLowerCase();
        if (!tmp.equals("application/epub+zip"));
        tmp = FileContent("META-INF/container.xml");
        tmp = Parser.cutUntil(Parser.cutFrom(tmp, "<rootfile"), "/>");
        tmp = getProperty(tmp,"full-path");
        rootfile=tmp;
        if (rootfile.indexOf(filesep)>0) {
            relbase=Parser.cutUntilLast(rootfile,filesep);
        } else {
            relbase="";
        }
        if (!relbase.equals("")) relbase+=filesep;
        tmp=FileContent(rootfile);
        String md=Parser.cutFrom(Parser.cutUntil(tmp, "</metadata>"), "<metadata").trim();
        if (md.equals(""))
            md = Parser.cutFrom(Parser.cutUntil(tmp, "</opf:metadata>"), "<opf:metadata").trim();
        md=Parser.cutFrom(md,">");
        MetaData=XMLtoAL(md);
        //System.out.println(MetaData.toString());
        md=Parser.cutFrom(Parser.cutUntil(tmp, "</manifest>"), "<manifest").trim();
        if (md.equals(""))
            md = Parser.cutFrom(Parser.cutUntil(tmp, "</opf:manifest>"), "<opf:manifest").trim();
        md=Parser.cutFrom(md,">");
        Manifest=XMLtoAL(md);
        //System.out.println(Manifest.toString());
        md=Parser.cutFrom(Parser.cutUntil(tmp, "</spine>"), "<spine").trim();
        if (md.equals(""))
            md = Parser.cutFrom(Parser.cutUntil(tmp, "</opf:spine>"), "<opf:spine").trim();
        md=Parser.cutFrom(md,">");
        Spine=XMLtoAL(md);
        //System.out.println(Spine.toString());
    }

    public String getProperty(String t, String key) {
        key+="=";
        if (t.indexOf(key)==-1) return(new String(""));
        String out=Parser.cutFrom(t,key).trim();
        char sepchar=out.charAt(0);
        out=out.substring(1);
        int i=out.indexOf(sepchar);
        if (i==-1) return(new String(""));
        out=out.substring(0,i);
        return(out);
    }

    private ArrayList<XMLentry> XMLtoAL(String md) {
        md=md.trim();
        ArrayList<XMLentry> out=new ArrayList<XMLentry>();
        String t,p,v;
        while (md.length()>0) {
            while (md.startsWith("<!--")) {
                md=Parser.cutFrom(md,"-->").trim();
            }
            if (md.charAt(0)!='<') break;
            t=Parser.cutUntil(md," ").substring(1);
            if (t.indexOf('>')>-1) {
                t=Parser.cutUntil(t,">");
                p=new String("");
            } else {
                p=Parser.cutUntil(Parser.cutFrom(md," "),">");
            }
            if (!p.endsWith("/")) {
                v=Parser.cutUntil(Parser.cutFrom(md,">"),"</"+t+">");
                md=Parser.cutFrom(md,"</"+t+">").trim();
                out.add(new XMLentry(t,p,v));
            } else {
                md=Parser.cutFrom(md,p).substring(1).trim();
                p=p.substring(0,p.length()-1);
                out.add(new XMLentry(t,p,null));
            }
        }
        return(out);
    }

    public String FileContent(String s) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(ZF.getInputStream(ZF.getEntry(s))));
        String tmp = new String("");
        while (br.ready()) {
            tmp += "\n" + br.readLine();
        }
        tmp = tmp.trim();
        br.close();
        return (tmp);
    }

    public String toPlainText(String in) {
        in = Parser.replace(in, "<br/>", "#oo#");
        in = Parser.replace(in, "</p>", "#oo#");
        in = Parser.replace(in, "</h2>", "#oo#");
        in = Parser.replace(in, "</li>", "#oo#");
        in = Parser.cutTags(in);
        in = in + " ";
        in = Parser.replace(in, "#oo# ", "#oo#");
        in = Parser.replace(in, "#oo#", linesep + linesep);
        in = Parser.replace(in, "  ", " ");
        return(in);
    }

    // Reads in the given file s at position pos
    public void ReadInFileContent(String s,int pos) throws IOException {
        //System.out.println("Reading: "+s);
        BufferedReader br = new BufferedReader(new InputStreamReader(ZF.getInputStream(ZF.getEntry(s))));
        String in = new String("");
        StringBuffer txt=new StringBuffer(10000);
        while (br.ready() && (in.indexOf("<body") == -1)) {
            txt.append(in+"\n");
            in = br.readLine();
        }
        txt.append(Parser.cutUntil(in,">")+">");
        in=Parser.cutFrom(in,">");
        fheader.add(pos, txt.toString().trim());
        txt=new StringBuffer(10000);
        while (br.ready() && (in.indexOf("</body") == -1)) {
            txt.append(in+"\n");
            in = br.readLine().trim();
        }
        txt.append(Parser.cutUntil(in,"</body"));
        fcontent.add(pos, txt.toString().trim());
        txt=new StringBuffer(10000);
        while (br.ready()) {
            txt.append(in+"\n");
            in = br.readLine().trim();
        }
        txt.append(in+"\n");
        ffooter.add(pos, txt.toString().trim());
        br.close();
    }

    public void assureReadIn() {
        if (!readIn) {
            try {
                ZF=new ZipFile(filename);
                String fn;
                int i=0;
                for (XMLentry ent : Spine) {
                    fn=ent.getProperty("idref");
                    //System.out.println("------assureReadIn");
                    //System.out.println(fn);
                    XMLentry ent2=new XMLentry("","","");
                    for (XMLentry ent3 : Manifest) {
                        if (ent3.getProperty("id").equals(fn)) {
                            ent2=ent3;
                            break;
                        }
                    }
                    //System.out.println(ent2.getProperty("id"));
                    if (ent2.getProperty("id").equals(fn)) {
                        fn=ent2.getProperty("href");
                        ReadInFileContent(relbase+fn, i);
                        i++;
                    }
                }
                ZF.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
            readIn=true;
        }
    }

    public String containedText() {
        assureReadIn();
        if (!readIn) return(null);
        StringBuffer out=new StringBuffer(10000);
        for(int i=0;i<fcontent.size();i++) {
               out.append(this.toPlainText(fcontent.get(i)));
               out.append(EOP);
        }
        return(out.toString());
    }

    public class XMLentry {
        String tag;
        String properties;
        String value;

        public XMLentry(String t,String p, String v) {
            tag=t; properties=p; value=v;
        }

        public String getProperty(String key) {
            key+="=";
            if (properties.indexOf(key)==-1) return(new String(""));
            String out=Parser.cutFrom(properties,key).trim();
            char sepchar=out.charAt(0);
            out=out.substring(1);
            int i=out.indexOf(sepchar);
            if (i==-1) return(new String(""));
            out=out.substring(0,i);
            return(out);
        }

        public String toString() {
            return("Tag: "+tag+" // Properties: "+properties+" // Value: "+value);
        }
    }
}
