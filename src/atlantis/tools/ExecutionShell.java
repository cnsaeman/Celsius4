//
// Celsius Library System v2
// (w) by C. Saemann
//
// ExecutionShell.java
//
// This class contains the main frame and the main class of the Celsius Library System
//
// typesafe
//
// checked 16.09.2007
//

package atlantis.tools;

import atlantis.tools.TextFile;
import atlantis.tools.Parser;
import java.util.ArrayList;

public class ExecutionShell extends Thread {
    
    public String[] transcommand;
    public String output;               // the output of the program started
    public String errors;               // the error messages of the program started
    public boolean errorflag;           // indicates, whether an error has occured
    public String errorMsg;             // the message of the error
    private String outputFileName;      // target file
    private boolean redirectoutput;     // should output be redirected?
    private final boolean letgo;              // should the executed command be monitored further?
    
    /**
     * Constructor (command, priority)
     * To optimize
     */
    public ExecutionShell(String c,int priority,boolean lg) {
        letgo=lg;
        try {
            if (c.indexOf(">")>-1) {
                outputFileName=Parser.cutFrom(c,">");
                c=Parser.cutUntil(c,">").trim();
                redirectoutput=true;
            } else redirectoutput=false;
            
            // turn command string into command array
            ArrayList<String> cmds=new ArrayList<String>();
            while (c.length()>0) {
                if (c.startsWith("\"")) {
                    int i=c.indexOf("\" ");
                    if (i>-1) while (c.charAt(i-1)=='\\') i=c.indexOf("\" ",i+1);
                    if (i>0) {
                        cmds.add(Parser.replace(c.substring(1,i),"\\\"","\""));
                        c=c.substring(i+1).trim();
                    } else {
                        cmds.add(Parser.replace(Parser.cutUntilLast(c.substring(1),"\""),"\\\"","\""));
                        c="";
                    }
                } else if (c.startsWith("'")) {
                    int i=c.indexOf("' ");
                    if (i>-1) while (c.charAt(i-1)=='\\') i=c.indexOf("' ",i+1);
                    if (i>0) {
                        cmds.add(Parser.replace(c.substring(1,i),"\\'","'"));
                        c=c.substring(i+1).trim();
                    } else {
                        cmds.add(Parser.replace(Parser.cutUntilLast(c.substring(1),"'"),"\\'","'"));
                        c="";
                    }
                } else {
                    cmds.add(Parser.cutUntil(c," "));
                    c=Parser.cutFrom(c," ");
                }
            }
            transcommand=new String[cmds.size()];
            for (int i=0;i<cmds.size();i++)
                transcommand[i]=cmds.get(i);
            output="";
            errorflag=false;
            errorMsg="";
            if (priority==0) this.setPriority(Thread.NORM_PRIORITY);
            if (priority==-1) this.setPriority(Thread.MIN_PRIORITY);
            if (priority==+1) this.setPriority(Thread.MAX_PRIORITY);
        } catch(Exception e) { e.printStackTrace(); }
    }
    
    /**
     * Execute the command
     */
    @Override
    public void start() {
        try {
            Process p = Runtime.getRuntime().exec(transcommand);
            StreamGobbler SGout=new StreamGobbler(p.getInputStream(),"Output");
            StreamGobbler SGerr=new StreamGobbler(p.getErrorStream(),"Error");
            SGout.start();
            SGerr.start();
            if (letgo) return;
            boolean completed=false;
            int m=0;
            // go through loop at most 600 times=2 mins o
            while ((!completed) && (m<600)) {
                try {
                    sleep(200); p.exitValue(); completed=true;
                } catch (IllegalThreadStateException e) { m++;  }
            }
            output=SGout.getOutput();
            errors=SGerr.getOutput();
            if (!completed) {
                p.destroy();
                errors+="not completed!";
            }
            if (redirectoutput) {
                TextFile out=new TextFile(outputFileName,false);
                out.putString(output);
                out.close();
            }
        } catch (Exception e){
            e.printStackTrace();
            errorflag=true;
            errorMsg=e.toString();
        }
    }
        
}

