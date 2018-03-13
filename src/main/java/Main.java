

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.MissingResourceException;

import javax.swing.JOptionPane;

import org.slf4j.LoggerFactory;

import samrock.gui.SamRock;
import samrock.manga.maneger.MangaManeger;
import samrock.utils.Utils;

public class Main {
    public static final double VERSION = 7.2;

    public static void main(String[] args) {
        System.setProperty("java.util.logging.config.file","logging.properties");
        new File("logs").mkdirs();
        
        System.out.println("JVM_ARGS");
        ManagementFactory.getRuntimeMXBean().getInputArguments().forEach(s -> System.out.println("\t"+s));
        
        File file = new File("running");
        if(file.exists()){
            JOptionPane.showMessageDialog(null, "Already Running", "One app is allowed", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        } else{
            try {
                Files.createFile(file.toPath());
                file.deleteOnExit();
            } catch (IOException e) {	
                JOptionPane.showMessageDialog(null, e, "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        try {
            Utils.load();
            MangaManeger.createInstance();
            SamRock sam = new SamRock(VERSION);
            sam.setVisible(true);
            while(!sam.isShowing()){}
        } catch (ClassNotFoundException | MissingResourceException | SQLException | InstantiationException | IllegalAccessException | IOException e) {
            LoggerFactory.getLogger(Main.class).error("Error Caught in MainMethod, App Will close", e);
            return;
        }
    }
}
