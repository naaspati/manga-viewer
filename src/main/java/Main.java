

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.lang.management.ManagementFactory;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;

import javax.swing.JOptionPane;

import samrock.Utils;
import samrock.gui.SamRock;

public class Main {
    public static void main(String[] args) {
        System.out.println("JVM_ARGS");
        ManagementFactory.getRuntimeMXBean().getInputArguments().forEach(s -> System.out.println("  "+s));
        
        try {
        	FileChannel fc = FileChannel.open(Paths.get("samrock.lock"), CREATE, WRITE, READ);
			FileLock lock = fc.tryLock();
			
			if(lock == null) {
				JOptionPane.showMessageDialog(null, "Already Running", "One app is allowed", JOptionPane.WARNING_MESSAGE);
				return;
			}
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
            Utils.getLogger(Main.class).error("Error Caught in MainMethod, App Will close", e);
            return;
        }
    }
}
