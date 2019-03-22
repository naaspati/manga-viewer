package samrock;

import static java.nio.file.FileVisitResult.CONTINUE;
import static sam.swing.SwingPopupShop.hidePopup;
import static sam.swing.SwingPopupShop.popupBackground;
import static sam.swing.SwingPopupShop.popupFont;
import static sam.swing.SwingPopupShop.popupForeground;
import static sam.swing.SwingPopupShop.popupborder;
import static sam.swing.SwingPopupShop.showPopup;
import static samrock.RH.getColor;
import static samrock.RH.getFont;
import static samrock.RH.getImageIcon;
import static samrock.RH.getInt;
import static samrock.RH.getString;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import sam.io.fileutils.FileOpenerNE;
import sam.myutils.MyUtilsPath;


public final class Utils {
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
	public static void load() throws Exception {
		Class.forName(JDBC.class.getCanonicalName());
		Files.createDirectories(APP_DATA);

		ImageIO.setUseCache(false);
		loadPopupLabelConstants();
		FileOpenerNE.setErrorHandler((file, e) -> logger.error("failed to open file: {}", file, e));

		UIManager.put("ToolTip.background", getColor("tooltip.background"));
		UIManager.put("ToolTip.foreground", getColor("tooltip.foreground"));
		UIManager.put("ToolTip.font", getFont("tooltip.font"));

		UIManager.put("MenuItem.font", getFont("popupmenu.menuitem.font"));
		UIManager.put("MenuItem.foreground", getColor("popupmenu.menuitem.forground"));
		UIManager.put("MenuItem.background", getColor("popupmenu.menuitem.background"));
		UIManager.put("MenuItem.border", BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getColor("popupmenu.menuitem.separator_color")), new EmptyBorder(10, 5, 5, 5)));
	}

	private static void loadPopupLabelConstants() {
		popupFont = getFont("popup.font");
		popupForeground = getColor("popup.foreground");
		popupBackground  = getColor("popup.background");
		int popupLabelPadding = getInt("popup.padding");
		popupborder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getColor("popup.border.color"), 1, true), BorderFactory.createEmptyBorder(popupLabelPadding,popupLabelPadding,popupLabelPadding,popupLabelPadding));
	}

	public static void showHidePopup(String msg, int delay) {hidePopup(showPopup(msg), delay);}

	public static JLabel getNothingfoundlabel(String text) {
		JLabel nothingFoundLabel = new JLabel(text, JLabel.CENTER);
		nothingFoundLabel.setIcon(getImageIcon("nothingfound.label.icon"));
		nothingFoundLabel.setDoubleBuffered(false);
		nothingFoundLabel.setOpaque(true);
		nothingFoundLabel.setVerticalTextPosition(SwingConstants.TOP);
		nothingFoundLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		nothingFoundLabel.setFont(getFont("nothingfound.label.font"));
		nothingFoundLabel.setForeground(getColor("nothingfound.label.foreground"));
		nothingFoundLabel.setBackground(getColor("nothingfound.label.background"));
		return nothingFoundLabel;
	}

	//this methods saves some memory 
	//one reason i found that as ImageIO.read doesn't know actual source of image, thus this does not cache the image
	public static BufferedImage getImage(String string) {

		if(string == null || string.trim().isEmpty())
			return null;

		else return getImage(new File(string));
	}

	public static BufferedImage getImage(File file) {
		if(file == null || !file.exists())
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

	private static final ZoneId z = ZoneId.systemDefault();
	private static final DateTimeFormatter dataTimeFormatter = DateTimeFormatter.ofPattern("dd,MMM hh:mma");
	private static final DateTimeFormatter lastYeardateTimeFormatter = DateTimeFormatter.ofPattern("dd,MMM yyy HH:mm");
	private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mma");
	private static final LocalDate today = LocalDate.now();
	private static final LocalDate yesterday = today.minusDays(1);
	private static final int thisYear = today.getYear();

	public static String getFormattedDateTime(long time) {
		if(time < 10000)
			return "Yet To Be";

		LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), z);
		LocalDate d = dt.toLocalDate();

		if(d.equals(today))
			return "Today ".concat(dt.format(timeFormatter));
		else if(d.equals(yesterday))
			return "Yesterday ".concat(dt.format(timeFormatter));
		else if(d.getYear() == thisYear)
			return dt.format(dataTimeFormatter);
		else
			return dt.format(lastYeardateTimeFormatter);
	}

	public static JButton createButton(String iconKey, String toolTipKey, String textkey, Color textForeground,
			ActionListener actionListener) {
		JButton b = new JButton();

		if(iconKey != null)
			b.setIcon(getImageIcon(iconKey));
		if(textkey != null)
			b.setText(getString(textkey));

		if(toolTipKey != null)
			b.setToolTipText(getString(toolTipKey));

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
	public static JButton createMenuButton(ActionListener e) {
		return createButton("popupmenu.icon", "popupmenu.tooltip", null, null, e);
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
}
