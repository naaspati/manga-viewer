package samrock.gui.front;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.logging.Level;
import org.slf4j.Logger;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import sam.myutils.Checker;
import sam.nopkg.Junk;
import samrock.PrintFinalize;
import samrock.RH;
import samrock.Utils;
import samrock.manga.Manga;
import samrock.manga.maneger.IconManger;
import samrock.manga.maneger.MangaManeger;
import samrock.manga.maneger.Mangas;

public final class DataView extends JPanel implements PrintFinalize {
	private static final long serialVersionUID = 4095744450884123779L;
	private static final Logger logger = Utils.getLogger(DataView.class);

	private Manga manga;
	private static final Color NAME_LABEL_FOREGROUND;
	private static final Color NAME_LABEL_BACKGROUND;
	private static final Color DELETED_MANGA_NAME_LABEL_BACKGROUND;

	static {
		NAME_LABEL_FOREGROUND = RH.getColor("datapanel.manga.name.foreground");
		NAME_LABEL_BACKGROUND = RH.getColor("datapanel.manga.name.background");
		DELETED_MANGA_NAME_LABEL_BACKGROUND = RH.getColor("deleted.manga.background");
	}

	private final JMenuItem delete;
	private final JMenuItem undelete;
	private final JMenuItem favorite;
	private final JMenuItem unfavorite;
	private final JMenuItem openUrls;
	private final JMenuItem openBuid;
	//FIXME to-be removed private final JMenuItem openthumbFolder;

	private final JLabel nameLabel;
	private final JEditorPane detailsPane;
	private final String dataHtmlTemplate;
	private final MangaThumbSetLabel mangaImageSet; //the strip of the shown in dataPanel that is this
	private final ImageIcon FAVORITED_NAME_LABEL_ICON;

	private final JScrollPane mangaImageSetPane;
	private final String searchedTextHighlight;

	private final Mangas mangasOnDisplay;
	private final boolean resourcesLoaded;
	private List<String> urls;

