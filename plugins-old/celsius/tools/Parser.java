//
// Celsius Library System
// (w) by C. Saemann
//
// Parser.java
//
// This class contains the routines for parsing text.
//
// typesafe
//
// checked: 16.09.2007
//

package celsius.tools;

import java.util.regex.Pattern;

public class Parser {
    
    /**
     * Routines to treat enumlists: lists, consisting of strings separated by a comma
     */
    
    /**
     * Cut all html tags out of string s
     */
    public static String CutTags(String s) {
        int i=s.indexOf("<");
        int j=s.indexOf(">",i);
        String tmp=new String("");
        while ((i>-1) && (j>-1)) {
            tmp="";
            if (i>0) tmp=s.substring(0,i);
            if (j<s.length()-1) tmp=tmp+s.substring(j+1);
            s=tmp.trim();
            i=s.indexOf("<");
            j=s.indexOf(">",i);
        }
        return(s);
    }
    
    /**
     * public static String Substitute(String s,String e,String f)
     * Substitutes e in s by f, Deprecated, replaced with String's replace-method
     */
    public static String Substitute(String s, String e, String f) {
        int j = s.indexOf(e);
        if (j > -1) {
            String tmp = new String("");
            while (j > -1) {
                tmp += s.substring(0, j) + f;
                if (s.length() > j + e.length()) {
                    s = s.substring(j + e.length());
                } else {
                    s = new String("");
                }
                j = s.indexOf(e);
            }
            tmp += s;
            return (tmp);
        } else {
            return (s);
        }
    }
    
    public static boolean Blank(String s) {
        return ((s==null) || ("".equals(s)));
    }

    
    public static boolean HasAREF(String s) {
        String tmp=new String(s.toLowerCase());
        int i=tmp.indexOf("<a ");
        i=tmp.indexOf("href",i);
        int j=tmp.indexOf("</a>",i);
        return((i>0) && (j>0));
    }
    
    public static String CutTillAREF(String s) {
        String tmp=new String(s.toLowerCase());
        try {
            int i=tmp.indexOf("<a ");
            if (i>-1) {
                s=s.substring(i);
            } else {
                s=new String("");
            }
        } catch(Exception e) { }
        if (s.toLowerCase().indexOf("href")>s.toLowerCase().indexOf("</a>")) {
            s=CutTillAREF(s.substring(1));
        }
        return(s);
    }
    
    public static String CutAREF(String s) {
        String tmp=new String(s.toLowerCase());
        try {
            int i=tmp.indexOf("<a ");
            int j=tmp.indexOf("</a>",i);
            if (j>0) {
                s=s.substring(j+4);
            }
        } catch(Exception e) {
        }
        return(s);
    }
    
    /**
     * Substitutes problematic characters in filenames by descriptions
     */
    public static String CutProhibitedChars2(String txt) {
        if (txt.startsWith("(")) txt="~~"+txt;
        txt=txt.replace("/","~slash~");
        txt=txt.replace(":","~doublepoint~");
        txt=txt.replace("*","~asterisk~");
        txt=txt.replace("?","~questionmark~");
        txt=txt.replace("\"","~quotationmark~");
        txt=txt.replace("\\","~backslash~");
        txt=txt.replace("<","~lessthan~");
        txt=txt.replace(">","~greaterthan~");
        txt=txt.replace("|","~pipe~");
        txt=txt.replace("$","~dollar~");
        txt=txt.replace("{","~curly(~");
        txt=txt.replace("}","~curly)~");
        txt=txt.replace("&","~amp~");
        txt=txt.replace("'","~prime~");
        return(txt);
    }
    
    public static String RestoreProhibitedChars2(String txt) {
        if (txt.startsWith("~~")) txt=txt.substring(2);
        txt=txt.replace("~slash~","/");
        txt=txt.replace("~prime~","'");
        txt=txt.replace("~doublepoint~",":");
        txt=txt.replace("~asterisk~","*");
        txt=txt.replace("~questionmark~","?");
        txt=txt.replace("~quotationmark~","\"");
        txt=txt.replace("~backslash~","\\");
        txt=txt.replace("~lessthan~","<");
        txt=txt.replace("~greaterthan~",">");
        txt=txt.replace("~pipe~","|");
        txt=txt.replace("~dollar~","$");
        txt=txt.replace("~curly(~","{");
        txt=txt.replace("~amp~","&");
        txt=txt.replace("~curly)~","}");
        return(txt);
    }
    
    public static String CutProhibitedChars(String txt) {
        txt=txt.replace("/","");
        txt=txt.replace(":","");
        txt=txt.replace("*","");
        txt=txt.replace("?","");
        txt=txt.replace("\"","");
        txt=txt.replace("\\","");
        txt=txt.replace("<","");
        txt=txt.replace(">","");
        txt=txt.replace("|","");
        txt=txt.replace("$","");
        txt=txt.replace("{","");
        txt=txt.replace("&","");
        txt=txt.replace("}","");
        txt=txt.replace("\n"," ");
        return(txt);
    }
    
