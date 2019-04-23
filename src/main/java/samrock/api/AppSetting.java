package samrock.api;

import java.awt.Color;
import java.awt.Font;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

import javax.swing.ImageIcon;

import sam.myutils.System2;
import samrock.ViewElementType;

public interface AppSetting {
	/**
	 * gets corresponding value(String) to the key in ResourceBundle 
	 * @param key
	 * @return {@link String}
	 */
	String getString(String key);
	
    default String getString(String key, String defaultValue) {
        String s = getString(key);
        return s == null ? defaultValue : s;
    }
    default <E> E get(String key, E defaultValue, Function<String, E> mapper) {
        String s = getString(key);
        if(s == null)
            return defaultValue;

        return mapper.apply(s);
    }

    void setString(String key, String value);
    default boolean getBoolean(String key) {
        return getBoolean(key, false);
    }
    default boolean getBoolean(String key, boolean defaultValue) {
        return System2.parseBoolean(getString(key), defaultValue);
    }
    default int getInt(String key, int defaultValue) {
        String s = getString(key);
        if(s == null)
            return defaultValue;

        return Integer.parseInt(s.trim());
    }
    default double getDouble(String key, double defaultValue) {
        String s = getString(key);
        if(s == null)
            return defaultValue;

        return Double.parseDouble(s.trim());
    }
    
	/**
	 * gets corresponding value(Integer) to the key in ResourceBundle 
	 * @param key
	 * @return {@link Integer}
	 */
	default int getInt(String key) {
		return getInt(key, 0);
	}
	Color getColor(String key);
	Font getFont(String key);
	InputStream getStream(String key);

	/** get ImageIcon*/
	ImageIcon getImageIcon(String key);
	URL getUrl(String key);
	ViewElementType getStartupViewElementType();

}
