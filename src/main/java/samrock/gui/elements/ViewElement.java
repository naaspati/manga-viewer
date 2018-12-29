package samrock.gui.elements;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import sam.logging.MyLoggerFactory;
import sam.myutils.MyUtilsExtra;
import samrock.manga.MinimalListManga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.IconManger;
import samrock.manga.maneger.MangaManeger;
import samrock.manga.maneger.MangasOnDisplay;
import samrock.manga.recents.MinimalChapterSavePoint;
import samrock.utils.RH;
import samrock.utils.Utils;
import samrock.utils.ViewElementType;

final class ViewElement extends JLabel {
	private static final Logger LOGGER = MyLoggerFactory.logger(ViewElement.class);
    
    private static final long serialVersionUID = -1304445825214964125L;
    private static final Color MANGA_DELETED_BACKGROUND;

    private static ViewElementType currentElementType ;
    public static void setCurrentElementType(ViewElementType currentElementType) {
        ViewElement.currentElementType = currentElementType;
    }

    /* *****************************************************************************
     * *************************THUMB ELEMENT***************************************
     * *****************************************************************************
     */

    private static final Color THUMB_NORMAL_BACKGROUND;
    private static final Color THUMB_INFOCUS_BACKGROUND;

    private static final Color THUMB_FOREGROUND;

    private static final Color THUMB_UNREAD_STICKER_BACKGROUND;
    private static final Color THUMB_UNREAD_STICKER_FOREGROUND;

    private static final Font THUMB_UNREAD_STICKER_FONT;
    private static final int THUMB_UNREAD_STICKER_WIDTH;

    private static final ImageIcon THUMB_ICON_NOT_FOUND_ICON;
    private static final ImageIcon THUMB_ICON_LOADING_ICON;

    /**
     * 1 manga_name %s
     */
    private static final String THUMB_FORMATTING_STRING;
    private static final String THUMB_BLANK_STRING;

    /* *****************************************************************************
     * *************************LIST ELEMENT****************************************
     * *****************************************************************************
     */

    private static final Color LIST_NORMAL_FOREGROUND;

    private static final Color LIST_NORMAL_BACKGROUND; 
    private static final Color LIST_INFOCUS_BACKGROUND;

    private static final Border LIST_DEFAULT_BORDER;

    private static final ImageIcon LIST_ICON_NOT_FOUND_ICON;
    private static final ImageIcon LIST_ICON_LOADING_ICON;

    /**<pre>
     * 1 manga_name %s
     * 2 Author %s
     * 3 Rank  %d
     * 4 chapters_in_mangarock %d
     * 5 chapter_in_pc %d
     * 6 status %s
     * 7 favorite %s
     * 8 read count %d
     * 9 unread count %d
     * </pre>
     */
    private static final String LIST_FORMATTING_STRING;
    private static final String LIST_BLANK_STRING;

    /* *****************************************************************************
     * *************************RECENT ELEMENT****************************************
     * *****************************************************************************
     */

    /**<pre>
     * 1 manga_name %s
     * 2 Author %s
     * 3 Rank  %d
     * 4 chapters_in_mangarock %d
     * 5 chapter_in_pc %d
     * 6 status %s
     * 7 favorite %s
     * 8 read count %d
     * 9 unread count %d
     * 10 formatted recent time %s
     * 11 chapter_name %s
     * </pre>
     */
    private static final String RECENT_LIST_FORMATTING_STRING;
    private static final String RECENT_LIST_BLANK_STRING;

    /**<pre>
     * 1 manga_name %s
     * 2 formatted recent time %s
     * 3 chapter_name %s
     * </pre>
     */
    private static final String RECENT_THUMB_FORMATTING_STRING;
    private static final String RECENT_THUMB_BLANK_STRING;

    private static final ImageIcon RECENT_LIST_ICON_NOT_FOUND_ICON;
    private static final ImageIcon RECENT_LIST_ICON_LOADING_ICON;

