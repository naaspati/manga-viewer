package samrock.gui.front;

import static samrock.gui.Change.BACK_TO_DOCK;
import static samrock.gui.Change.CHANGETYPE_LIST;
import static samrock.gui.Change.CHANGETYPE_NORMAL;
import static samrock.gui.Change.CHANGETYPE_RECENT;
import static samrock.gui.Change.CHANGETYPE_THUMB;
import static samrock.gui.Change.CHANGEVIEW_CHAPTERS_LIST_VIEW;
import static samrock.gui.Change.CHANGEVIEW_DATA_VIEW;
import static samrock.gui.Change.CLOSE_APP;
import static samrock.gui.Change.ICONFY_APP;
import static samrock.gui.Change.OPEN_MOST_RECENT_CHAPTER;
import static samrock.gui.Change.OPEN_MOST_RECENT_MANGA;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import org.slf4j.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import sam.myutils.MyUtilsBytes;
import samrock.RH;
import samrock.Utils;
import samrock.ViewElementType;
import samrock.Views;
import samrock.gui.Change;
import samrock.gui.Changer;
import samrock.manga.maneger.DeleteQueue;
import samrock.manga.maneger.MangaManeger;
import samrock.manga.maneger.MangaManegerStatus;
import samrock.manga.maneger.Mangas;
import samrock.manga.maneger.SearchManeger;
import samrock.manga.maneger.SortingMethod;

public class WestControl extends JPanel {
	private static final long serialVersionUID = 6165358998450624096L;
	private static final Logger LOGGER = Utils.getLogger(WestControl.class);

	private static Runtime RUNTIME = Runtime.getRuntime();

	//listing controls
	private final JButton sortingButton;
	private final JButton listFavoritesButton;
	private final JButton listRecentsButton;
	private final JButton listdeleteQueuedButton;
	private final JButton openTagsDialogButton;

	private final  JButton backToDockButton;
	private final JButton changeToDataViewButton;
	private final JButton mostRecentsChapterButton;
	private final JButton changeToChaptersListViewButton;
	private final JButton settingsButton;
	private final JButton changeToListElementTypeButton;
	private final JButton changeToThumbElementTypeButton;

	private final JButton hidePanelButton;
	private final JButton showPanelButton;
	private final JButton iconifyAppButton;
	private final JButton closeAppButton;
	private final JButton menuButton;
	private final JButton searchButton;
	private final JPanel searchButtonContainer;

	private final Changer changer;
	private final Mangas mangasOnDisplay;
	private SearchManeger searchManeger;

	/*
	 *This is to cover entire CENTER of super panel  
	 */
	private final JPanel centrePanel;
	/*
	 * 1 -> BackToDockButton
	 * 2 -> MangaData and ListChapters Buttons
	 */
	private final JPanel minimizedControlPanel;
	/*
	 * 1 -> sorting buttons
	 * 2 -> search options
	 * 3 -> mostRecentButton, changeToThumbButton, changeToListView, settingButton
	 */
	private final JPanel maximizedControlPanel;

	/* NORTH -> iconify app and expand, shrink control panel buttons
	 * CENTRE -> centrePanel
	 * SOUTH -> status JLabel
	 * 
	 */

	private final Color default_foreground = RH.getColor("westcontrol.color.foreground");

