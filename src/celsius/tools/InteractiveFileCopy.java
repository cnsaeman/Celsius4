package celsius.tools;

import celsius.gui.MainFrame;
import celsius.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

public class InteractiveFileCopy {

    private final String src;
    private final String trg;
    private final ProgressMonitor progressMonitor;
    private final Resources RSC;

    public InteractiveFileCopy(MainFrame mf, String s, String t, Resources rsc) {
        src=s; trg=t;
        int max=(int) ((new File(src)).length()/4096/20);
        progressMonitor = new ProgressMonitor(mf, "Copying file :"+src+"\nto file :"+trg, "", 0, max);
        progressMonitor.setMillisToDecideToPopup(100);
        progressMonitor.setMillisToPopup(100);
        RSC=rsc;
    }

    public void go() {
        progressMonitor.setProgress(0);
        RSC.sequentialExecutor.submit(new Worker(src,trg));
    }

    class Worker extends SwingWorker<Void,Integer> {

        private final String src,trg;
        private Exception error;

        public Worker(String s, String t) {
            src=s; trg=t; error=null;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                int c=0;
                int t=0;
                FileInputStream fis  = new FileInputStream(new File(src));
                FileOutputStream fos = new FileOutputStream(new File(trg));
                byte[] buf = new byte[4096];
                int i;
                while((i=fis.read(buf))!=-1) {
                    fos.write(buf, 0, i);
                    c++;
                    if (c==20) {
                        c=0;
                        t++;
                        publish(t);
                    }
                }
                fis.close();
                fos.close();
            } catch (Exception e) {
                error=e;
            }
            return(null);
        }

        @Override
        public void process(List<Integer> data) {
            int copied=0;
            for (Integer i : data)
                if (copied<i) copied=i;
            progressMonitor.setProgress(copied);
        }

        @Override
        public void done() {
            progressMonitor.close();
            if (error!=null) {
                RSC.guiTools.showWarning("Warning:","Error while copying file: "+error.toString());
            }
        }
    }
}