    private static final IconManger iconManger;
    private static final MangasOnDisplay MOD;

    private static MouseListener mouseListener; 
    private static KeyListener keyListener;
    private static FocusListener focusListener;

    static void initConstants(MouseListener mouseAdapter, KeyListener keyAdapter, FocusListener focusListener) {
        ViewElement.mouseListener = mouseAdapter;  
        ViewElement.keyListener = keyAdapter; 
        ViewElement.focusListener = focusListener;
    }

    static {
        iconManger = IconManger.getInstance();
        MOD  = MangaManeger.getMangasOnDisplay();

        Font THUMB_NAME_LABEL_FONT = RH.getFont("thumbview.namelabel.font");

        THUMB_NORMAL_BACKGROUND = RH.getColor("thumbview.background.normal");
        THUMB_INFOCUS_BACKGROUND = RH.getColor("thumbview.background.infoucs");
        THUMB_FOREGROUND = RH.getColor("thumbview.namelabel.foreground");
        MANGA_DELETED_BACKGROUND = RH.getColor("deleted.manga.background"); 

        THUMB_UNREAD_STICKER_WIDTH = RH.getInt("thumbview.unread.sticker.width");

        THUMB_UNREAD_STICKER_BACKGROUND = RH.getColor("thumbView.unread_sticker.background");
        THUMB_UNREAD_STICKER_FOREGROUND = RH.getColor("thumbview.unread.sticker.foreground");
        THUMB_UNREAD_STICKER_FONT = RH.getFont("thumbview.unread.sticker.font");

        LIST_NORMAL_FOREGROUND = RH.getColor("listview.element.foreground");

        LIST_NORMAL_BACKGROUND = RH.getColor("listview.element.background.normal"); 
        LIST_INFOCUS_BACKGROUND = RH.getColor("listView.element.background_infocus");
        LIST_DEFAULT_BORDER = BorderFactory.createCompoundBorder(new EmptyBorder(5, 10, 5, 0), BorderFactory.createDashedBorder(Color.BLACK));

        BiFunction<String,ViewElementType, ImageIcon> thumb = (key, type) -> MyUtilsExtra.nullSafe(iconManger.getViewIcon(RH.getString(key), type), () -> iconManger.getNullIcon(type));

        THUMB_ICON_LOADING_ICON = thumb.apply("thumb_loading.icon", ViewElementType.THUMB);
        THUMB_ICON_NOT_FOUND_ICON = thumb.apply("thumb_not_found.icon", ViewElementType.THUMB);

        LIST_ICON_LOADING_ICON = thumb.apply("thumb_loading.icon", ViewElementType.LIST);
        LIST_ICON_NOT_FOUND_ICON = thumb.apply("thumb_not_found.icon", ViewElementType.LIST);

        RECENT_LIST_ICON_LOADING_ICON = thumb.apply("thumb_loading.icon", ViewElementType.RECENT_LIST);
        RECENT_LIST_ICON_NOT_FOUND_ICON = thumb.apply("thumb_not_found.icon", ViewElementType.RECENT_LIST);

        Font list_name_font = RH.getFont("listview.name.font");
        Font list_details_font = RH.getFont("listview.details.font");

        /**<pre>
         * 1 manga_name font size %d
         * 2 manga_name font name %s
         * 4 manga_details font size %d
         * 5 manga_details font name
         * </pre>
         */
        String tempListformatString = 
                ("<html><div id = 'manga_name' style = \"font-size:%dpx; font-family:'%s'\">&nbsp;%%s</div><br>"
                        + "<div id = 'manga_details' style = 'font-size:%dpx; font-family:\"%s\"; padding:5px;'>"
                        + "&nbsp;&nbsp;Author : <span color={variable_1}>%%s</span>&nbsp;&nbsp;"
                        + "&nbsp;Rank : <span color={variable_1}>%%d</span>&nbsp;&nbsp;"
                        + "&nbsp;Chapters : <span color={variable_1}>%%d</span> / <span color={variable_1}>%%d</span> &nbsp;&nbsp;"
                        + "&nbsp;Status : <span color={variable_1}>%%s</span>&nbsp;&nbsp;"
                        + "&nbsp;Favorite? : <span color={variable_1}>%%s</span>&nbsp;&nbsp;"
                        + "&nbsp;Read Count : <span color={variable_1}>%%d</span>&nbsp;&nbsp;"
                        + "&nbsp;Unread Count : <span color={variable_1}>%%d</span>&nbsp;&nbsp;"
                        + "</div></html>").replace("{variable_1}", RH.getString("listview.details.value.forground"));

        LIST_FORMATTING_STRING = String.format(tempListformatString,
                list_name_font.getSize(), 
                list_name_font.getFamily(), 
                list_details_font.getSize(), 
                list_details_font.getFamily());

        THUMB_FORMATTING_STRING = String.format("<html><div align = center style = \"font-size:%dpx; font-family:'%s'\" >%%s</div></html>", 
                THUMB_NAME_LABEL_FONT.getSize(),
                THUMB_NAME_LABEL_FONT.getFamily());

        Font RECENT_DETAILS_FONT = RH.getFont("recentview.details.font");
        Color RECENT_DETAILS_FOREGROUND = RH.getColor("recentview.details.fontforeground");
        Color RECENT_DETAILS_BACKGROUND = RH.getColor("recentview.details.fontbackground");

        String temp = "<div id = 'recents_details' bgcolor=%s color=%s style = \"font-size:%dpx; font-family:'%s' padding:20px; \" width=%dpx height=40px>&nbsp;%%s<br>&nbsp;%%s</div></html>";

        int THUMB_ICON_WIDTH = RH.getInt("thumbview.icon.width");

        temp = String.format(temp, 
                Utils.colorToCssRGBString(RECENT_DETAILS_BACKGROUND),
                Utils.colorToCssRGBString(RECENT_DETAILS_FOREGROUND),
                RECENT_DETAILS_FONT.getSize(),
                RECENT_DETAILS_FONT.getFamily(),
                THUMB_ICON_WIDTH - 50
                );

        RECENT_THUMB_FORMATTING_STRING = String.format("<html><div id = 'manga_name' style = \"font-size:%dpx; font-family:'%s'\" align = center >&nbsp;%%s</div><hr>", 
                THUMB_NAME_LABEL_FONT.getSize(),
                THUMB_NAME_LABEL_FONT.getFamily()
                ) + temp;

        temp = temp
                .replace("<br>", "&nbsp;&nbsp;|&nbsp;&nbsp;")
                .replace("width="+(THUMB_ICON_WIDTH - 50), "width=1400")
                .replace("height=40px", "height=25px");

        RECENT_LIST_FORMATTING_STRING = LIST_FORMATTING_STRING.replace("</html>", temp);

        String s = "=^..^=";
        THUMB_BLANK_STRING = String.format(THUMB_FORMATTING_STRING, s);
        RECENT_THUMB_BLANK_STRING = String.format(RECENT_THUMB_FORMATTING_STRING, s,s,s);
        LIST_BLANK_STRING = String.format(LIST_FORMATTING_STRING,s,s,0,0,0,s,s,0,0);
        RECENT_LIST_BLANK_STRING = String.format(RECENT_LIST_FORMATTING_STRING, s,s,0,0,0,s,s,0,0,s,s);
    }

