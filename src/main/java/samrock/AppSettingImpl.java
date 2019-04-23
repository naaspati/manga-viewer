package samrock;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.swing.ImageIcon;

import org.slf4j.Logger;

import sam.myutils.System2;
import samrock.api.AppSetting;

/**
 * ResourceHandler
 * 
 * @author Sameer
 *
 */
abstract class AppSettingImpl implements AppSetting  {
	private static final Path modifiable_path;
	private static boolean mod;
	private static final Properties modifiable = new Properties();

	private static final Logger logger = Utils.getLogger(AppSettingImpl.class);
	private static Callable<Properties> bundle_getter;

	private final Properties bundle;

	static {
		modifiable_path = Paths.get(System2.lookup("samrock_app_settings_modifiable", "samrock_app_settings_modifiable.properties"));
		if(Files.exists(modifiable_path)) {
			try {
				modifiable.load(Files.newInputStream(modifiable_path, StandardOpenOption.READ));
			} catch (IOException e) {
				logger.error("failed to read: {}", modifiable_path, e);
			}
		}
	}

	public AppSettingImpl() throws Exception {
		Properties bundle = null;

		try {
			if(bundle_getter != null) {
				bundle = bundle_getter.call();
			} else {

				Callable<Properties> callable = null;
				String s = System2.lookup("samrock_app_settings");

				if(s != null)
					callable = callable(Paths.get(s));

				if(callable == null)
					callable = callable(Paths.get("samrock_app_settings.properties"));

				if(callable == null)
					callable = clsLoaded();

				bundle_getter = callable == null ? Properties::new : callable;
				bundle = bundle_getter.call();
			}
		} catch (Throwable e) {
			logger.error("failed to load {}",getClass(), e);
		}

		if(bundle == null) {
			bundle_getter = Properties::new;
			this.bundle = new Properties();
		} else {
			this.bundle = bundle;
		}
	}


	private Callable<Properties> clsLoaded() {
		URL u = ClassLoader.getSystemResource("samrock_app_settings.properties"); 
		if(u == null)
			return null;
		else  {
			logger.debug("setting source: {}", u);

			return () -> {
				Properties props = new Properties();
				props.load(ClassLoader.getSystemResourceAsStream("samrock_app_settings.properties"));
				return props;
			};
		}
	}


	private Callable<Properties> callable(Path p) {
		if(Files.notExists(p)) {
			logger.error("path not found (samrock_app_settings): {}", p);
			return null;
		} else {
			logger.debug("setting source: {}", p);
			return () -> {
				Properties props = new Properties();
				props.load(Files.newInputStream(p, StandardOpenOption.READ));
				return props;
			};
		}
	}

	@Override
	public Color getColor(String key){return Color.decode(getString(key));}

	@Override
	public Font getFont(String key){
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
			logger.warn("Error while parsing Font for \"{}\"=\"{}\"", key, str, e);
			return new Font(null, 1, 200);
		}
	}

	@Override
	public String getString(String key) {
		String s = modifiable.isEmpty() ? null : modifiable.getProperty(key);
		return s != null ? s : bundle.getProperty(key);
	}

	@Override
	public void setString(String key, String value) {
		Object o = modifiable.put(value, key);
		mod = mod || !Objects.equals(value, o);
	}
	@Override
	public InputStream getStream(String key) {
		return ClassLoader.getSystemResourceAsStream(getString(key));
	}

	/** get ImageIcon*/
	@Override
	public ImageIcon getImageIcon(String key) {
		return new ImageIcon(getUrl(key));
	}
	@Override
	public URL getUrl(String key) {
		return ClassLoader.getSystemResource(getString(key));
	}
	@Override
	public ViewElementType getStartupViewElementType() {
		return getString("app.startup.view").trim().equalsIgnoreCase("list") ? ViewElementType.LIST : ViewElementType.THUMB;
	}
}
