package samrock.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import samrock.manga.MangaManeger;
import samrock.manga.MinimalChapterSavePoint;
import samrock.manga.MinimalListManga;
import samrock.manga.MinimalManga;
import samrock.utils.IconManger;
import samrock.utils.RH;
import samrock.utils.Utils;
import samrock.utils.ViewElementType;

final class ViewElement extends JLabel {
	private static final long serialVersionUID = -1304445825214964125L;
	private static ViewElementType currentElementType ;
	private static Color MANGA_DELETED_BACKGROUND;

	/* *****************************************************************************
	 * *************************THUMB ELEMENT***************************************
	 * *****************************************************************************
	 */

	private static Color THUMB_NORMAL_BACKGROUND;
	private static Color THUMB_INFOCUS_BACKGROUND;

	private static Color THUMB_FOREGROUND;

	private static Color THUMB_UNREAD_STICKER_BACKGROUND;
	private static Color THUMB_UNREAD_STICKER_FOREGROUND;

	private static Font THUMB_UNREAD_STICKER_FONT;
	private static int THUMB_UNREAD_STICKER_WIDTH;

	private static ImageIcon THUMB_ICON_NOT_FOUND_ICON;
	private static ImageIcon THUMB_ICON_LOADING_ICON;

	/**
	 * 1 manga_name %s
	 */
	private static String THUMB_FORMATTING_STRING;
	private static String THUMB_BLANK_STRING;

	/* *****************************************************************************
	 * *************************LIST ELEMENT****************************************
	 * *****************************************************************************
	 */

	private static Color LIST_NORMAL_FOREGROUND;

	private static Color LIST_NORMAL_BACKGROUND; 
	private static Color LIST_INFOCUS_BACKGROUND;

	private static Border LIST_DEFAULT_BORDER;

	private static ImageIcon LIST_ICON_NOT_FOUND_ICON;
	private static ImageIcon LIST_ICON_LOADING_ICON;

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
	private static String LIST_FORMATTING_STRING;
	private static String LIST_BLANK_STRING;

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
	private static String RECENT_LIST_FORMATTING_STRING;
	private static String RECENT_LIST_BLANK_STRING;

	/**<pre>
	 * 1 manga_name %s
	 * 2 formatted recent time %s
	 * 3 chapter_name %s
	 * </pre>
	 */
	private static String RECENT_THUMB_FORMATTING_STRING;
	private static String RECENT_THUMB_BLANK_STRING;

	private static ImageIcon RECENT_LIST_ICON_NOT_FOUND_ICON;
	private static ImageIcon RECENT_LIST_ICON_LOADING_ICON;

	private static IconManger iconManger;
	private static MangaManeger mangaManeger;

	private static MouseAdapter mouseAdapter; 
	private static KeyAdapter keyAdapter;
	private static FocusListener focusListener;