    /**
     * schneidet gemeinsamen Anfang ab, dann s1 zurück.
     */
    public static String CutCommonFirst(String s1,String s2) {
        StringBuffer t1=new StringBuffer(s1);
        StringBuffer t2=new StringBuffer(s2);
        while (t1.charAt(0)==t2.charAt(0)){
            t1=t1.deleteCharAt(0);
            t2=t2.deleteCharAt(0);
        }
        return(t1.toString());
    }
    
    /**
     * liefert String vor Folge s2 bzw. den vollen String bzw. leeren String zurück.
     */
    public static String CutTill(String s1,String s2) {
        int i=s1.indexOf(s2);
        if (i==-1) return(s1);
        return(new String(s1.substring(0,i)));
    }
    
    /**
     * liefert Reststring ab Folge s2 bzw. leeren String zurück.
     */
    public static String CutFrom(String s1,String s2) {
        int i=s1.indexOf(s2);
        if (i==-1) return(new String(""));
        i+=s2.length();
        return(new String(s1.substring(i)));
    }
    
    /**
     * liefert String vor Folge s2 bzw. den vollen String bzw. leeren String zurück.
     */
    public static String CutTillLast(String s1,String s2) {
        int i=s1.lastIndexOf(s2);
        if (i==-1) return(s1);
        return(new String(s1.substring(0,i)));
    }
    
    /**
     * liefert Reststring ab Folge s2 bzw. leeren String zurück.
     */
    public static String CutFromLast(String s1,String s2) {
        int i=s1.lastIndexOf(s2);
        if (i==-1) return(new String(""));
        i+=s2.length();
        return(new String(s1.substring(i)));
    }
    
    /**
     * schneidet vorangehendes s2 aus s1 aus oder liefert s1 komplett zurück.
     */
    public static String CutLeading(String s1,String s2) {
        if (s1.indexOf(s2)==0) return(s1.substring(s2.length()));
        return(s1);
    }
    
    /**
     * schneidet abschließendes s2 von s1 ab oder liefert s1 komplett zurück.
     */
    public static String CutLast(String s1,String s2) {
        if (s1.endsWith(s2)) return(s1.substring(0,s1.length()-s2.length()));
        return(s1);
    }
    
    public static String LowerPart(String s1,String s2) {
        String tmp=s1.toLowerCase();
        String target=new String("");
        int i=tmp.indexOf(s2);
        int j=0;
        if (i<0) return(s1);
        while (i>-1){
            target+=s1.substring(j,i);
            target+=s2;
            j=i+s2.length();
            i=tmp.indexOf(s2,i+1);
        }
        if (j<s1.length()) target+=s1.substring(j);
        return(target);
    }
    
    public static String CutLastChars(String s,int i) {
        if (s.length()<i) return(s);
        return(s.substring(0,s.length()-i));
    }
    
    public static boolean IsCapitalized(String s1) {
        return (s1.equals(s1.toUpperCase()));
    }
    
    public static String LowerEndOfWords(String s1) {
        String spaces=new String(" {=,::.!-(\"");
        String tmp=s1.toUpperCase();
        String target=new String("");
        int i;
        if (tmp.length()<1) return(new String("<empty>"));
        target+=tmp.substring(0,1);
        for(i=1;i<tmp.length();i++) {
            if (spaces.indexOf(tmp.substring(i-1,i))==-1) {
                target+=tmp.substring(i,i+1).toLowerCase();
            } else {
                target+=tmp.substring(i,i+1);
            }
        }
        target=target.replace("Ii","II");
        target=target.replace("Iii","III");
        return(target);
    }

    public static String LowerEndOfWords2(String s1) {
        String spaces=new String("={}.:;!'-(\"");
        String tmp=s1.toUpperCase();
        String target=new String("");
        int i;
        if (tmp.length()<1) return(new String("<empty>"));
        target+=tmp.substring(0,1);
        for(i=1;i<tmp.length();i++) {
            if ((spaces.indexOf(tmp.substring(i-1,i))==-1) && 
                !((tmp.charAt(i-1)==' ') && (spaces.indexOf(tmp.substring(i-2,i-1))>-1))) {
                target+=tmp.substring(i,i+1).toLowerCase();
            } else {
                target+=tmp.substring(i,i+1);
            }
        }
        target=target.replace("Ii","II");
        target=target.replace("Iii","III");
        return(target);
    }
    
    
    public static String CutAREFURL(String s) {
        String tmp=new String(s.toLowerCase());
        try {
            int i=tmp.indexOf("<a ");
            i=tmp.indexOf("href",i);
            int k=tmp.indexOf("=",i)+1;
            int j=tmp.indexOf(">",k);
            return(s.substring(k,j).replace("\"",""));
        } catch(Exception e) {
        }
        return("");
    }
    
