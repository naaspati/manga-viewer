package samrock.utils;

import static samrock.utils.RH.getColor;
import static samrock.utils.RH.getFont;
import static samrock.utils.RH.getImageIcon;
import static samrock.utils.RH.getInt;
import static samrock.utils.RH.getString;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.JDBC;

public final class Utils {
    private Utils(){}

    private static Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final long START_UP_TIME = System.currentTimeMillis();
    private static final Runtime RUNTIME = Runtime.getRuntime();
    /**
     * popup_id -> popup
     */
    private static final PopupFactory popupFactory = PopupFactory.getSharedInstance();


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
    public static void load() throws ClassNotFoundException {
        Class.forName(JDBC.class.getCanonicalName());

        ImageIO.setUseCache(false);
        loadPopupLabelConstants();

        UIManager.put("ToolTip.background", getColor("tooltip.background"));
        UIManager.put("ToolTip.foreground", getColor("tooltip.foreground"));
        UIManager.put("ToolTip.font", getFont("tooltip.font"));

        UIManager.put("MenuItem.font", getFont("popupmenu.menuitem.font"));
        UIManager.put("MenuItem.foreground", getColor("popupmenu.menuitem.forground"));
        UIManager.put("MenuItem.background", getColor("popupmenu.menuitem.background"));
        UIManager.put("MenuItem.border", BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getColor("popupmenu.menuitem.separator_color")), new EmptyBorder(10, 5, 5, 5)));
    }

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

        else return getImage(Paths.get(string));
    }

    public static BufferedImage getImage(Path path) {
        if(path == null || Files.notExists(path))
            return null;

        BufferedImage img = null;

        try(InputStream is = Files.newInputStream(path)) {
            img =  ImageIO.read(is);
        } catch (IOException|NullPointerException e) {
            logger.error("error while loading Image, \nsource: "+path, e);
        }
        return img;
    }
    public static BufferedImage getImage(URL url) {
        try {
            return ImageIO.read(url);
        } catch (IOException|NullPointerException e) {
            logger.error("error while loading Image, \nurl: "+url, e);
        }
        return null;
    }

    public static String colorToCssRGBString(Color color) {
        return String.format("rgb(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private static Component popupRelativeTo;

    /**
     *  
     * @param component further popups will shown relative to this component (if visible , else will show in center of screen)
     */
    public static void setPopupRelativeTo(Component component) {
        popupRelativeTo = component;
    }

    private static Font popupFont;
    private static Color popupForeground;
    private static Color popupBackground;
    private static Border popupborder;

    private static void loadPopupLabelConstants(){
        popupFont = getFont("popup.font");
        popupForeground = getColor("popup.foreground");
        popupBackground  = getColor("popup.background");
        int popupLabelPadding = getInt("popup.padding");
        popupborder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getColor("popup.border.color"), 1, true), BorderFactory.createEmptyBorder(popupLabelPadding,popupLabelPadding,popupLabelPadding,popupLabelPadding));
    }

    private static JLabel getPopupJLabel(String text){
        JLabel popupLabel = new JLabel(text, JLabel.CENTER);
        popupLabel.setFont(popupFont);
        popupLabel.setBackground(popupBackground);
        popupLabel.setForeground(popupForeground);
        popupLabel.setOpaque(true);
        popupLabel.setDoubleBuffered(false);
        popupLabel.setBorder(popupborder);
        return popupLabel;
    }

    private static Popup[] currentPopups = new Popup[10];
    private static int currentIndex = 0;
    /**
     * 
     * @param msg shows a popup with given msg
     * @return popupId which is used in {@link #hidePopup(popupId, delay)}
     */
    public static Integer showPopup(String msg) {
        JLabel popupLabel = getPopupJLabel(msg);

        currentIndex++;
        if(currentIndex >= 10)
            currentIndex = 0;

        if(currentPopups[currentIndex] != null)
            hidePopup(currentIndex, 0);

        int x = (popupRelativeTo == null  || !popupRelativeTo.isVisible()? SCREEN_SIZE.width/2 : (popupRelativeTo.getLocation().x + popupRelativeTo.getWidth()/2)) - popupLabel.getPreferredSize().width/2; 
        int y = (popupRelativeTo == null || !popupRelativeTo.isVisible() ? SCREEN_SIZE.height/2 : (popupRelativeTo.getLocation().y + popupRelativeTo.getHeight()/2)) - popupLabel.getPreferredSize().height/2;

        (currentPopups[currentIndex] = popupFactory.getPopup( popupRelativeTo, popupLabel, x, y)).show();

        return currentIndex;
    }

    public static void showHidePopup(String msg, int delay) {hidePopup(showPopup(msg), delay);}

    public static void hidePopup(int popupId, int delay){
        if(currentPopups[popupId] == null)
            return;
        if(delay == 0)
            currentPopups[popupId].hide();
        else{
            final Popup p = currentPopups[popupId];

            Timer t = new Timer(delay, e -> EventQueue.invokeLater(p::hide));
            t.start();
            t.setRepeats(false);
            t = null;
        }

        currentPopups[popupId] = null;
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

    /**
     * 
     * @param name
     * @return null if name is null at start, or empty after removing bad characters and trimming, else will return processed name 
     */
    public static String removeInvalidCharsFromFileName(String name){
        if(name != null && !name.trim().isEmpty()){
            name = name
                    .replaceAll("[\\Q<>:\"/*|?\\E]", " ")//window researved keywords
                    .replaceAll("\\s+", " ") //remove all space characters except normal single space (" ")
                    .trim()
                    .replaceFirst("\\.+$", ""); //replace dot char at the end of name, as if it is left, then in naming folder or file windows removes it, and file path which contains the dot at the end will give error;

            return name.trim();
        }
        else return null;


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
    public static boolean openFile(File file){
        if(!file.exists()){
            logger.error("File Not  Found: "+file);
            return false;
        }

        try {
            Desktop.getDesktop().open(file);
            return true;
        } catch (IOException e) {
            logger.error("failed to open file: "+file, e);
            return false;
        }
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
            logger.error("Failed to open url".concat(uri), e1);
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

    /**
     * @return current used ram (in Mb) 
     */
    public static long getUsedRamAmount(){
        return  (RUNTIME.totalMemory() - RUNTIME.freeMemory())/1048576L;

    } 
}
