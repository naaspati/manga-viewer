package samrock;

import static java.nio.file.FileVisitResult.CONTINUE;
import static sam.swing.SwingPopupShop.hidePopup;
import static sam.swing.SwingPopupShop.popupBackground;
import static sam.swing.SwingPopupShop.popupFont;
import static sam.swing.SwingPopupShop.popupForeground;
import static sam.swing.SwingPopupShop.popupborder;
import static sam.swing.SwingPopupShop.showPopup;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.JDBC;

import sam.config.MyConfig;
import sam.di.Injector;
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.Checker;
import sam.myutils.MyUtilsPath;
import sam.nopkg.EnsureSingleton;
import samrock.api.AppConfig;


public final class Utils {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	
	private Utils(){}
	private static final Logger logger = getLogger(Utils.class);
	private static final boolean DEBUG = logger.isDebugEnabled();

	public static final Path SELF_DIR = MyUtilsPath.selfDir();
	public static final Path APP_DATA = SELF_DIR.resolve("app_data");

	public static final long START_UP_TIME = System.currentTimeMillis();
	public static final File THUMB_FOLDER = new File(MyConfig.SAMROCK_THUMBS_DIR);
	
	public static Logger getLogger(@SuppressWarnings("rawtypes") Class clazz) {
		return LoggerFactory.getLogger(clazz);
	}
	public static Logger getLogger(String loggerName) {
		return LoggerFactory.getLogger(loggerName);
	}