    public static String CutAREFTXT(String s) {
        String tmp=new String(s.toLowerCase());
        int i=tmp.indexOf("<a ");
        int k=tmp.indexOf("<a",i);
        k=tmp.indexOf(">",k+1);
        int j=tmp.indexOf("</a>",k);
        if (j==-1) return("");
        String txt;
        try {
            txt=new String(s.substring(k+1,j).trim());
            txt=CutTags(txt).trim();
        } catch(Exception e) { txt=e.getMessage(); }
        return(txt);
    }
    
    public static String ExtractInBrackets(String s1) {
        String tmp=s1;
        String tmp2=new String("");
        while (tmp.length()>0) {
            tmp=CutFrom(tmp,"(");
            tmp2+=CutTill(tmp,")");
        }
        return(tmp2);
    }
    
    public static String ExtractInBracketsWS(String s1) {
        String tmp=s1;
        String tmp2=new String("");
        while (tmp.length()>0) {
            tmp=CutFrom(tmp,"(");
            tmp2+=CutTill(tmp,")")+" ";
        }
        return(tmp2.trim());
    }
    
    public static String CutBrackets(String s1) {
        String tmp=s1;
        String tmp2=new String("");
        while (tmp.length()>0) {
            tmp2+=CutTill(tmp,"(");
            tmp=CutFrom(tmp,")");
        }
        return(tmp2);
    }
    
    // Counts number of occurences of a substring. cleaned 1
    public static int HowOftenContains(String s1,String s2) {
        int j=0; // number of occurences
        int k=s2.length(); // length of big string
        int i=-k; // starting point
        while ((i=s1.indexOf(s2,i+k))>-1)
            j++;
        return(j);
    }
    
    public static String ReturnFirstItem(String s1) {
        if (s1.length()==0) return(s1);
        String tmp;
        if (s1.charAt(0)=='"') {
            s1=CutFrom(s1,"\"");
            tmp=CutTill(s1,"\"");
        } else { tmp=CutTill(s1,"|"); }
        return(tmp.trim());
    }
    
    public static String CutFirstItem(String s1) {
        if (s1.length()==0) return(s1);
        String tmp;
        if (s1.charAt(0)=='"') {
            s1=CutFrom(s1,"\"");
            tmp=CutFrom(s1,"\" ");
        } else { tmp=CutFrom(s1,"|"); }
        return(tmp.trim());
    }
    
    public static boolean HasMoreItems(String s1) {
        return(s1.trim().length()>0);
    }
    
    public static String decodeHTML(String s1) {
        s1=s1.replaceAll(Pattern.quote("&quot;"),"\"");
        s1=s1.replaceAll(Pattern.quote("&lt;"),"<");
        s1=s1.replaceAll(Pattern.quote("&gt;"),">");
        s1=s1.replaceAll(Pattern.quote("&amp;"),"&");
        int i=s1.indexOf("&#x");
        while (i>-1) {
            int j=s1.indexOf(";",i);
            String sub=s1.substring(i+3,j);
            char c=(char)((int)(Integer.parseInt(sub,16)));
            s1=s1.substring(0,i)+c+s1.substring(j+1);
            i=s1.indexOf("&#x");
        }
        return(s1);
    }
    
    /**
     * Returns true if EnumList s1 contains s2
     */
    public static boolean EnumContains(String s1,String s2) {
        if (s1==null) return(false);
        if (s1.length()<s2.length()) return(false);
        if (s1.equals(s2)) return(true);
        if (s1.indexOf("|"+s2+"|")>-1) return(true);
        if (s1.startsWith(s2+"|")) return(true);
        if (s1.endsWith("|"+s2)) return(true);
        return(false);
    }

    /**
     * Returns true if EnumList s1 contains s2
     */
    public static boolean EnumContains2(String s1,String s2) {
        if (s1.equals(s2)) return(true);
        if (s1.indexOf("|"+s2+",")>-1) return(true);
        if (s1.startsWith(s2+",")) return(true);
        return(false);
    }
    
    /**
     * Replace s2 in EnumList s1 by s3
     */
    public static String EnumReplace(String s1,String s2,String s3) {
        s1="|"+s1+"|";
        s1=s1.replace("|"+s2+"|","|"+s3+"|");
        s1=s1.substring(1,s1.length()-1);
        return(s1);
    }
    
    /**
     * Deletes string s2 from EnumList s1
     */
    public static String EnumDelete(String s1,String s2) {
        s1="|"+s1+"|";
        s1=s1.replace(s2+"|","");
        if (s1.equals("|") || s1.equals("||")) return(new String(""));
        s1=s1.substring(1,s1.length()-1);
        return(s1);
    }
    
}
