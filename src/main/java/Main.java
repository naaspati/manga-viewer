

import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import sam.io.fileutils.FilesUtilsIO;
import sam.logging.MyLoggerFactory;
import samrock.gui.SamRock;
import samrock.utils.Utils;

public class Main {
    public static void main(String[] args) {
        System.out.println("JVM_ARGS");
        ManagementFactory.getRuntimeMXBean().getInputArguments().forEach(s -> System.out.println("  "+s));
        
        try {
			FilesUtilsIO.createFileLock(Paths.get("samrock.lock"));
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Already Running", "One app is allowed", JOptionPane.WARNING_MESSAGE);
			System.exit(0);
		}
        try {
            Utils.load();
            SamRock sam = new SamRock(System.getenv("APP_VERSION"));
            sam.setVisible(true);
            // while(!sam.isShowing()){}
        } catch (Exception e) {
            MyLoggerFactory.logger(Main.class).log(Level.SEVERE, "Error Caught in MainMethod, App Will close", e);
            return;
        }
    }
}
