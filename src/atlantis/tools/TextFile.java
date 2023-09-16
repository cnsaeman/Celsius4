/*
 Atlantis Software tools package
*/

package atlantis.tools;

import java.net.*;
import java.util.Properties;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.*;


/**
 * Convenience class for working with text files.
 * 
 * @author cnsaeman
 */
public class TextFile implements AutoCloseable {

    private final boolean canWriteTo;           // not (read only)
    private final String fileName;              // file name
    private int line;                           // current line in file
    private String lf;                          // linefeed
    private FileReader fr=null;                 // Readers for internal use.
    private BufferedReader br=null;
    private OutputStream out=null;
    
    private GZIPInputStream fis=null;
    private InputStreamReader isr=null;
    private FileOutputStream fos=null;
    
    /**
     * Open a text file for reading
     * 
     * @param fileName
     * @throws IOException 
     */
    public TextFile(String fileName) throws IOException {
        this.fileName=fileName;
        canWriteTo=false;
        fr=new FileReader(fileName);
        br=new BufferedReader(fr);
        line=1;
    }

    /**
     * Create or append a text file for writing.
     * 
     * @param fileName
     * @param append
     * @throws IOException 
     */
    public TextFile(String fileName,boolean append) throws IOException {
        this.fileName=fileName;
        canWriteTo=true;
        Properties p=System.getProperties();
        lf=p.getProperty("line.separator");
        out=new FileOutputStream(fileName,((new File(fileName)).exists() && append));
    }

    /**
     * Opens a compressed TextFile for reading.: 
     * 
     * @param fileName
     * @param compressionType - e.g. "GZIP"
     * @throws IOException 
     */
    public TextFile(String fileName,String compressionType) throws IOException {
        this.fileName=fileName;
        canWriteTo=false;
        if (compressionType.equals("GZIP")) {
            fis  = new GZIPInputStream(new FileInputStream(new File(fileName)));
            isr = new InputStreamReader(fis);
            br=new BufferedReader(isr);
        }
    }
    
    /**
     * Opens a compressed text file for writing, potentially appending
     * 
     * @param fileName
     * @param compressionType
     * @param append
     * @throws IOException 
     */
    public TextFile(String fileName,String compressionType,boolean append) throws IOException {
        this.fileName=fileName;
        canWriteTo=true;
        Properties p=System.getProperties();
        lf=p.getProperty("line.separator");
        if (compressionType.equals("GZIP")) {
            fos=new FileOutputStream(fileName,((new File(fileName)).exists() && append));
            out=new GZIPOutputStream(fos);
        }
    }
    
    /**
     * Read a text from a URL
     * 
     * @param url
     * @throws FileNotFoundException 
     */
    public TextFile(URL url) throws FileNotFoundException {
        fileName=url.getPath();
        canWriteTo=false;
        fr=new FileReader(url.getPath());
        br=new BufferedReader(fr);
        line=1;
    }
    
    /**
     * Write a string to the text file with line feed.
     * 
     * @param string
     * @throws IOException 
     */
    public void putString(String string) throws IOException {
        if (canWriteTo) {
            String tmp=string+lf;
            out.write(tmp.getBytes());
        }
        line++;
    }
    
    /**
     * Write a string to the text file without line feed.
     * 
     * @param string
     * @throws IOException 
     */
    public void putStringO(String string) throws IOException {
        if (canWriteTo) {
            String tmp=string;
            out.write(tmp.getBytes());
        }
    }
    
    /**
     * Read a string from the text file.
     * @return 
     * @throws java.io.IOException
     */
    public String getString() throws IOException {
        if (!canWriteTo) { line++;return(br.readLine()); }
        return(null);
    }

    /**
     * Read a string from the text file without checks.
     * @return 
     * @throws java.io.IOException
     */
    public String getFastString() throws IOException {
        return(br.readLine());
    }
    
    /**
     * Read a string from the text file and prevent null returns.
     * @return 
     * @throws java.io.IOException
     */
    public String getSafeString() throws IOException {
        if (!ready()) return("");
        if (!canWriteTo) { line++;return(br.readLine()); }
        return("");
    }
    
    /**
     * Indicate if a file is ready for reading
     * @return 
     * @throws java.io.IOException 
     */
    public boolean ready() throws IOException {
        return(br.ready());
    }
    
    /**
     * Returns the current line number.
     * @return 
     */
    public int getLine() {
        return(line);
    }
    
    /**
     * Returns the name of the TextDatei.
     * @return 
     */
    public String getName() {
        return(fileName);
    }
    
    /**
     * Returns true, if a file is writable
     * @return 
     */
    public boolean getWrite() {
        return(canWriteTo);
    }
    
    /**
     * Close the TextFile
     * 
     * @throws java.io.IOException
     */
    @Override
    public void close() throws IOException {
        if (out!=null) {
            out.flush();
            out.close();
        }
        if (br!=null) br.close();
        if (fr!=null) fr.close();
        if (fis!=null) fis.close();
        if (isr!=null) isr.close();
        if (fos!=null) fos.close();
    }
    
