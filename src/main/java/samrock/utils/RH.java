package samrock.utils;

import static samrock.manga.maneger.SortingMethod.*;

import java.awt.Color;
import java.awt.Font;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import samrock.manga.maneger.SortingMethod;

/**
 * ResourceHandler
 * 
 * @author Sameer
 *
 */
public final class RH {
    private  RH() {}
    private static final ResourceBundle bundle;
    private static final Properties config = new Properties();
    private static Logger logger = Logger.getLogger("ResourceHandler");

    static {
        bundle = ResourceBundle.getBundle("settings-16884306918370");
        
        try {
            config.load(Files.newInputStream(Paths.get("config.properties")));
        } catch (Exception e) {}
    }

    public static void putConfig(String key, String value) {
        config.put(key, value);
    }

    public static String getConfig(String key) {
        return config.getProperty(key);
    }
    /**
     * gets corresponding value(String) to the key in ResourceBundle 
     * @param key
     * @return {@link String}
     */
    public static String getString(String key){return bundle.getString(key);}


    /**
     * gets corresponding value(Integer) to the key in ResourceBundle 
     * @param key
     * @return {@link Integer}
     */
    public static int getInt(String key){
        int value  = 0;
        try {
            value = Integer.parseInt(getString(key).trim());
        } catch (NumberFormatException e) {
           logger.log(Level.WARNING, "Error occerred during parsing value of "+key, e);
        }
        return value;
    }

    public static Color getColor(String key){return Color.decode(getString(key));}

    public static Font getFont(String key){
        String str = getString(key);
        try {
            String[] s = str.trim().split("\\|");
            if(s.length != 3)
                throw new IllegalArgumentException("Error while processing font for key: "+key);
            String name = s[0].trim().isEmpty() || s[0].trim().equalsIgnoreCase("null") ? null : s[0].trim();
            int style = Integer.parseInt(s[1].trim());
            int size = Integer.parseInt(s[2].trim());

            return new Font(name, style, size);
        } catch (IllegalArgumentException|NullPointerException e) {
            logger.log(Level.WARNING, "Error while parsing Font for key: "+key+"\tvalue: "+str, e);
            return new Font(null, 1, 200);
        }
    }
    public static InputStream getStream(String key) {
        return ClassLoader.getSystemResourceAsStream(getString(key));
    }

    /** get ImageIcon*/
    public static ImageIcon getImageIcon(String key) {
        return new ImageIcon(getUrl(key));
    }
    public static URL getUrl(String key) {
        return ClassLoader.getSystemResource(getString(key));
    }
    
    public static ViewElementType getStartupViewElementType() {
        return getString("app.startup.view").trim().equalsIgnoreCase("list") ? ViewElementType.LIST : ViewElementType.THUMB;
    }

    public static SortingMethod getStartupSortingMethod() {
        switch (getInt("app.startup.sorting")) {
        case 0:
            return ALPHABETICALLY_INCREASING;
        case 1:
            return RANKS_INCREASING;
        case 2:
            return READ_TIME_DECREASING;
        case 3:
            return UPDATE_TIME_DECREASING;
        default:
            return ALPHABETICALLY_INCREASING;
        }
    }


}