    private int unreadCount;
    private MinimalManga manga;
    private int modIndex, index;
    private boolean mangaDeleted;
    private boolean isInFocus;

    ViewElement() {
        setDoubleBuffered(true);
        setFocusable(true);
        setOpaque(true);
        changeElementType();
        
        addMouseListener(mouseListener);
        addFocusListener(focusListener);
        addKeyListener(keyListener);
    }
    void changeElementType(){
        loaded = true;
        unload();
    }
    
    /**
     * index in cotnainer
     * @return
     */
    public void setIndex(int index) {
		this.index = index;
	}
    /**
     * index in cotnainer
     * @return
     */
    public int getIndex() {
		return index;
	}
    public void setModIndex(int modIndex) {
        this.modIndex = modIndex;
        
        if(modIndex < 0) {
            this.mangaDeleted = false;
            this.manga = null;
            unload();
        } else {
            
            MinimalManga m = MOD.getManga(modIndex);
            
            if(m == this.manga)
                return;
            
            unload();
            this.manga = m;
            setMangaDeleted(MOD.getDeleteQueue().contains(m));
            
        }
        //TODO reset process
    }
    boolean isInVisibleRect(){
        return getVisibleRect().height != 0;
    }
    private boolean loaded = false;

    public void unload() {
        if(!loaded)
            return;

        if(currentElementType == ViewElementType.THUMB || currentElementType == ViewElementType.RECENT_THUMB){
            setForeground(THUMB_FOREGROUND);
            setBackground(THUMB_NORMAL_BACKGROUND);
            setBorder(null);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setIcon(THUMB_ICON_LOADING_ICON);
            setText(currentElementType == ViewElementType.THUMB ? THUMB_BLANK_STRING : RECENT_THUMB_BLANK_STRING);
        }
        else if(currentElementType == ViewElementType.LIST || currentElementType == ViewElementType.RECENT_LIST){
            setForeground(LIST_NORMAL_FOREGROUND);
            setBackground(LIST_NORMAL_BACKGROUND);
            setBorder(LIST_DEFAULT_BORDER);
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setVerticalTextPosition(SwingConstants.TOP);
            setIcon(currentElementType == ViewElementType.LIST ? LIST_ICON_LOADING_ICON : RECENT_LIST_ICON_LOADING_ICON);
            setText(currentElementType == ViewElementType.LIST ? LIST_BLANK_STRING : RECENT_LIST_BLANK_STRING);
        }
        loaded = false;
    }