    /**
     * Copies the content of URL s1 into File s2.Returns true/false, depending on the success
     * 
     * @param urlString - the URL to read out
     * @param fileName - the name of the target file
     * @return 
     */
    public static boolean readOutURLToFile(String urlString, String fileName) {
        boolean success=true;
        InputStream in=null;
        OutputStream out=null;
        try {
            URL url=new URL(urlString);
            in=url.openStream();
            out=new FileOutputStream(fileName);
            
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                out.write(buffer,0,bytes_read);
            }
        } catch (Exception e) {
            success=false;
        } finally {
            try {
                in.close();
                out.close();
            } catch(Exception e) {}
        }
        return(success);
    }
    
    /**
     * Returns the content of a URL as a string
     * @param urlString
     * @return 
     */
    public static String readOutURL(String urlString) {
        StringBuffer out=new StringBuffer(4000);
        try {
            URL url=new URL(urlString);
            URLConnection urlconn=url.openConnection();
            urlconn.setReadTimeout(10000);
            urlconn.setConnectTimeout(10000);
            InputStream in=urlconn.getInputStream();
            
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                out.append(new String(buffer,0,bytes_read));
            }
            in.close();
        } catch (Exception e) { out=new StringBuffer("##??"+e.toString()); }
        return(out.toString());
    }
    
    /**
     * Returns the content of a file as a String
     * @param fileName
     * @return 
     */
    public static String readOutFile(String fileName) {
        StringBuffer out=new StringBuffer(4000);
        try {
            InputStream in=new FileInputStream(fileName);
            
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                out.append(new String(buffer,0,bytes_read));
            }
            in.close();
        } catch (Exception e) { out=new StringBuffer("##??"+e.toString()); }
        return(out.toString());
    }

    /**
     * Returns the content of a file as a String
     * @param string
     * @param fileName
     * @return 
     */
    public static String writeStringToFile(String string, String fileName) {
        String out="OK";
        try {
            Files.write( Paths.get(fileName), string.getBytes());
        } catch (Exception e) { out="##??"+e.toString(); }
        return(out);
    }
    
    /**
     * Returns the content of a file as a String
     * @param filename
     * @return 
     */
    public static StringBuffer readOutZipFile(String filename) {
        StringBuffer buffer=new StringBuffer();
        String tmp;
        try {
            GZIPInputStream fis = new GZIPInputStream(new FileInputStream(new File(filename)));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            while ((tmp = br.readLine()) != null) {
                buffer.append(tmp);
                buffer.append('\n');
            }
            br.close();
            isr.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (buffer);
    }
    
    /**
     * Gzips a file
     * 
     * @param fileName
     * @throws IOException 
     */
    public static void GZip(String fileName) throws IOException {
        FileInputStream fis  = new FileInputStream(new File(fileName));
        GZIPOutputStream fos = new GZIPOutputStream(new FileOutputStream(new File(fileName+".gz")));
        byte[] buf = new byte[1024];
        int i;
        while((i=fis.read(buf))!=-1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
        (new File(fileName)).delete();
    }
    /**
    * GUnzips a file
     * 
     * @param fileName
     * @throws IOException 
     */
    public static void GUnZip(String fileName) throws IOException {
        GZIPInputStream  fis = new GZIPInputStream(new FileInputStream(new File(fileName)));
        fileName=fileName.substring(0,fileName.length()-3);
        FileOutputStream fos = new FileOutputStream(new File(fileName));
        byte[] buf = new byte[1024];
        int i;
        while((i=fis.read(buf))!=-1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
        (new File(fileName+".gz")).delete();
    }
    
    /**
     * Open a zip file
     * @param fileName
     * @return 
     * @throws java.io.IOException
     */
    public static ZipOutputStream openZip(String fileName) throws IOException {
        ZipOutputStream to=new ZipOutputStream(new FileOutputStream(fileName,(new File(fileName)).exists()));
        return(to);
    }
    
    /**
     * Add a Zip entry to zip file
     * @param zipOutputStream
     * @param fileName
     * @throws java.io.IOException
     */
    public static void addToZip(ZipOutputStream zipOutputStream,String fileName) throws IOException {
        FileInputStream from=new FileInputStream(fileName);
        zipOutputStream.putNextEntry(new ZipEntry(fileName));
        byte[] buffer=new byte[4096];
        int bytes_read;
        while((bytes_read=from.read(buffer))!=-1)
            zipOutputStream.write(buffer,0,bytes_read);
        from.close();
        zipOutputStream.closeEntry();
    }
    
    /**
     * Close Zip-File
     * @param zipOutputStream
     * @throws java.io.IOException
     */
    public static void closeZip(ZipOutputStream zipOutputStream) throws IOException {
        zipOutputStream.close();
    }

    
}