	static void initConstants(MouseAdapter mouseAdapter, 
			KeyAdapter keyAdapter, 
			FocusListener focusListener){
		if(RECENT_THUMB_FORMATTING_STRING != null)
			return;

		ViewElement.mouseAdapter = mouseAdapter;  
		ViewElement.keyAdapter = keyAdapter; 
		ViewElement.focusListener = focusListener;

		iconManger = IconManger.getInstance();
		mangaManeger = MangaManeger.getInstance();

		currentElementType = RH.getStartupViewElementType();

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



		THUMB_ICON_LOADING_ICON = iconManger.getViewIcon(RH.getString("thumb_loading.icon"), ViewElementType.THUMB);
		THUMB_ICON_NOT_FOUND_ICON = iconManger.getViewIcon(RH.getString("thumb_not_found.icon"), ViewElementType.THUMB);

		THUMB_ICON_LOADING_ICON = THUMB_ICON_LOADING_ICON == null ? iconManger.getNullIcon(ViewElementType.THUMB) : THUMB_ICON_LOADING_ICON;
		THUMB_ICON_NOT_FOUND_ICON = THUMB_ICON_NOT_FOUND_ICON == null ? iconManger.getNullIcon(ViewElementType.THUMB) : THUMB_ICON_NOT_FOUND_ICON;

		LIST_ICON_LOADING_ICON = iconManger.getViewIcon(RH.getString("thumb_loading.icon"), ViewElementType.LIST);
		LIST_ICON_NOT_FOUND_ICON = iconManger.getViewIcon(RH.getString("thumb_not_found.icon"), ViewElementType.LIST);

		LIST_ICON_LOADING_ICON = LIST_ICON_LOADING_ICON == null ? iconManger.getNullIcon(ViewElementType.LIST) : LIST_ICON_LOADING_ICON;
		LIST_ICON_NOT_FOUND_ICON = LIST_ICON_NOT_FOUND_ICON == null ? iconManger.getNullIcon(ViewElementType.LIST) : LIST_ICON_NOT_FOUND_ICON;

		RECENT_LIST_ICON_LOADING_ICON = iconManger.getViewIcon(RH.getString("thumb_loading.icon"), ViewElementType.RECENT_LIST);
		RECENT_LIST_ICON_NOT_FOUND_ICON = iconManger.getViewIcon(RH.getString("thumb_not_found.icon"), ViewElementType.RECENT_LIST);

		RECENT_LIST_ICON_LOADING_ICON = RECENT_LIST_ICON_LOADING_ICON == null ? iconManger.getNullIcon(ViewElementType.RECENT_LIST) : RECENT_LIST_ICON_LOADING_ICON;
		RECENT_LIST_ICON_NOT_FOUND_ICON = RECENT_LIST_ICON_NOT_FOUND_ICON == null ? iconManger.getNullIcon(ViewElementType.RECENT_LIST) : RECENT_LIST_ICON_NOT_FOUND_ICON;

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

	static ViewElementType getCurrentElementType() {
		return currentElementType;
	}

	static void setCurrentElementType(ViewElementType elementType) {
		currentElementType = elementType;
	}

	private int unreadCount;
	public final int ARRAY_INDEX;
	private boolean mangaDeleted;

	ViewElement(int arrayIndex) {
		this.ARRAY_INDEX = arrayIndex;
		setDoubleBuffered(false);
		setFocusable(true);
		setOpaque(true);
		changeElementType();
	}

	void changeElementType(){
		loaded = true;
		unload();
	} 

	boolean isInVisibleRect(){
		return getVisibleRect().height != 0;
	}
	
	private boolean listenersAdded = false;
	void addListeners(){
		if(listenersAdded)
			return;

		listenersAdded = true;

		setEnabled(true);
		addMouseListener(mouseAdapter);
		addFocusListener(focusListener);
		addKeyListener(keyAdapter);
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
		if(loaded)
			return;

		ImageIcon icon = iconManger.getViewIcon(mangaManeger.getRandomThumbPath(ARRAY_INDEX), currentElementType);

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
		resetText();
		
		loaded = true;
	}

	void setInFocus(boolean isInFocus){
		if(currentElementType == ViewElementType.THUMB || currentElementType == ViewElementType.RECENT_THUMB)
			setBackground(mangaDeleted ? MANGA_DELETED_BACKGROUND : isInFocus ? THUMB_INFOCUS_BACKGROUND : THUMB_NORMAL_BACKGROUND);
		else
			setBackground(mangaDeleted ? MANGA_DELETED_BACKGROUND :  isInFocus ? LIST_INFOCUS_BACKGROUND : LIST_NORMAL_BACKGROUND);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

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


	public void resetDeleted() {
		if(mangaDeleted = mangaManeger.isMangaInDeleteQueue(mangaManeger.getManga(ARRAY_INDEX)))
			setBackground(MANGA_DELETED_BACKGROUND);
	}
	
	void resetText() {
		MinimalManga manga = mangaManeger.getManga(ARRAY_INDEX);
		unreadCount = manga.getUnreadCount();
		resetDeleted();
		
		if(currentElementType == ViewElementType.THUMB || currentElementType == ViewElementType.RECENT_THUMB){
			if(currentElementType == ViewElementType.THUMB)
				setText(String.format(THUMB_FORMATTING_STRING, 
						((manga.getName().length() > 18)?manga.getName().substring(0, 18)+"&hellip;":manga.getName())));
			else{
				MinimalChapterSavePoint savePoint = mangaManeger.getChapterSavePoint(manga.ARRAY_INDEX);
				String name = savePoint == null || savePoint.getChapterFileName() == null ? "savePoint = null" :  savePoint.getChapterFileName().replaceFirst("\\.jpe?g$", "").trim();

				setText(String.format(RECENT_THUMB_FORMATTING_STRING,
						(manga.getName().length() > 18)?manga.getName().substring(0, 18)+"&hellip;":manga.getName(),
								(savePoint == null ? "savePoint = null" : Utils.getFormattedDateTime(savePoint.getSaveTime())),
								(name.length() > 18)?name.substring(0, 18)+"&hellip;":name
						));
			}
		}
		else{
			MinimalListManga manga2 = (MinimalListManga) manga;
			if(currentElementType == ViewElementType.LIST){
				setText(String.format(LIST_FORMATTING_STRING,
						manga2.getName(),
						manga2.AUTHOR_NAME,
						manga2.RANK,
						manga2.CHAP_COUNT_MANGAROCK,
						manga2.getChapCountPc(),
						(manga2.STATUS ? "Completed" : "On Going"),
						(manga2.isFavorite() ? "Yes" : "No"),
						manga2.getReadCount(),
						manga2.getUnreadCount()
						));
			}
			else{
				MinimalChapterSavePoint savePoint = mangaManeger.getChapterSavePoint(manga.ARRAY_INDEX);
				setText(String.format(RECENT_LIST_FORMATTING_STRING,
						manga2.getName(),
						manga2.AUTHOR_NAME,
						manga2.RANK,
						manga2.CHAP_COUNT_MANGAROCK,
						manga2.getChapCountPc(),
						(manga2.STATUS ? "Completed" : "On Going"),
						(manga2.isFavorite() ? "Yes" : "No"),
						manga2.getReadCount(),
						manga2.getUnreadCount(),
						(savePoint == null ? "savePoint = null" : Utils.getFormattedDateTime(savePoint.getSaveTime())),
						(savePoint == null ? "savePoint = null" : savePoint.getChapterFileName().replaceFirst("\\.jpe?g$", "").trim())
						));
			}
		}
		
		resetDeleted();
		revalidate();
		repaint();
		
	}
}
