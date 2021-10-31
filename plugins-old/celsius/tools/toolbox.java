//
// Celsius Library System
// (w) by C. Saemann
//
// toolbox.java
//
// This class contains various tools used in the other classes
//
// typesafe, pre-completed
//
// checked 15.09.2007
//

package celsius.tools;

import celsius.BibTeXRecord;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class toolbox {
    
    // Final Strings
    public final static Object[] optionsYNC = { "Yes", "No", "Cancel" };
    public final static Object[] optionsOC = { "OK", "Cancel" };
    public final static Object[] optionsYN = { "Yes", "No" };
    
    private static int threadindex=0;
    
    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public final static String filesep = System.getProperty("file.separator");  // EndOfLine signal
    public final static String EOP=String.valueOf((char)12);   // EndOfPage signal
    
    /**
     * Returns a new index for each thread, for debugging in Msg1
     */
    public static String getThreadIndex() {
        return(Integer.toString(threadindex++));
    }

    public static String fillLeadingZeros(String n,int i) {
        String out=n;
        while (out.length()<i) out="0"+out;
        return(out);
    }

    public static String formatSeconds(int sec) {
        int h = sec / 3600;
        int m = (sec - h * 3600) / 60;
        int s = (sec - h * 3600 - m * 60);
        String outdur = fillLeadingZeros(String.valueOf(m), 2) + ":" + fillLeadingZeros(String.valueOf(s), 2);
        if (h > 0) {
            outdur = String.valueOf(h) + ":" + outdur;
        }
        return (outdur);
    }
    
    /**
     * Warning Dialog
     */
    public static void Warning(java.awt.Component p, String msg,String head) {
        JOptionPane.showMessageDialog(p, msg, head, JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Information Dialog
     */
    public static void Information(java.awt.Component p, String msg,String head) {
        JOptionPane.showMessageDialog(p,msg,head, JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Question dialog with options OC
     */
    public static int QuestionOC(java.awt.Component p, String msg,String head) {
        return(JOptionPane.showOptionDialog(p,msg, head, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null, optionsOC, optionsOC[0]));
    }
    
    /**
     * Question dialog with options YNC
     */
    public static int QuestionYNC(java.awt.Component p, String msg,String head) {
        return(JOptionPane.showOptionDialog(p, msg, head,
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, optionsYNC, optionsYNC[0]));
    }
    
    /**
     * Question dialog with options YN
     */
    public static int QuestionYN(java.awt.Component p, String msg,String head) {
        return(JOptionPane.showOptionDialog(p, msg, head,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, optionsYN, optionsYN[0]));
    }
    
    /**
     * Question dialog with two choices: A,B
     */
    public static int QuestionAB(java.awt.Component p, String msg,String head,String A,String B) {
        Object[] options=new Object[2];
        options[0]=A; options[1]=B;
        return(JOptionPane.showOptionDialog(p, msg, head,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]));
    }

    /**
     * Question dialog with three choices: A, B, C
     */
    public static int QuestionABC(java.awt.Component p, String msg,String head,String A,String B,String C) {
        Object[] options=new Object[3];
        options[0]=A; options[1]=B; options[2]=C;
        return(JOptionPane.showOptionDialog(p, msg, head,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]));
    }
            
    /**
     * Question dialog with three choices: A, B, C
     */
    public static int QuestionABCD(java.awt.Component p, String msg,String head,String A,String B,String C,String D) {
        Object[] options=new Object[4];
        options[0]=A; options[1]=B; options[2]=C; options[3]=D;
        return(JOptionPane.showOptionDialog(p, msg, head,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]));
    }

    /**
     *  Returns current timestamp
     */
    public static String ActDatum() {
        Date ActD=new Date();
        return(ActD.toString());
    }
    
    public static String getFirstPage(String s) {
        String firstpage;
        if (!(new File(s)).exists()) {
            return("No plain text file found.");
        }
        try {
            GZIPInputStream fis = new GZIPInputStream(new FileInputStream(new File(s)));
            InputStreamReader isr = new InputStreamReader(fis);
            char[] fp = new char[4000];
            isr.read(fp);
            isr.close();
            fis.close();
            firstpage = new String(fp);
        } catch (IOException e) {
            e.printStackTrace();
            firstpage="error::"+e.toString();
        }
        return(firstpage);
    }
        
    /**
     * Normalize the BibTeX-String passed as an argument
     */    
    public static String NormalizeBibTeX(String tmp) {
        if (tmp.trim().length()<1) return("");
        BibTeXRecord btr=new BibTeXRecord(tmp);
        if (btr.parseError!=0) {
            toolbox.Warning(null,"BibTeX entry not consistent: "+BibTeXRecord.status[btr.parseError], "Warning:");
            return(tmp);
        }
        btr.put("title", Parser.LowerEndOfWords2(btr.get("title")));
        return(btr.toString());
    }
    
    /**
     * Condense a title string for Comparison
     */
    public static String Condense(String pre) {
        StringBuilder out=new StringBuilder(pre.toLowerCase());
        int i=0;
        while (i<out.length()) {
            if (!Character.isLetter(out.charAt(i))) out.deleteCharAt(i);
            else i++;
        }
        return(out.toString());
    }

    public static String wrap(String s) {
        return(wrap(s,80));
    }

    public static String wrap(String s, int len) {
        StringBuilder t=new StringBuilder(s);
        int i=-1;
        while (t.length()>i+len) {
            int k = t.lastIndexOf(" ", i + len);
            if (k > i) {
                int l = t.lastIndexOf("\n", i + len);
                if ((l>i) && (l < k)) {
                    i = l;
                } else {
                    t.replace(k, k + 1, "\n");
                    i += len;
                }
            } else {
                k=i+len;
                t.insert(k, "\n");
                i += len;
            }
        }
        return(t.toString());
    }
    
    /**
     * Center the current JDialog frame on screen
     */
    public static void centerFrame(JDialog frame) {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle r=environment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(r.width/2 - (frame.getWidth()/2),r.height/2 - (frame.getHeight()/2));
    }

    /**
     * Center the current JWindow frame on screen
     */
    public static void centerFrame(JFrame frame) {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle r=environment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(r.width/2 - (frame.getWidth()/2),r.height/2 - (frame.getHeight()/2));
    }
    
    /**
     * Get all citation tags from a latex-file
     */
    public static ArrayList<String> getCitations(String filename) {
        TextFile TD;
        ArrayList<String> Refs=new ArrayList<String>();
        try {
            TD = new TextFile(filename);
            String tmp1,tmp2,tmp3;
            String tmp="";
            while (TD.ready()) {
                tmp+=" "+TD.getString();
            }
            TD.close();
            tmp3=tmp;
            // Cut all \cite
            while (tmp.indexOf("\\cite{")>-1) {
                tmp=Parser.CutFrom(tmp,"\\cite{");
                tmp2=Parser.CutTill(tmp,"}").trim();
                while (tmp2.indexOf(",")>0) {
                    tmp1=Parser.CutTill(tmp2,",").trim();
                    if (Refs.indexOf(tmp1)==-1) Refs.add(tmp1);
                    tmp2=Parser.CutFrom(tmp2,",").trim();
                }
                if (Refs.indexOf(tmp2)==-1) Refs.add(tmp2.trim());
            }
            tmp=tmp3;
            while (tmp.indexOf("\\cite[")>-1) {
                tmp=Parser.CutFrom(tmp,"\\cite[");
                tmp=Parser.CutFrom(tmp,"{");
                tmp2=Parser.CutTill(tmp,"}").trim();
                while (tmp2.indexOf(",")>0) {
                    tmp1=Parser.CutTill(tmp2,",").trim();
                    if (Refs.indexOf(tmp1)==-1) Refs.add(tmp1);
                    tmp2=Parser.CutFrom(tmp2,",").trim();
                }
                if (Refs.indexOf(tmp2)==-1) Refs.add(tmp2.trim());
            }
            tmp=tmp3;
            // Cut all \bibitem
            while (tmp.indexOf("\\bibitem{")>-1) {
                tmp=Parser.CutFrom(tmp,"\\bibitem{");
                tmp2=Parser.CutTill(tmp,"}").trim();
                while (tmp2.indexOf(",")>0) {
                    tmp1=Parser.CutTill(tmp2,",").trim();
                    if (Refs.indexOf(tmp1)==-1) Refs.add(tmp1);
                    tmp2=Parser.CutFrom(tmp2,",").trim();
                }
                if (Refs.indexOf(tmp2)==-1) Refs.add(tmp2.trim());
            }
        } catch (IOException ex) {
            toolbox.Warning(null,"Error while reading information file:\n"+ex.toString(),"Exception:");
            ex.printStackTrace();
        }
        return(Refs);
    }

    /**
     * Turn an author string into a short string
     */
    public static String shortenNames(String authors) {
        if (authors.indexOf(",")==-1) return(authors.replaceAll("\\|",", "));
       return(Parser.CutTillLast(authors.replaceAll(", .*?\\|", ", "),",").trim());
    }

    /**
     * Turn an author string into a short string
     */
    public static String ToBibTeXAuthors(String authors) {
        return (authors.replaceAll("\\|", " and "));
    }

    /**
     * Create LaTex author from BibTeX one
     */
    public static String Authors3FromCelAuthors(String authorsin) {
        if (authorsin.indexOf(",")==-1) return(authorsin.replaceAll("\\|",", "));
        String[] authors=authorsin.split("\\|");
        String author;
        String out="";
        try {
            for(int j=0;j<authors.length;j++) {
                author=authors[j];
                if (author.indexOf(",")>-1)
                    author=Parser.CutFrom(author,",").trim()+" "+Parser.CutTill(author,",").trim();
                if (author.indexOf(".")==-1) {
                    String prenomes=Parser.CutTillLast(author," ").trim();
                    author=Parser.CutFromLast(author," ").trim();
                    int i=prenomes.lastIndexOf(" ");
                    while (i>-1) {
                        author=prenomes.substring(i+1,i+2)+". "+author;
                        prenomes=prenomes.substring(0,i).trim();
                        i=prenomes.lastIndexOf(" ");
                    }
                    author=prenomes.substring(0,1)+". "+author;
                }
                out+=author+", ";
            }
        } catch (Exception e) { out+="Error"; }
        out=Parser.CutTillLast(out,", ");
        if (out.indexOf(", ")>-1)
           out=Parser.CutTillLast(out,", ")+" and "+Parser.CutFromLast(out,", ");
        return(out);
    }

    /**
     * Create usual authors list
     */
    public static String Authors4FromCelAuthors(String authorsin) {
        String[] authors=authorsin.split("\\|");
        String author;
        String out="";
        try {
            for(int j=0;j<authors.length;j++) {
                author=authors[j];
                if (author.indexOf(",")>-1)
                    author=Parser.CutFrom(author,",").trim()+" "+Parser.CutTill(author,",").trim();
                out+=author+", ";
            }
        } catch (Exception e) { out+="Error"; }
        out=Parser.CutTillLast(out,", ");
        if (out.indexOf(", ")>-1)
           out=Parser.CutTillLast(out,", ")+" and "+Parser.CutFromLast(out,", ");
        return(out);
    }

    public static String Identifier(celsius.MProperties Information) {
	String tmp="";

	String arxref=Information.get("arxiv-ref");
	String arxname=Information.get("arxiv-name");
	if (arxref!=null) {
	  if (arxref.indexOf(arxname)>-1) {
	    tmp=arxref;
	  } else {
	    tmp=arxref+" ["+arxname+"]";
	  }
	}

        String bibtexstr = Information.get("bibtex");
	if (bibtexstr!=null) {
	  celsius.BibTeXRecord btr = new celsius.BibTeXRecord(bibtexstr);
	  if ((btr!=null) && (btr.parseError==0)) {
                tmp+=" "+btr.getIdentifier();
                String eprint=btr.get("eprint");
                if (eprint!=null) {
                    int i=eprint.indexOf("/");
                    if (i>0) {
                        if (eprint.charAt(i+1)=='9') {
                            tmp="19"+eprint.substring(i+1,i+3)+" "+tmp;
                        } else {
                            tmp="20"+eprint.substring(i+1,i+3)+" "+tmp;
                        }
                    } else {
                        tmp="20"+eprint.substring(0,2)+" "+tmp;
                    }
                } else {
                  if (btr.get("year")!=null) tmp=btr.get("year")+" "+tmp;
                }
          }
	}
	return(tmp.trim());
    }

    public static int intvalue(String s) {
        int i;
        try {
            i=Integer.valueOf(s);
        } catch (Exception e) { i=0; }
        return(i);
    }

    public static String latitudeToString(double d) {
        String tmp;
        if (d<0) {
            tmp="S";
            d=-d;
        } else tmp="N";
        DecimalFormat twoPlaces = new DecimalFormat("00");
        DecimalFormat twothreePlaces = new DecimalFormat("00.000");
        DecimalFormatSymbols dfs=DecimalFormatSymbols.getInstance();
        dfs.setDecimalSeparator('.');
        twothreePlaces.setDecimalFormatSymbols(dfs);
        tmp+=twoPlaces.format((long)Math.floor(d))+"."+twothreePlaces.format(Math.floor((d-Math.floor(d))*60*10000)/10000);
        return(tmp);
    }

    public static double StringToLatitude(String s) {
        String r=s.substring(1);
        double d=0;
        d+=Double.valueOf(Parser.CutTill(r,"."));
        d+=(Double.valueOf(Parser.CutFrom(r,"."))/60);
        if (s.charAt(0)=='S') d=-d;
        return(d);
    }

    public static String longitudeToString(double d) {
        String tmp;
        if (d<0) {
            tmp="W";
            d=-d;
        } else tmp="E";
        DecimalFormat twoPlaces = new DecimalFormat("00");
        DecimalFormat twothreePlaces = new DecimalFormat("00.000");
        DecimalFormatSymbols dfs=DecimalFormatSymbols.getInstance();
        dfs.setDecimalSeparator('.');
        twothreePlaces.setDecimalFormatSymbols(dfs);
        tmp+=twoPlaces.format((long)Math.floor(d))+"."+twothreePlaces.format(Math.floor((d-Math.floor(d))*60*10000)/10000);
        return(tmp);
    }

    public static double StringToLongitude(String s) {
        String r=s.substring(1);
        double d=0;
        d+=Double.valueOf(Parser.CutTill(r,"."));
        d+=(Double.valueOf(Parser.CutFrom(r,"."))/60);
        if (s.charAt(0)=='W') d=-d;
        return(d);
    }

    private static double sq(double d) {
        return(d*d);
    }

    public static double doubleFromDistance(String t) {
        return(Double.valueOf(Parser.CutTill(t, "km").trim()));
    }

    public static String getDistance(double lat1, double lon1, double lat2, double lon2) {
        double dlon = (lon2 - lon1)*Math.PI/180;
        double dlat = (lat2 - lat1)*Math.PI/180;
        double a = sq(Math.sin(dlat/2)) + Math.cos(lat1*Math.PI/180) * Math.cos(lat2*Math.PI/180) * sq(Math.sin(dlon/2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = 6367 * c;
        return(Double.toString(Math.floor(d*10)/10)+"km");
    }

    public static String getBearing(double lat1, double lon1, double lat2, double lon2) {
        lat1=lat1*Math.PI/180;
        lat2=lat2*Math.PI/180;
        lon1=lon1*Math.PI/180;
        lon2=lon2*Math.PI/180;
        double bear=Math.atan2(Math.sin(lon2-lon1)*Math.cos(lat2),
           Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon2-lon1));
        while (bear<0) bear+=2*Math.PI;
        while (bear>2*Math.PI) bear-=2*Math.PI;
        if (bear>15.0/8.0*Math.PI) return("N");
        if (bear>13.0/8.0*Math.PI) return("NW");
        if (bear>11.0/8.0*Math.PI) return("W");
        if (bear>9.0/8.0*Math.PI) return("SW");
        if (bear>7.0/8.0*Math.PI) return("S");
        if (bear>5.0/8.0*Math.PI) return("SE");
        if (bear>3.0/8.0*Math.PI) return("E");
        if (bear>1.0/8.0*Math.PI) return("NE");
        return("N");
    }

    public static ArrayList<String> listOf(String s) {
        ArrayList<String> ret=new ArrayList<String>();
        if ((s!=null) && (s.length()>0))
           ret.addAll(Arrays.asList(s.split("\\|")));
        return(ret);
    }

    public static String normalize(String s) {
        return(Parser.Substitute(s.toLowerCase().trim()," ",""));
    }

    public static String md5Hash(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] data = s.getBytes();
            m.update(data,0,data.length);
            BigInteger i = new BigInteger(1,m.digest());
            return(String.format("%1$032X", i));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return("Error!");
    }

    public static String stripError(String tmp) {
        return(Parser.CutFrom(tmp,"??##Error"));
    }

    /**
     * Creates a Celsius author string from a BibTeX one
     */
    public static String authorsBibTeX2Cel(String tmp) {
        String authors="";
        tmp=Parser.LowerEndOfWords(tmp);
        if (tmp.indexOf(',')>-1) {
            authors=tmp.replace(" And ","|");
        } else {
            String tmp2;
            while (tmp.length()>0) {
                tmp2=Parser.CutTill(tmp," And ").trim();
                authors+="|"+Parser.CutFromLast(tmp2," ")+", "+Parser.CutTillLast(tmp2, " ");
                tmp=Parser.CutFrom(tmp," And ").trim();
            }
            if (authors.length()>1) authors=authors.substring(1);
        }
        return(authors);
    }
    
}
