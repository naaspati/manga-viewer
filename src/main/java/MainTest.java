import java.io.IOException;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import sam.swing.utils.SwingUtils;

public class MainTest {
    public static void main(String[] args) {
        try {
            setSystemProperties();
        } catch (IOException e1) {
            SwingUtils.showErrorDialog("failed to load: system properties", e1);
            return;
        }
        LoggerFactory.getLogger(Main.class)
        .error("anime", new NullPointerException());
    }
    
    private static void setSystemProperties() throws IOException {
        Properties p = new Properties();
        p.load(ClassLoader.getSystemResourceAsStream("system-16884306918370.properties"));
        System.getProperties().putAll(p);
    }
    
    

}