	public DataView() {
		super(new BorderLayout(), false);

		setBackground(RH.getColor("datapanel.dock_color"));
		this.mangasOnDisplay = MangaManeger.mangas();

		JPanel p2 = new JPanel(new BorderLayout(), false);
		p2.setOpaque(true);
		p2.setBackground(NAME_LABEL_BACKGROUND);
		p2.setBorder(BorderFactory.createMatteBorder(1, 0, 2, 0, RH.getColor("datapanel.manga.name.panel.separator.color")));

		nameLabel = new JLabel("", JLabel.LEFT);
		nameLabel.setDoubleBuffered(false);

		FAVORITED_NAME_LABEL_ICON = RH.getImageIcon("datapanel.favorited.icon");

		nameLabel.setFont(RH.getFont("datapanel.manga.name.font"));
		nameLabel.setForeground(NAME_LABEL_FOREGROUND);
		nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 0));
		nameLabel.setIconTextGap(30);

		nameLabel.setOpaque(false);

		p2.add(nameLabel, BorderLayout.CENTER);

		JPopupMenu popupMenu = new JPopupMenu();

		BiFunction<String, ActionListener, JMenuItem> createMenuItem = (key, actionListener) -> {
			JMenuItem b = new JMenuItem(RH.getString(key+".text"), RH.getImageIcon(key+".icon"));
			b.setIconTextGap(10);

			if(actionListener != null)
				b.addActionListener(actionListener);
			b.setDoubleBuffered(false);
			popupMenu.add(b);
			return b;
		};

		createMenuItem.apply("datapanel.menubutton.copymanganame", e -> {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(manga.getMangaName()), null);
			Utils.showHidePopup("Manga Name Copied", 1000);
		});

		delete = createMenuItem.apply("datapanel.menubutton.delete", null);
		undelete = createMenuItem.apply("datapanel.menubutton.undelete", null);
		favorite = createMenuItem.apply("datapanel.menubutton.favorites", null);
		unfavorite = createMenuItem.apply("datapanel.menubutton.unfavorites", null);
		createMenuItem.apply("datapanel.menubutton.openmanga.folder", e -> Utils.openFile(manga.getDir().toFile()));
		/** FIXME to-be removed
		 * openthumbFolder = createMenuItem.apply("datapanel.menubutton.thumbfolder", e -> {
			try {
				Runtime.getRuntime().exec("explorer /Select,\""+(new File(MyConfig.SAMROCK_THUMBS_DIR, MangaManeger.getRandomThumbPath(manga.getMangaIndex())))+"\"");
			} catch (IOException e1) {
				logger.log(Level.SEVERE, "Failed to open thumbg folder", e1);
			}
		});
		 */
		// FIXME change "datapanel.menubutton.mangafox" -> "datapanel.menubutton.urls" 
		openUrls = createMenuItem.apply("datapanel.menubutton.mangafox", this::urlsAction);
		openBuid = createMenuItem.apply("datapanel.menubutton.buid", e ->  Utils.browse("https://www.mangaupdates.com/series.html?id=".concat(String.valueOf(manga.getBuId()))));
		openBuid.setToolTipText("open https://www.mangaupdates.com/series.html?id={BU_ID}");

		//FIXME to-be removed createMenuItem.apply("datapanel.menubutton.movethumbs", e ->  importThumbs());
		//FIXME to-be removed createMenuItem.apply("datapanel.menubutton.reloadicons", e ->  reloadIcons());

		delete.addActionListener(e -> {
			mangasOnDisplay.getDeleteQueue().add(manga);
			toggleDeleteMenuItems();
		});
		undelete.addActionListener(e -> {
			mangasOnDisplay.getDeleteQueue().remove(manga);
			toggleDeleteMenuItems();
		});

		favorite .addActionListener(e -> {
			manga.setFavorite(true);
			toggleFavoritesMenuItems();
		});
		unfavorite .addActionListener(e -> {
			manga.setFavorite(false);
			toggleFavoritesMenuItems();
		});

		JButton menuButton = Utils.createMenuButton(null);
		menuButton.addActionListener(e -> popupMenu.show(menuButton, 0 - popupMenu.getWidth() + menuButton.getWidth(), 0));
		p2.add(menuButton, BorderLayout.EAST);

		mangaImageSet = new MangaThumbSetLabel();

		mangaImageSet.setBorder(new EmptyBorder(10, 10, 10, 20));
		mangaImageSetPane = new JScrollPane(mangaImageSet, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mangaImageSetPane.setDoubleBuffered(false);

		mangaImageSetPane.getVerticalScrollBar().setOpaque(false);
		mangaImageSetPane.getViewport().setOpaque(false);
		mangaImageSetPane.setOpaque(false);

		detailsPane = new JEditorPane("text/html", "");
		detailsPane.setDoubleBuffered(false);

		HTMLEditorKit kit = new HTMLEditorKit();
		detailsPane.setEditorKit(kit);
		detailsPane.setOpaque(true);
		detailsPane.setBackground(getBackground());

		detailsPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));	

		Font th_Font = RH.getFont("datapanel.th.font");
		Font td_Font = RH.getFont("datapanel.td.font");

		StyleSheet style = kit.getStyleSheet();

		String cssTemplate;
		String s2 = null;

		try(InputStream is = RH.getStream("datapanel.html.template.path");
				InputStream is2 = RH.getStream("datapanel.css.template.path");
				ByteArrayOutputStream bos = new ByteArrayOutputStream(is.available())) {
			int b;
			while((b = is.read()) != -1) bos.write(b);

			s2 = bos.toString();

			bos.reset();
			while((b = is2.read()) != -1) bos.write(b);

			cssTemplate = new String(bos.toByteArray());
		} catch (IOException e) {
			logger.log(Level.WARNING, "Error while loading\r\n"+RH.getString("datapanel.html.template.path")+System.lineSeparator()+RH.getString("datapanel.css.template.path"), e);
			add(Utils.getNothingfoundlabel("Error while loading resources"));
			searchedTextHighlight = null;
			resourcesLoaded = false;
			dataHtmlTemplate = null;
			return;
		}

		dataHtmlTemplate = s2;

		IntFunction<String> getHtmlStyle = fontStyle -> {
			if(fontStyle == Font.PLAIN)
				return "normal";
			else if(fontStyle == Font.BOLD)
				return "bold";
			else if(fontStyle == Font.ITALIC)
				return "italic";
			return  "oblique";
		};

		Function<String, String> getHtmlColor = key -> {
			String str = RH.getString(key);

			if(str.matches("#\\w+"))
				return str;

			try {
				return Integer.toHexString(Integer.parseInt(str)).replaceFirst("ff", "#");
			} catch (NumberFormatException e) { }

			return Integer.toHexString(RH.getColor(key).getRGB()).replaceFirst("ff", "#");
		};

		String thFontValue = getHtmlStyle.apply(th_Font.getStyle())+" "+th_Font.getSize()+"pt '"+th_Font.getName()+"', serif";
		String tdFontValue = getHtmlStyle.apply(td_Font.getStyle())+" "+td_Font.getSize()+"pt '"+td_Font.getName()+"', sans-serif";

		style.addRule(String.format(cssTemplate, 
				getHtmlColor.apply("datapanel.details.th.background"),
				getHtmlColor.apply("datapanel.details.th.foreground"), 
				thFontValue, tdFontValue, 
				getHtmlColor.apply("datapanel.details.td.foreground")));

		Document doc = kit.createDefaultDocument();
		detailsPane.setDocument(doc);

		detailsPane.setEditable(false);
		JScrollPane detailspaneScrollPane = new JScrollPane(detailsPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		detailspaneScrollPane.setDoubleBuffered(false);

		add(detailspaneScrollPane, BorderLayout.CENTER);
		add(p2, BorderLayout.NORTH);
		add(mangaImageSetPane, BorderLayout.EAST);

		searchedTextHighlight = "<span bgcolor="+getHtmlColor.apply("datapanel.search.text.highlight")+">$1</span>";
		resourcesLoaded = true;

		changeManga();
	}

	private void urlsAction(Object ignore) {
		if(urls == null) {
			try {
				urls = MangaManeger.getUrls(manga);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "failed to load urls", e);
				return;
			}
		}

		// display a JList in a JDialog with an open button.
		// FIXME urls viewer
		Junk .notYetImplemented();
	}

	/** FIXME to-be removed
	 * 
	private void importThumbs() {
		String folderString = JOptionPane.showInputDialog(null, "Path to Thumbs", "D:\\Core Files\\PrintScreen Files");

		if(folderString == null || (folderString = folderString.replace("\"", "").trim()).isEmpty())
			return;

		File input = new File(folderString);

		if(!input.exists()){
			Utils.showHidePopup("not found", 1500);
			return;
		}

		if(input.isFile()){
			try {
				Runtime.getRuntime().exec("explorer /Select,\""+folderString+"\"");
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Failed to open file location: "+folderString, e);
				return;
			}

			if(JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null, "<html>sure to move image file?<br>"+input.getName()+"</html>"))
				return;
		}

		if(!Utils.openFile(input))
			return;

		File[] files = input.listFiles(File::isFile);

		if(files.length == 0){
			Utils.showHidePopup("empty folder", 1500);
			return;
		}

		JCheckBox[] ch = new JCheckBox[files.length];

		JPanel p = new JPanel(new GridLayout(0, 3), false);
		for (int i = 0; i < files.length; i++) {
			final String s = files[i].getName();
			p.add(ch[i] = new JCheckBox(s));
			ch[i].setSelected(s.endsWith("jpg") || s.endsWith("jpeg"));
		}

		if(JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(null, p, "select to move!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null)){
			Utils.showHidePopup("Cancelled", 1500);
			return;
		}

		ArrayList<File> temp = new ArrayList<>();

		for (int i = 0; i < ch.length; i++) {
			if(ch[i].isSelected())
				temp.add(files[i]);
		}

		if(temp.isEmpty()){
			Utils.showHidePopup("nothing selected", 1500);
			return;
		}

		files = temp.toArray(new File[temp.size()]); 


		String[] thumbs = MangaManeger.getThumbsPaths(manga.getMangaIndex());

		File[] files2 = Arrays.copyOf(files, files.length + thumbs.length);

		for (int i = files.length; i < files2.length; i++){
			File src = new File(thumbFolder, thumbs[i - files.length]);
			File target = new File(thumbFolder, "temp_"+i);
			src.renameTo(target);
			files2[i] = target;
		}

		files = null;

		Arrays.sort(files2, Comparator.comparing(File::length).reversed());

		File folder = new File(thumbFolder, String.valueOf(manga.getMangaId()));

		folder.mkdirs();

		String str1 = String.valueOf(manga.getMangaId()).concat("_");
		int k = 0;

		for (File f : files2)
			f.renameTo(new File(folder, str1.concat(String.valueOf(k++).concat(".jpg"))));

		thumbFolder.setLastModified(time);
		Utils.openFile(folder);

		if(JOptionPane.showConfirmDialog(null,"Refresh?")  != JOptionPane.YES_OPTION)
			return;

		reloadIcons();
	}
	 */

	/** FIXME to-be removed
	 * 	private void reloadIcons(){
		MangaManeger.reListIcons();
		IconManger.getInstance().removeIconCache(manga.getMangaId());

		EventQueue.invokeLater(() -> {
			mangaImageSet.reset();
			detailsPane.setCaretPosition(0);
			mangaImageSetPane.getVerticalScrollBar().setValue(0);
			revalidate();
			repaint();
		});
	}
	 */

	private void toggleFavoritesMenuItems() {
		boolean b = manga.isFavorite();
		unfavorite.setVisible(b);
		favorite.setVisible(!b);
		nameLabel.setIcon(b ? FAVORITED_NAME_LABEL_ICON : null);
	}

	private void toggleDeleteMenuItems() {
		boolean b = mangasOnDisplay.getDeleteQueue().contains(manga);
		undelete.setVisible(b);
		delete.setVisible(!b);
		nameLabel.getParent().setBackground(b ?  DELETED_MANGA_NAME_LABEL_BACKGROUND : NAME_LABEL_BACKGROUND);
	}

	public void changeManga(){
		if(!resourcesLoaded)
			return;

		manga = MangaManeger.getCurrentManga();

		if(manga == null){
			removeAll();
			add(Utils.getNothingfoundlabel("DataView Error(manga = null)"));
			return;
		}
		nameLabel.setText("<html>"+manga.getMangaName()+"</html>");
		toggleDeleteMenuItems();
		toggleFavoritesMenuItems();
		openBuid.setVisible(manga.getBuId() > 0);
		//FIXME to-be removed openthumbFolder.setVisible(MangaManeger.getRandomThumbPath(manga.getMangaIndex()) != null);
		detailsPane.setText(getHtml());

		//without this, the detailsPane scrolls to the end of the page
		EventQueue.invokeLater(() -> {
			mangaImageSet.reset();
			detailsPane.setCaretPosition(0);
			mangaImageSetPane.getVerticalScrollBar().setValue(0);
			revalidate();
			repaint();
		});


	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private String getHtml(){
		/**
		 * 1 Author
		 * 2 Unread Count
		 * 3 Status
		 * 4 Read Count
		 * 5 Rank
		 * 6 Last Updated
		 * 7 Is Favorite?
		 * 8 Last Read
		 * 9 Chapters In mangaRock
		 * 10 id / bu_id
		 * 11 Chapters / Strips In Pc
		 * 12 dir_name
		 * 13 Tags
		 * 14 Description
		 */
		String description = Optional.ofNullable(manga.getDescription())
				.filter(s -> !Checker.isEmptyTrimmed(s))
				.map(s -> s.replaceAll("\r?\n", "<br>").replace("\t", "&emsp;"))
				.map(des -> {
					String replace = Optional.ofNullable(MangaManeger.searchManager(false))
							.map(s -> s.activeTextSearch())
							.filter(s -> !Checker.isEmptyTrimmed(s))
							.orElse(null);
					
					if(replace != null) {
						Pattern pattern = Pattern.compile(replace, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
						des = pattern.matcher(des).replaceAll(searchedTextHighlight);
					}
					return des;
				})
				.orElse("No Description");
		
		
		String tags = parseTags(manga, Optional.ofNullable(MangaManeger.searchManager(false)).map(s -> s.activeTagSearch()).orElse(Collections.emptySet()));

		return String.format(dataHtmlTemplate, 
				manga.getAuthorName(),
				manga.getUnreadCount(),
				(manga.getStatusString()),
				manga.getReadCount(),
				String.valueOf(manga.getRank()),
				Utils.getFormattedDateTime(manga.getLastUpdateTime()),
				(manga.isFavorite() ? "Yes" : "No"),
				Utils.getFormattedDateTime(manga.getLastReadTime()),
				manga.getChapCountMangarock(),
				MangaManeger.mangaIdOf(manga)+" / "+(manga.getBuId() < 0 ? "<span color='#FDF5E6'>N/A</span>" : manga.getBuId()),
				manga.getChapCountPc()+" / "+(manga.getReadCount()+manga.getUnreadCount()),
				manga.getDirName(),
				tags,
				description
				);
	}
	
	private final StringBuilder sb = new StringBuilder();

	private String parseTags(Manga manga, Set<String> colortags) {
		String[] array = manga.getTags();
		if(Checker.isEmpty(array))
			return "";

		sb.setLength(0);

		if(Checker.isEmpty(colortags)) {
			for (String tag : array) 
				sb.append(tag).append(", ");
			
		} else {
			for (String tag : array) {
				if(colortags.contains(tag))
					sb.append("<span bgcolor=red>").append(tag).append("</span>").append(", ");
				else
					sb.append(tag).append(", ");
			}
		}
		
		if(sb.length() > 2)
			sb.setLength(sb.length() - 2);

		return sb.toString();
	}

	private final class MangaThumbSetLabel extends JLabel {
		private static final long serialVersionUID = -4491822309848422721L;

		private final IconManger iconManger;
		private ImageIcon nothingFound;

		MangaThumbSetLabel() {
			setDoubleBuffered(false);

			iconManger = IconManger.getInstance();
			setVerticalAlignment(JLabel.TOP);

			nothingFound = RH.getImageIcon("mangathumbsetlabel.nothingfound.icon");
		}

		void reset(){
			ImageIcon icon = iconManger.getDataPanelImageSetIcon(MangaManeger.getThumbManager().getThumbsPaths(manga), manga);
			icon = icon == null ? nothingFound : icon;
			setIcon(icon);
			mangaImageSetPane.setPreferredSize(new Dimension(icon.getIconWidth() + 20, icon.getIconHeight()));
		}
	}
	@Override
	protected void finalize() throws Throwable {
		printFinalize();
	}

}