	public WestControl(Changer controlPanelChanger) {
		super(new BorderLayout(), false);
		final Font default_font = RH.getFont("westcontrol.font");
		final Color default_background = RH.getColor("westcontrol.color.background");

		changer = controlPanelChanger;

		setBackground(default_background);
		setForeground(default_foreground);
		setFont(default_font);

		Consumer<JComponent> setAlignmentsTopLeft = j -> {
			j.setAlignmentY(Component.TOP_ALIGNMENT);
			j.setAlignmentX(Component.LEFT_ALIGNMENT);
		};

		Consumer<JComponent> setAlignmentsBottomLeft = j -> {
			j.setAlignmentX(Component.LEFT_ALIGNMENT);
			j.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		};

		hidePanelButton = Utils.createButton("westcontrol.button.hidecontrolpanel.icon", "westcontrol.button.hidecontrolpanel.tooltip", null,null, e -> clickShowHideButton(false));  
		showPanelButton = Utils.createButton("westcontrol.button.showcontrolpanel.icon", "westcontrol.button.showcontrolpanel.tooltip", null,null, e -> clickShowHideButton(true));

		iconifyAppButton = Utils.createButton("samrock.iconify.button.icon", "samrock.iconify.button.tooltip", null, null,  e -> changer.changeTo(ICONFY_APP));
		closeAppButton = Utils.createButton("samrock.close.button.icon", "samrock.close.button.tooltip", null, null,  e -> changer.changeTo(CLOSE_APP));

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem mi = new JMenuItem("Iconfy", iconifyAppButton.getIcon());
		popupMenu.add(mi);
		mi.addActionListener(e -> changer.changeTo(ICONFY_APP));

		mi = new JMenuItem("Close App", closeAppButton.getIcon());
		popupMenu.add(mi);
		mi.addActionListener(e -> changer.changeTo(CLOSE_APP));

		menuButton = Utils.createMenuButton(e -> popupMenu.show((JComponent)e.getSource(), 0, 0));
		menuButton.setVisible(false);

		JPanel p = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));
		p.add(iconifyAppButton);
		p.add(closeAppButton);
		p.add(menuButton);
		p.add(Box.createGlue());
		p.add(hidePanelButton);
		p.add(showPanelButton);
		p.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(5, 0, 20, 0), BorderFactory.createMatteBorder(0, 0, 1, 0, RH.getColor("westcontrol.border.northcontrol.separator_color"))));
		add(p, BorderLayout.NORTH);

		setAlignmentsTopLeft.accept(iconifyAppButton);
		setAlignmentsTopLeft.accept(hidePanelButton);
		setAlignmentsTopLeft.accept(closeAppButton);
		setAlignmentsTopLeft.accept(menuButton);

		showPanelButton.setAlignmentY(Component.TOP_ALIGNMENT);
		showPanelButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

		showPanelButton.setVisible(false);

		centrePanel = Utils.createJPanel(new GridLayout(1, 1));
		add(centrePanel);

		//--------------------------------------------------------------------------------------------------------
		minimizedControlPanel = Utils.createJPanel(new GridLayout(5, 1));
		backToDockButton = Utils.createButton("westcontrol.button.backtodock.icon", "westcontrol.button.backtodock.tooltip",null,null, e -> changer.changeTo(BACK_TO_DOCK));

		p = Utils.createJPanel(new BoxLayout(null, BoxLayout.Y_AXIS));
		p.add(backToDockButton);

		backToDockButton.setVisible(false);

		backToDockButton.setAlignmentY(Component.TOP_ALIGNMENT);
		backToDockButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		minimizedControlPanel.add(p);
		minimizedControlPanel.add(Box.createGlue());
		minimizedControlPanel.add(Box.createGlue());
		p = null;

		changeToDataViewButton = Utils.createButton("westcontrol.button.changetodataview.icon", "westcontrol.button.changetodataview.tooltip", null, null, e -> changer.changeTo(CHANGEVIEW_DATA_VIEW)); 
		changeToChaptersListViewButton = Utils.createButton("westcontrol.button.changetochaptersview.icon", "westcontrol.button.changetochaptersview.tooltip", null, null, e -> changer.changeTo(CHANGEVIEW_CHAPTERS_LIST_VIEW));
		mostRecentsChapterButton = Utils.createButton("westcontrol.button.mostrecentchapter.icon_large", "westcontrol.button.mostrecentchapter.tooltip", null, default_foreground, e -> changer.changeTo(OPEN_MOST_RECENT_CHAPTER));

		changeToDataViewButton.setVisible(false);
		changeToChaptersListViewButton.setVisible(false);
		mostRecentsChapterButton.setVisible(false);

		changeToDataViewButton.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		changeToChaptersListViewButton.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		mostRecentsChapterButton.setAlignmentY(Component.BOTTOM_ALIGNMENT);

		p = Utils.createJPanel(new BoxLayout(null, BoxLayout.Y_AXIS));
		p.add(changeToChaptersListViewButton);
		p.add(changeToDataViewButton);
		p.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		minimizedControlPanel.add(mostRecentsChapterButton);
		minimizedControlPanel.add(p);
		p = null;

		//--------------------------------------------------------------------------------------------------------

		maximizedControlPanel = Utils.createJPanel(new GridLayout(2, 1));

		JPopupMenu sortingPopupMenu = new JPopupMenu();
		sortingPopupMenu.setOpaque(false);

		Border MENU_ITEM_BORDER = UIManager.getBorder("MenuItem.border");
		SortingMethod startupSorting = RH.getStartupSortingMethod();

		JMenuItem[] firstClick = {null};
		Function<SortingMethod, JMenuItem> createMenuItem = method ->  {

			String key = "westcontrol.sort.menuitem."+method.toString().toLowerCase();

			JMenuItem b = new JMenuItem(RH.getString(key+".text"),
					RH.getImageIcon(key+".icon"));

			b.setName(method.toString());
			b.setToolTipText(RH.getString(key+".tooltip"));
			b.setDoubleBuffered(false);
			b.setFont(default_font);
			b.setForeground(default_foreground);
			b.setBackground(default_background);
			b.setBorder(MENU_ITEM_BORDER);
			b.addActionListener(e -> sortingAction(method, b));

			if(method == startupSorting)
				firstClick[0] = b;
			return b;

		};

		for (SortingMethod o : new  SortingMethod[]{
				SortingMethod.ALPHABETICALLY_INCREASING,
				SortingMethod.RANKS_INCREASING,
				SortingMethod.READ_TIME_DECREASING,
				SortingMethod.UPDATE_TIME_DECREASING
		}
				) {
			sortingPopupMenu.add(createMenuItem.apply(o));
		}

		JPanel p3 = Utils.createJPanel(new BoxLayout(null, BoxLayout.Y_AXIS));

		sortingButton = new SortingButton();
		sortingButton.setText("Alphabetically    ");  
		sortingButton.setForeground(default_foreground);
		sortingButton.setIcon(RH.getImageIcon("westcontrol.button.listfavorites.icon"));
		sortingButton.addActionListener(e -> sortingPopupMenu.show(sortingButton, 5, 0));


		listFavoritesButton = Utils.createButton("westcontrol.button.listfavorites.icon", "westcontrol.button.listfavorites.tooltip", "westcontrol.button.listfavorites.text",default_foreground, e -> sortingAction(FAVORITES, e.getSource()));
		listRecentsButton = Utils.createButton("westcontrol.button.listrecents.icon", "westcontrol.button.listrecents.tooltip", "westcontrol.button.listrecents.text", default_foreground, e -> sortingAction(SortingMethod.READ_TIME_DECREASING, e.getSource()));

		listdeleteQueuedButton = Utils.createButton("westcontrol.button.listdelete.icon", "westcontrol.button.listdelete.tooltip", "westcontrol.button.listdelete.text", default_foreground, e -> sortingAction(DELETE, e.getSource()));

		openTagsDialogButton = Utils.createButton("westcontrol.button.open.tagssearch.icon", "westcontrol.button.open.tagssearch.tooltip", "westcontrol.button.open.tagssearch.text", default_foreground, e -> {
			if(searchManeger == null)
				searchManeger = new SearchManeger();
			searchManeger.openTagsSearch();

		});

		Color buttonClickedColor = RH.getColor("westcontrol.sortingButtons.button.backgroundcolor_whenclicked");

		for(JButton b : new JButton[]{sortingButton,listFavoritesButton,listRecentsButton,listdeleteQueuedButton,openTagsDialogButton}){
			b.setFont(default_font);
			setAlignmentsTopLeft.accept(b);
			b.setAlignmentY(Component.TOP_ALIGNMENT);
			b.setAlignmentX(Component.LEFT_ALIGNMENT);
			b.setIconTextGap(25);
			b.setBackground(buttonClickedColor);
			p3.add(b);
		}

		listdeleteQueuedButton.setVisible(false);

		maximizedControlPanel.add(p3);
		p = null;

		searchButtonContainer = Utils.createJPanel(new BorderLayout());

		searchButton = Utils.createButton("westcontrol.button.search.icon", "westcontrol.button.search.tooltip", "westcontrol.button.search.text", default_foreground, e -> {
			JButton b = (JButton)e.getSource();

			if(searchManeger == null)
				searchManeger = new SearchManeger();

			searchButtonContainer.remove(b);
			JLabel l = new JLabel("<html>On each charactor click, word is searched in manga name<br><b>Press Enter to search in manga description</b></html>");
			l.setHorizontalTextPosition(SwingConstants.LEFT);
			l.setVerticalTextPosition(SwingConstants.BOTTOM);
			l.setForeground(default_foreground);
			l.setAlignmentX(Component.LEFT_ALIGNMENT);
			l.setAlignmentY(Component.BOTTOM_ALIGNMENT);
			JPanel pq = Utils.createJPanel(new GridLayout(3, 1 , 1 , 5));

			pq.add(l);
			pq.add(searchManeger.getTextSearchComponent());

			JPanel ps = Utils.createJPanel(new FlowLayout(FlowLayout.LEFT));

			JButton reset = Utils.createButton(null, null, null, default_background, e1 -> searchManeger.resetTextSearch());
			JButton remove = Utils.createButton(null, null, null, default_background, null);

			reset.setContentAreaFilled(true);
			reset.setBackground(default_foreground);
			remove.setContentAreaFilled(true);
			remove.setBackground(default_foreground);
			reset.setToolTipText("reset current search");
			remove.setToolTipText("remove search box(will reset first)");

			reset.setText("reset");
			remove.setText("remove");

			remove.addActionListener(e1 -> {
				searchManeger.resetTextSearch();
				reAddSearchButton();
			});

			ps.add(reset);
			ps.add(remove);
			pq.add(ps);

			searchButtonContainer.add(pq, BorderLayout.SOUTH);
			searchButtonContainer.revalidate();
			searchButtonContainer.repaint();
		});

		searchButton.setFont(RH.getFont("westcontrol.button.search.font"));

		searchButtonContainer.add(searchButton, BorderLayout.SOUTH);
		searchButton.setAlignmentY(Component.TOP_ALIGNMENT);
		searchButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		settingsButton = Utils.createButton("westcontrol.button.settings.icon", "westcontrol.button.settings.tooltip", null,getForeground(), e -> Utils.showHidePopup("not working (yet)", 1000));
		changeToListElementTypeButton = Utils.createButton("westcontrol.button.changeelementtype.list.icon", "westcontrol.button.changeelementtype.list.tooltip", null, default_foreground, e -> toggleElementType(CHANGETYPE_LIST));
		changeToThumbElementTypeButton =  Utils.createButton("westcontrol.button.changeelementtype.thumb.icon", "westcontrol.button.changeelementtype.thumb.tooltip", null, default_foreground, e -> toggleElementType(CHANGETYPE_THUMB));
		JButton mostRecentsChapterButton2 = Utils.createButton("westcontrol.button.mostrecentchapter.icon", "westcontrol.button.mostrecentchapter.tooltip", null, default_foreground, e -> changer.changeTo(OPEN_MOST_RECENT_CHAPTER));
		JButton mostRecentsMangaButton = Utils.createButton("westcontrol.button.mostrecentmanga.icon", "westcontrol.button.mostrecentmanga.tooltip", null, default_foreground, e -> changer.changeTo(OPEN_MOST_RECENT_MANGA));

		p = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));
		p.add(mostRecentsChapterButton2);
		p.add(mostRecentsMangaButton);
		p.add(changeToListElementTypeButton);
		p.add(changeToThumbElementTypeButton);
		p.add(settingsButton);

		ViewElementType type = RH.getStartupViewElementType();

		changeToThumbElementTypeButton.setVisible(type == ViewElementType.LIST);
		changeToListElementTypeButton.setVisible(type == ViewElementType.THUMB);

		JPanel p2 = Utils.createJPanel(new BorderLayout());
		p2.add(searchButtonContainer, BorderLayout.NORTH);
		p2.add(p, BorderLayout.SOUTH);
		p = p2;

		setAlignmentsBottomLeft.accept(changeToListElementTypeButton);
		setAlignmentsBottomLeft.accept(mostRecentsChapterButton2);
		setAlignmentsBottomLeft.accept(mostRecentsMangaButton);
		setAlignmentsBottomLeft.accept(changeToThumbElementTypeButton);
		setAlignmentsBottomLeft.accept(settingsButton);

		maximizedControlPanel.add(p);

		centrePanel.add(maximizedControlPanel);

		p = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));

		Font font = RH.getFont("westcontrol.bottom.text.font");
		Border border = new CompoundBorder(new MatteBorder(0, 0, 0, 2, default_foreground), new EmptyBorder(5, 20, 5, 20));

		JLabel numberOfMangasOnDisplay = new JLabel("Count");
		numberOfMangasOnDisplay.setForeground(default_foreground);
		p.add(numberOfMangasOnDisplay);
		numberOfMangasOnDisplay.setToolTipText(RH.getString("westcontrol.bottom.mangasondisplaycount.label.tooltip"));
		numberOfMangasOnDisplay.setBorder(border);
		numberOfMangasOnDisplay.setOpaque(true);

		int animationDelay = RH.getInt("westcontrol.bottom.animation.delay");
		Color animationColor = RH.getColor("westcontrol.bottom.animation.background");

		numberOfMangasOnDisplay.setBackground(default_background);

		Timer mangaOnDisplayTimer = new Timer(animationDelay, e -> numberOfMangasOnDisplay.setBackground(default_background));
		mangaOnDisplayTimer.setRepeats(false);

		numberOfMangasOnDisplay.setFont(font);
		this.mangasOnDisplay = MangaManeger.mangas();
		DeleteQueue dq = mangasOnDisplay.getDeleteQueue();

		mangasOnDisplay.getMangaIdsListener()
		.addChangeListener((indices, code) -> {
			if(code == MangaManegerStatus.MOD_MODIFIED){
				numberOfMangasOnDisplay.setText(String.valueOf(mangasOnDisplay.length()));
				numberOfMangasOnDisplay.setBackground(animationColor);
				mangaOnDisplayTimer.restart();
			}
		});

		dq.addChangeListener((manga, code) -> listdeleteQueuedButton.setVisible(!dq.isEmpty()));

		JLabel ramUsed = new JLabel();
		ramUsed.setForeground(default_foreground);
		ramUsed.setOpaque(true);
		p.add(ramUsed);
		ramUsed.setToolTipText(RH.getString("westcontrol.bottom.ram.label.tooltip"));
		p.setFont(font);
		ramUsed.setBorder(border);
		EventQueue.invokeLater(() -> ramUsed.setText(String.valueOf(getUsedRamAmount())));

		ramUsed.setBackground(RH.getColor("westcontrol.bottom.animation.background"));

		Timer ramOpaqueTimer = new Timer(animationDelay, e -> ramUsed.setBackground(default_background));
		ramOpaqueTimer.setRepeats(false);
		ramUsed.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() > 0) {
					gcDialog();
				}
			}; 
		});

		Timer ramTimer = new Timer(RH.getInt("westcontrol.bottom.ram.label.update.delay")*1000, e -> {
			ramUsed.setText(String.valueOf(getUsedRamAmount()));
			ramUsed.setBackground(animationColor);
			ramOpaqueTimer.restart();
		});

		ramTimer.start();

		/**
		 *        JButton errorCount = Utils.createButton(null, "westcontrol.bottom.errorscount.button.tooltip", null, RH.getColor("westcontrol.bottom.errorscount.foreground"), e -> Utils.openErrorDialog());
        p.add(Box.createGlue());
        p.add(errorCount);


        errorCount.setText("0");
        errorCount.setFont(font);
        errorCount.setOpaque(true);
        errorCount.setBackground(RH.getColor("westcontrol.bottom.errorscount.background"));
        errorCount.setAlignmentX(Component.RIGHT_ALIGNMENT);
        errorCount.setVisible(false);

        Utils.setErrorCountListenerConsumer(i -> {
            errorCount.setVisible(true);
            errorCount.setText(String.valueOf(i));
        });
		 */

		p.setBorder(border);
		add(p, BorderLayout.SOUTH);

		Utils.addExitTasks(() -> {
			if(this.searchManeger != null)
				this.searchManeger.dispose();
		});

		addKeyStrokes();
		firstClick[0].doClick();
	}
	protected void gcDialog() {
		Dialog dialog = new Dialog((JFrame)null, "Memory Usage", true);
		TextArea t = new TextArea(null, 5, 30, TextArea.SCROLLBARS_NONE);

		t.setBackground(Color.white);

		Runnable run = () -> {
			Runtime r = Runtime.getRuntime();
			StringBuilder sb = new StringBuilder();
			Function<Long, String> f = l -> MyUtilsBytes.bytesToHumanReadableUnits(l, false);
			sb.append(" Total Memory: ").append(f.apply(r.totalMemory())).append('\n')
			.append("  Free Memory: ").append(f.apply(r.freeMemory())).append('\n')
			.append("   Max Memory: ").append(f.apply(r.maxMemory())).append('\n')
			.append("  used Memory: ").append(f.apply(r.totalMemory() - r.freeMemory())).append('\n');

			t.setText(sb.toString());

		};
		run.run();

		dialog.setBackground(Color.white);

		t.setEditable(false);
		t.setFont(new Font("Consolas", Font.PLAIN, 16));
		dialog.add(t);

		Button btn = new Button("System.gc()");
		btn.setFont(t.getFont());
		btn.addActionListener(e -> {
			t.setText("WAIT");
			System.gc();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {}
			run.run();
		});
		dialog.add(btn, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) { dialog.dispose(); }
		});

		dialog.setVisible(true);
	}

	/**
	 * @return current used ram (in Mb) 
	 */
	public long getUsedRamAmount(){
		return  (RUNTIME.totalMemory() - RUNTIME.freeMemory())/1048576L;
	}
	/**
	 * add all keystrokes from this method 
	 */
	private void addKeyStrokes() {
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true);

		getActionMap().put("back_button_click", new AbstractAction() {
			private static final long serialVersionUID = 4283130191318965674L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if(backToDockButton.isVisible())
					backToDockButton.doClick();
			}
		});

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "back_button_click");

	}

	private void toggleElementType(Change changetypeList) {
		changeToThumbElementTypeButton.setVisible(changetypeList == CHANGETYPE_LIST);
		changeToListElementTypeButton.setVisible(changetypeList == CHANGETYPE_THUMB);
		changer.changeTo(changetypeList);
	}
	
	private static final Object DELETE = new Object();
	private static final Object FAVORITES = new Object();

	/**
	 * if both param are null, then its considered that TagsDialog is called
	 * @param method
	 * @param source
	 */
	private void sortingAction(Object method, Object source) {
		if(searchManeger != null)
			searchManeger.basicClear();

		if(method instanceof SortingMethod) {
			try {
				mangasOnDisplay.sort((SortingMethod)method, false);
			} catch (SQLException | IOException e) {
				LOGGER.log(Level.SEVERE, "failed to sort", e);
			}
		} else if(method == DELETE)
			mangasOnDisplay.setFilter(Mangas.ONLY_DELETE_QUEUED);
		else if(method == FAVORITES)
			mangasOnDisplay.setFilter(Mangas.ONLY_FAVORITES);

		sortingButton.setOpaque(false);
		listFavoritesButton.setOpaque(false);
		listRecentsButton.setOpaque(false);
		listdeleteQueuedButton.setOpaque(false);
		openTagsDialogButton.setOpaque(false);

		if(source instanceof JMenuItem){
			JMenuItem mi = (JMenuItem) source;
			sortingButton.setText(mi.getText()+"    ");
			sortingButton.setIcon(mi.getIcon());
			sortingButton.setOpaque(true);
		}
		else{
			((JButton) source).setOpaque(true);
			sortingButton.setText(RH.getString("westcontrol.button.sorting.text"));
			sortingButton.setIcon(RH.getImageIcon("westcontrol.button.sorting.icon"));
		}

		reAddSearchButton();

		if(listRecentsButton.equals(source))
			changer.changeTo(CHANGETYPE_RECENT);
		else
			changer.changeTo(CHANGETYPE_NORMAL);

		revalidate();
		repaint();
	}

	private void reAddSearchButton() {
		if(searchButtonContainer.getComponentCount() == 1 && searchButtonContainer.getComponent(0) instanceof JButton)
			return;

		searchButtonContainer.removeAll();
		searchButtonContainer.add(searchButton, BorderLayout.SOUTH);
		searchButtonContainer.revalidate();
		searchButtonContainer.repaint();
	}

	private void clickShowHideButton(boolean showPanelButtonClicked) {
		centrePanel.remove(maximizedControlPanel);
		centrePanel.remove(minimizedControlPanel);
		centrePanel.add(showPanelButtonClicked ? maximizedControlPanel : minimizedControlPanel);
		hidePanelButton.setVisible(showPanelButtonClicked);
		showPanelButton.setVisible(!showPanelButtonClicked);
		menuButton.setVisible(!showPanelButtonClicked);
		iconifyAppButton.setVisible(showPanelButtonClicked);
		closeAppButton.setVisible(showPanelButtonClicked);
	}

	private final class SortingButton extends JButton {
		private static final long serialVersionUID = -5646964856160929709L;
		private final Image img = RH.getImageIcon("popupmenu.icon").getImage();

		public SortingButton() {
			super(RH.getString("westcontrol.button.sorting.text"), RH.getImageIcon("westcontrol.button.sorting.icon"));
			setToolTipText(RH.getString("westcontrol.button.sorting.tooltip"));
			setBorderPainted(false);
			setContentAreaFilled(false);
			setFocusPainted(false);
			setFocusable(false);
			setDoubleBuffered(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			g.drawImage(img, getWidth() - img.getWidth(null), getHeight()/2 - img.getHeight(null)/2, null);
			g.dispose();
		}
	}	


	@Deprecated
	public void hibernate() {
		if(sleeping)
			return; 

		reAddSearchButton();

		if(searchManeger != null && searchManeger.hibernate())
			searchManeger = null;

		sleeping = true;
	}

	private boolean sleeping = false;
	public void wakeUp() {
		if(!sleeping || searchManeger == null)
			return;

		boolean[] b =  searchManeger.wakeUp();

		if(b[0])
			searchButton.doClick();
	}

	public void viewChanged(Views view){
		if(view == Views.VIEWELEMENTS_VIEW)
			showPanelButton.doClick();
		else{
			hidePanelButton.doClick();
			showPanelButton.setVisible(false);
		}

		backToDockButton.setVisible(view != Views.VIEWELEMENTS_VIEW);
		changeToDataViewButton.setVisible(view == Views.CHAPTERS_LIST_VIEW);
		changeToChaptersListViewButton.setVisible(view == Views.DATA_VIEW);
		mostRecentsChapterButton.setVisible(view == Views.CHAPTERS_LIST_VIEW || view == Views.DATA_VIEW);
	}
}