    /**
     * @param load text and icon of corresponding manga
     */
    void load() {
        if(loaded || manga == null)
            return;

        ImageIcon icon = iconManger.getViewIcon(manga, MangaManeger.getThumbManager().getRandomThumbPath(manga), currentElementType);

        if(icon != null)
            setIcon(icon);
        else{
            if(currentElementType == ViewElementType.THUMB || currentElementType == ViewElementType.RECENT_THUMB)
                setIcon(THUMB_ICON_NOT_FOUND_ICON);
            else if(currentElementType == ViewElementType.LIST)
                setIcon(LIST_ICON_NOT_FOUND_ICON);
            else if(currentElementType == ViewElementType.RECENT_LIST)
                setIcon(RECENT_LIST_ICON_NOT_FOUND_ICON);
        }
        reset();
        loaded = true;
    }

    void setInFocus(boolean isInFocus){
        this.isInFocus = isInFocus;

        if(mangaDeleted)
            return;
        if(currentElementType == ViewElementType.THUMB || currentElementType == ViewElementType.RECENT_THUMB)
            setBackground(isInFocus ? THUMB_INFOCUS_BACKGROUND : THUMB_NORMAL_BACKGROUND);
        else
            setBackground(isInFocus ? LIST_INFOCUS_BACKGROUND : LIST_NORMAL_BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        LOGGER.finer(() -> "REPAINT: "+this);   
        
        if(isEnabled() && unreadCount > 0 && (currentElementType == ViewElementType.THUMB || currentElementType == ViewElementType.RECENT_THUMB)){
            g.translate(getWidth() - THUMB_UNREAD_STICKER_WIDTH, 0);
            g.setColor(THUMB_UNREAD_STICKER_BACKGROUND);
            g.setFont(THUMB_UNREAD_STICKER_FONT);


            g.fill3DRect(0, 0, THUMB_UNREAD_STICKER_WIDTH, THUMB_UNREAD_STICKER_WIDTH, true);
            g.setColor(THUMB_UNREAD_STICKER_FOREGROUND);

            g.translate(THUMB_UNREAD_STICKER_WIDTH/2, THUMB_UNREAD_STICKER_WIDTH/2);
            FontMetrics fm = g.getFontMetrics();

            g.drawString(unreadCount+"",  - fm.stringWidth(unreadCount+"")/2, fm.getAscent()/2);

            g.translate(-THUMB_UNREAD_STICKER_WIDTH/2, -THUMB_UNREAD_STICKER_WIDTH/2);
            g.translate(-getWidth() + THUMB_UNREAD_STICKER_WIDTH, 0);
            g.dispose();
        }
    }
    
	public void setMangaDeleted(boolean mangaDeleted) {
        if(this.mangaDeleted == mangaDeleted)
            return;

        this.mangaDeleted = mangaDeleted;

        if(mangaDeleted)
            setBackground(MANGA_DELETED_BACKGROUND);
        else
            setInFocus(isInFocus);
    } 

    @SuppressWarnings("deprecation")
	void reset() {
        unreadCount = manga.getUnreadCount();
        setMangaDeleted(mangaDeleted);

        if(currentElementType == ViewElementType.THUMB || currentElementType == ViewElementType.RECENT_THUMB){
            if(currentElementType == ViewElementType.THUMB)
                setText(String.format(THUMB_FORMATTING_STRING, 
                        ((manga.getMangaName().length() > 18)?manga.getMangaName().substring(0, 18)+"&hellip;":manga.getMangaName())));
            else{
                MinimalChapterSavePoint savePoint = MangaManeger.getChapterSavePoint(manga);
                String name = savePoint == null || savePoint.getChapterFileName() == null ? "savePoint = null" :  savePoint.getChapterFileName().replaceFirst("\\.jpe?g$", "").trim();

                setText(String.format(RECENT_THUMB_FORMATTING_STRING,
                        (manga.getMangaName().length() > 18)?manga.getMangaName().substring(0, 18)+"&hellip;":manga.getMangaName(),
                                (savePoint == null ? "savePoint = null" : Utils.getFormattedDateTime(savePoint.getSaveTime())),
                                (name.length() > 18)?name.substring(0, 18)+"&hellip;":name
                        ));
            }
        }
        else{
            MinimalListManga manga2 = (MinimalListManga) manga;
            if(currentElementType == ViewElementType.LIST){
                setText(String.format(LIST_FORMATTING_STRING,
                        manga2.getMangaName(),
                        manga2.getAuthorName(),
                        manga2.getRank(),
                        manga2.getChapCountMangarock(),
                        manga2.getChapCountPc(),
                        manga2.getStatusString(),
                        manga2.isFavoriteString(),
                        manga2.getReadCount(),
                        manga2.getUnreadCount()
                        ));
            }
            else{
                MinimalChapterSavePoint savePoint = MangaManeger.getChapterSavePoint(manga);
                setText(String.format(RECENT_LIST_FORMATTING_STRING,
                        manga2.getMangaName(),
                        manga2.getAuthorName(),
                        manga2.getRank(),
                        manga2.getChapCountMangarock(),
                        manga2.getChapCountPc(),
                        manga2.getStatusString(),
                        manga2.isFavoriteString(),
                        manga2.getReadCount(),
                        manga2.getUnreadCount(),
                        (savePoint == null ? "savePoint = null" : Utils.getFormattedDateTime(savePoint.getSaveTime())),
                        (savePoint == null ? "savePoint = null" : savePoint.getChapterFileName().replaceFirst("\\.jpe?g$", "").trim())
                        ));
            }
        }

        setMangaDeleted(mangaDeleted);
        revalidate();
        repaint();
    }
    
    @Override
	public String toString() {
		return "ViewElement [index:"+index+ (modIndex < 0 ? "" : "manga_id:"+MangaManeger.mangaIdOf(manga)) + "]";
	}

    public MinimalManga getManga() {
        return manga;
    }
    public int y(){
        return getLocationOnScreen().y;
    }
    public int getModIndex() {
        return modIndex;
    }
}
