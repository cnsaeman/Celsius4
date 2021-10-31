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

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ToolBox {
    
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
     *  Returns current timestamp
     */
    public static String getCurrentDate() {
        Date ActD=new Date();
        return(ActD.toString());
    }

    public static String getFirstPage(String s) {
        if (s==null) return("");
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
    

    public static String Identifier(celsius.data.MProperties Information) {
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
	  celsius.data.BibTeXRecord btr = new celsius.data.BibTeXRecord(bibtexstr);
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
        d+=Double.valueOf(Parser.cutUntil(r,"."));
        d+=(Double.valueOf(Parser.cutFrom(r,"."))/60);
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
        d+=Double.valueOf(Parser.cutUntil(r,"."));
        d+=(Double.valueOf(Parser.cutFrom(r,"."))/60);
        if (s.charAt(0)=='W') d=-d;
        return(d);
    }

    private static double sq(double d) {
        return(d*d);
    }

    public static double doubleFromDistance(String t) {
        return(Double.valueOf(Parser.cutUntil(t, "km").trim()));
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
        return(Parser.replace(s.toLowerCase().trim()," ",""));
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
        return(Parser.cutFrom(tmp,"??##Error"));
    }

    /**
     * Creates a Celsius author string from a BibTeX one
     */
    public static String authorsBibTeX2Cel(String tmp) {
        String authors="";
        tmp=Parser.lowerEndOfWords(tmp);
        if (tmp.indexOf(',')>-1) {
            authors=tmp.replace(" And ","|");
        } else {
            String tmp2;
            while (tmp.length()>0) {
                tmp2=Parser.cutUntil(tmp," And ").trim();
                authors+="|"+Parser.cutFromLast(tmp2," ")+", "+Parser.cutUntilLast(tmp2, " ");
                tmp=Parser.cutFrom(tmp," And ").trim();
            }
            if (authors.length()>1) authors=authors.substring(1);
        }
        return(authors);
    }
    
    public static long now() {
        return (System.currentTimeMillis()/1000);
    }

}