	/**
	 * loads<br>
	 * &emsp;SQL driver<br>
	 * &emsp;ResourceBundle<br>
	 * <br>
	 * UIManager.put("ToolTip.background", getColor("tooltip.background")); <br>
	 *	UIManager.put("ToolTip.foreground", getColor("tooltip.foreground"));<br>
	 *	UIManager.put("ToolTip.font", getFont("tooltip.font"));<br>
	 *<br>
	 * and some other work
	 * @throws ClassNotFoundException, MissingResourceException
	 */
	public static void load(AppConfig setting) throws Exception {
		singleton.init();
		
		Class.forName(JDBC.class.getCanonicalName());
		Files.createDirectories(APP_DATA);

		ImageIO.setUseCache(false);
		loadPopupLabelConstants(setting);
		FileOpenerNE.setErrorHandler((file, e) -> logger.error("failed to open file: {}", file, e));

		UIManager.put("ToolTip.background",setting.getColor("tooltip.background"));
		UIManager.put("ToolTip.foreground", setting.getColor("tooltip.foreground"));
		UIManager.put("ToolTip.font", setting.getFont("tooltip.font"));

		UIManager.put("MenuItem.font", setting.getFont("popupmenu.menuitem.font"));
		UIManager.put("MenuItem.foreground", setting.getColor("popupmenu.menuitem.forground"));
		UIManager.put("MenuItem.background", setting.getColor("popupmenu.menuitem.background"));
		UIManager.put("MenuItem.border", BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, setting.getColor("popupmenu.menuitem.separator_color")), new EmptyBorder(10, 5, 5, 5)));
	}

	private static void loadPopupLabelConstants(AppConfig setting) {
		popupFont = setting.getFont("popup.font");
		popupForeground = setting.getColor("popup.foreground");
		popupBackground  = setting.getColor("popup.background");
		int popupLabelPadding = setting.getInt("popup.padding");
		popupborder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(setting.getColor("popup.border.color"), 1, true), BorderFactory.createEmptyBorder(popupLabelPadding,popupLabelPadding,popupLabelPadding,popupLabelPadding));
	}

	public static void showHidePopup(String msg, int delay) {hidePopup(showPopup(msg), delay);}

	public static JLabel getNothingfoundlabel(String text, AppConfig setting) {
		JLabel nothingFoundLabel = new JLabel(text, JLabel.CENTER);
		nothingFoundLabel.setIcon(setting.getImageIcon("nothingfound.label.icon"));
		nothingFoundLabel.setDoubleBuffered(false);
		nothingFoundLabel.setOpaque(true);
		nothingFoundLabel.setVerticalTextPosition(SwingConstants.TOP);
		nothingFoundLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		nothingFoundLabel.setFont(setting.getFont("nothingfound.label.font"));
		nothingFoundLabel.setForeground(setting.getColor("nothingfound.label.foreground"));
		nothingFoundLabel.setBackground(setting.getColor("nothingfound.label.background"));
		return nothingFoundLabel;
	}

	//this methods saves some memory 
	//one reason i found that as ImageIO.read doesn't know actual source of image, thus this does not cache the image
	public static BufferedImage getImage(String string) {
		if(Checker.isEmptyTrimmed(string))
			return null;

		else return getImage(new File(string));
	}

	public static BufferedImage getImage(File file) {
		if(Checker.notExists(file))
			return null;

		BufferedImage img = null;

		try(InputStream is = new FileInputStream(file)) {
			img =  ImageIO.read(is);
		} catch (IOException|NullPointerException e) {
			logger.error("error while loading Image, \nsource: {}",file, e);
		}
		return img;
	}
	public static BufferedImage getImage(URL url) {
		try {
			return ImageIO.read(url);
		} catch (IOException|NullPointerException e) {
			logger.error("error while loading Image, \nurl: {}",url, e);
		}
		return null;
	}

	public static String colorToCssRGBString(Color color) {
		return String.format("rgb(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
	}

	public static JButton createButton(String iconKey, String toolTipKey, String textkey, Color textForeground,
			ActionListener actionListener, AppConfig setting) {
		JButton b = new JButton();

		if(iconKey != null)
			b.setIcon(setting.getImageIcon(iconKey));
		if(textkey != null)
			b.setText(setting.getString(textkey));

		if(toolTipKey != null)
			b.setToolTipText(setting.getString(toolTipKey));

		b.setBorderPainted(false);
		b.setContentAreaFilled(false);
		b.setFocusPainted(false);
		b.setFocusable(false);
		b.setIgnoreRepaint(true);

		if(actionListener != null)
			b.addActionListener(actionListener);
		if(textForeground != null)
			b.setForeground(textForeground);

		b.setDoubleBuffered(false);
		return b;
	}

	/**
	 * creates a JPanel with <br>
	 * p = new JPanel(false);<br>
	 * p.setOpaque(false) <br>
	 * <br>
	 * <br>
	 * if layoutManager is BoxLayout, Pass variable as new BoxLayout(null, {axis}),
	 * it will create a new BoxLayout as new BoxLayout(p, {axis}), and set to returning JPanel p
	 * 
	 * @param layoutManager
	 * @return
	 */
	public static JPanel createJPanel(LayoutManager layoutManager){
		JPanel p;

		if(layoutManager instanceof BoxLayout){
			p = new JPanel(false);
			p.setLayout(new BoxLayout(p, ((BoxLayout)layoutManager).getAxis()));
		}
		else
			p = new JPanel(layoutManager, false);

		p.setOpaque(false);

		return p;
	}

	public static void browse(String uri) {
		try {
			Desktop.getDesktop().browse(new URI(uri));
		} catch (IOException | URISyntaxException e1) {
			logger.error("Failed to open url {}", uri, e1);
		}
	}

	private static final ArrayList<Runnable> doBeforeExitList = new ArrayList<>();

	public static void removeExitTasks(Runnable r) { doBeforeExitList.remove(r); }

	/**
	 * these will be performed when {@link #exit()} 
	 * @param r
	 */
	public static void addExitTasks(Runnable r) { doBeforeExitList.add(r); }

	/**
	 * create 
	 * @param e
	 * @return createButton("popupmenu.icon", "popupmenu.tooltip", null, null, e);
	 */
	public static JButton createMenuButton(ActionListener e, AppConfig setting) {
		return createButton("popupmenu.icon", "popupmenu.tooltip", null, null, e, setting);
	}
	public static void exit() {
		doBeforeExitList.forEach(Runnable::run);
		System.exit(0);
	}

	private static final Runtime RUNTIME = Runtime.getRuntime();
	/**
	 * @return current used ram (in Mb) 
	 */
	public static long getUsedRamAmount(){
		return  (RUNTIME.totalMemory() - RUNTIME.freeMemory())/1048576L;

	}
	public static Path subpath(Path p) {
		if(p == null)
			return p;

		return p.startsWith(SELF_DIR) ? p.subpath(SELF_DIR.getNameCount(), p.getNameCount()) : p;
	}
	public static void openFile(File file) {
		FileOpenerNE.openFile(file);
	}
	public static void delete(Path file_dir) throws IOException {
		if(Files.notExists(file_dir))
			return;
		if(Files.isRegularFile(file_dir)) {
			Files.delete(file_dir);
			logger.debug("DELETED: {}",file_dir);
			return;
		}

		int count = file_dir.getNameCount();
		StringBuilder sb = DEBUG ? new StringBuilder() : null;
		if(sb != null)
			sb.append("deleting dir: ").append(file_dir).append('\n');

		Files.walkFileTree(file_dir, new SimpleFileVisitor<Path> () {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				if(sb != null)
					sb.append("  ").append(file.subpath(count, file.getNameCount()));
				return CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path file, IOException attrs) throws IOException {
				Files.delete(file);
				if(sb != null)
					sb.append("  ").append(file.subpath(count, file.getNameCount()));
				return CONTINUE;
			}
		});
		
		if(sb != null)
			logger.debug(sb.toString());
	}
	
	private static final String[] numbers = new String[100];

	public static String toString(int i) {
		if(i < 0 || i >= numbers.length)
			return Integer.toString(i);
		
		String s = numbers[i];
		if(s == null) {
			s = Integer.toString(i);
			numbers[i] = s;
		}
		
		return s;
	}
	public static AppConfig config() {
		return Injector.getInstance().instance(AppConfig.class);
	} 
}
