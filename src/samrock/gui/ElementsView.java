package samrock.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Timer;

import samrock.manga.MangaManeger;
import samrock.manga.MinimalManga;
import samrock.utils.RH;
import samrock.utils.SortingMethod;
import samrock.utils.Utils;
import samrock.utils.ViewElementType;

final class ElementsView extends JPanel{

	private static final long serialVersionUID = -969394743514855344L;
	public static final int VIEW_ELEMENT_CLICKED = 0x400;
	

	private static ElementsView instance;

	static ElementsView getInstance(IntConsumer viewPanelWatcher) {

		if(instance == null)
			instance = new ElementsView(viewPanelWatcher);

		return instance;
	}

	private final LayoutManager thumbViewLayoutManeger = new WrapLayout(FlowLayout.CENTER, 15, 15);
	private final LayoutManager listViewLayoutManeger = new GridLayout(0, 1, 0, 10);

	private final JPanel centerPanel;
	private final JScrollPane scrollPane;
	private final JScrollBar verticalScrollBar;
	private final JViewport viewport;
	private final Timer viewElementsloaderTimer;
	private final IntConsumer watcher;
	private boolean mangaDeletedInternally = false;

	private final Color THUMBVIEW_DOCK_BACKGROUND;
	private final Color LISTVIEW_DOCK_BACKGROUND;

	/**
	 * arrayIndicesOfMangasOnDisplay
	 */
	private int[] mangasOnDisplay;
	private final MangaManeger mangaManeger;
	private final JPanel sortingPanel;
	private ViewElement[] viewElements;
	ArrayList<ViewElement> visibleElements = new ArrayList<>();

	//listeners
	private final MouseAdapter mouseAdapter;
	private final KeyAdapter keyAdapter;
	private final FocusListener focusListener;

	private int selectedIndex;
	/**
	 * currentElementType == ViewElementType.THUMB || currentElementType = ViewElementType.RECENT_THUMB
	 */
	private boolean isElementTypeThumb;

	private boolean isSleeping = false;
	private int focusedElementIndex = -1;

	public void hibernate(){
		if(isSleeping)
			return;

		isSleeping = true;

		if(viewElements != null){
			for (ViewElement v : visibleElements) {
				if(v.isFocusOwner()){
					focusedElementIndex = v.ARRAY_INDEX;
					break;
				} }
		}
		
		centerPanel.removeAll();
		viewElements = null;
		visibleElements.forEach(ViewElement::unload);
		visibleElements.clear();		
		visibleElements = null;
		mangasOnDisplay = null;
	} 
	public void wakeUp(){
		if(!isSleeping)
			return;

		isSleeping = false;

		mangasOnDisplay = mangaManeger.getMangasOnDisplay();

		viewElements = new ViewElement[mangaManeger.getMangasCount()];
		for (int i = 0; i < viewElements.length; i++) viewElements[i] = new ViewElement(i);

		visibleElements = new ArrayList<>();
		reset();

		EventQueue.invokeLater(() -> {
			if(focusedElementIndex != -1){
				final ViewElement v = viewElements[focusedElementIndex];
				EventQueue.invokeLater(() -> viewport.scrollRectToVisible(calculateRect(v)));
			}
			else
				EventQueue.invokeLater(() -> verticalScrollBar.setValue(0));

			focusedElementIndex = -1;
		});
	}

	private ElementsView(IntConsumer viewPanelWatcher) {
		super(new BorderLayout(), false);
		
		isElementTypeThumb = RH.getStartupViewElementType() == ViewElementType.THUMB; 

		centerPanel = new JPanel(isElementTypeThumb ? thumbViewLayoutManeger : listViewLayoutManeger, false);

		watcher = viewPanelWatcher;
		THUMBVIEW_DOCK_BACKGROUND = RH.getColor("thumbview.dock.color");
		LISTVIEW_DOCK_BACKGROUND = RH.getColor("listview.dock.color");

		mangaManeger = MangaManeger.getInstance();

		mouseAdapter = getMouseAdapter();
		focusListener = getFocusListener();
		keyAdapter = getKeyAdapter(); 

		ViewElement.initConstants(mouseAdapter, keyAdapter, focusListener);

		centerPanel.setOpaque(true);
		centerPanel.setBackground(isElementTypeThumb ? THUMBVIEW_DOCK_BACKGROUND : LISTVIEW_DOCK_BACKGROUND);
		centerPanel.setFocusCycleRoot(true);

		//NORTH------------------------------------------------
		sortingPanel = new JPanel(false);
		sortingPanel.setLayout(new GridLayout(1, 2));
		sortingPanel.setOpaque(true);
		sortingPanel.setBackground(RH.getColor("viewpanel.sortingpanel.background"));

		JPopupMenu popupMenu = new JPopupMenu();
		BiConsumer<String, ActionListener> miCreate = (text, actionListener) -> {
			JMenuItem mi = new JMenuItem(text);
			mi.setDoubleBuffered(false);
			if(actionListener == null)
				mi.addActionListener(e -> Utils.showHidePopup("Not(Working yet)", 1000));
			else
				mi.addActionListener(actionListener);

			popupMenu.add(mi);
		};

		miCreate.accept("Select", null);
		miCreate.accept("Unselect All", null);
		miCreate.accept("Delete Selected", null);

		JPanel p = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));

		JButton menuButton = Utils.createMenuButton(e -> popupMenu.show((JButton)e.getSource(), 0, 0));

		menuButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		menuButton.setAlignmentY(Component.CENTER_ALIGNMENT);

		p.add(menuButton);
		sortingPanel.add(p);

		JPanel p2 = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));
		p2.add(Box.createGlue());

		final JButton[] sortingButtons = Stream.of(SortingMethod.values())
				.filter(s  -> s != SortingMethod.DELETE_QUEUED && s != SortingMethod.FAVORITES)
				.map(method -> {
					String key = "viewpanel.sortingpanel.button."+method.toString().toLowerCase();
					JButton j =  Utils.createButton(key+".icon", key+".tooltip", null, null, e -> mangaManeger.changeCurrentSortingMethod(method, true));
					j.setName(method.toString());
					j.setAlignmentX(Component.RIGHT_ALIGNMENT);
					j.setAlignmentY(Component.TOP_ALIGNMENT);
					p2.add(j);
					return j;
				}).toArray(JButton[]::new);

		sortingPanel.add(p2);

		scrollPane = new JScrollPane(centerPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		verticalScrollBar = scrollPane.getVerticalScrollBar();
		viewport = scrollPane.getViewport();

		add(sortingPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		viewElementsloaderTimer = new Timer(300, e ->  loadIconTextViewElements());
		viewElementsloaderTimer.setRepeats(false);
		verticalScrollBar.addAdjustmentListener(e -> {
			addListenersViewElements();
			viewElementsloaderTimer.restart();
		});

		viewElements = new ViewElement[mangaManeger.getMangasCount()];
		for (int i = 0; i < viewElements.length; i++) viewElements[i] = new ViewElement(i);

		mangaManeger.addMangaManegerWatcher(code -> {
			if(code == MangaManeger.MOD_MODIFIED){
				mangasOnDisplay = mangaManeger.getMangasOnDisplay();
				reset();

				SortingMethod sm = mangaManeger.getCurrentSortingMethod();
				String sms = sm.opposite().toString();
				boolean b = mangasOnDisplay.length != 0;
				for (JButton bt : sortingButtons) bt.setVisible(b && sms.equals(bt.getName()));
				menuButton.setVisible(b);
				viewElementsloaderTimer.restart();
			}
			else if(!mangaDeletedInternally && code == MangaManeger.DQ_UPDATED){
				if(viewElements != null)
					viewElements[mangaManeger.getCurrentManga().ARRAY_INDEX].resetDeleted();
			}
		});

		
	}

	public ViewElementType getCurrentElementType(){
		return ViewElement.getCurrentElementType();
	}

	private KeyAdapter getKeyAdapter() {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				ViewElement element = (ViewElement)e.getSource();
				int key = e.getKeyCode();

				if(key == KeyEvent.VK_ENTER) {
					doMangaClick(e.getSource());
					return;
				}
				if(key == KeyEvent.VK_DELETE) {
                    deleteManga(e.getSource());
                    return;
                }

				if(!isElementTypeThumb){
					if(key == KeyEvent.VK_UP)
						element.transferFocusBackward();
					else if(key == KeyEvent.VK_DOWN)
						element.transferFocus();
					return;
				}

				if(key == KeyEvent.VK_RIGHT)
					element.transferFocus();
				else if(key == KeyEvent.VK_LEFT)
					element.transferFocusBackward();

				int x = element.getLocationOnScreen().x;
				int y = element.getLocationOnScreen().y;

				int posInArray = element.ARRAY_INDEX;
				int index = 0;

				for (int i : mangasOnDisplay) {
					if(posInArray == i) break;
					else index++;
				}

				ViewElement element2 = null;

				if(key == KeyEvent.VK_DOWN){
					if(y >= viewElements[mangasOnDisplay[mangasOnDisplay.length - 1]].getLocationOnScreen().y)
						element2 = viewElements[mangasOnDisplay[0]];
					else{
						index++;
						for (; index < mangasOnDisplay.length; index++) {
							element2 = viewElements[mangasOnDisplay[index]];

							if(element2.getLocationOnScreen().x >= x && element2.getLocationOnScreen().y > y)
								break;
						} } }
				else if(key == KeyEvent.VK_UP){
					if(y <= viewElements[mangasOnDisplay[0]].getLocationOnScreen().y)
						element2 = viewElements[mangasOnDisplay[mangasOnDisplay.length - 1]];
					else{
						index--;
						for (; index > -1; index--) {
							element2 = viewElements[mangasOnDisplay[index]];
							if(element2.getLocationOnScreen().x <= x && element2.getLocationOnScreen().y < y)
								break;
						} } }

				if(element2 != null)
					element2.requestFocus();
			}
		};
	}

	protected void doMangaClick(Object o) {
		selectedIndex = ((ViewElement)o).ARRAY_INDEX;
		watcher.accept(VIEW_ELEMENT_CLICKED);
	}
	protected void deleteManga(Object o) {
	    MinimalManga m = mangaManeger.getManga(((ViewElement)o).ARRAY_INDEX);
	    
	    if(JOptionPane.showConfirmDialog(null, "<html>sure to delete?<br>"+m.MANGA_NAME) != JOptionPane.YES_OPTION)
	        return;
	    mangaDeletedInternally = true;
	    mangaManeger.addMangaToDeleteQueue(m);
	    ((ViewElement)o).resetDeleted();
	    mangaDeletedInternally = false;
    }

	public int getArrayIndexOfSelectedManga() {
		return selectedIndex;
	}

	private FocusListener getFocusListener() {
		return new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				((ViewElement)(e.getSource())).setInFocus(false);
			}

			@Override
			public void focusGained(FocusEvent e) {
				ViewElement element = (ViewElement)e.getSource();

				if(element.getVisibleRect().height < element.getHeight()){
					if(element.ARRAY_INDEX == mangasOnDisplay[0])
						verticalScrollBar.setValue(0);
					else if(element.ARRAY_INDEX == mangasOnDisplay[mangasOnDisplay.length - 1])
						verticalScrollBar.setValue(verticalScrollBar.getMaximum());
					else
						viewport.scrollRectToVisible(calculateRect(element));
				}
				element.setInFocus(true);
			}
		};
	}

	private MouseAdapter getMouseAdapter() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() > 1) doMangaClick(e.getSource());
				else e.getComponent().requestFocus();
			}
		};
	}

	private synchronized void addListenersViewElements() {
		for (ViewElement v : visibleElements) 
			if(!v.isInVisibleRect())
				v.unload();
		
		visibleElements.clear();

		if(mangasOnDisplay == null || viewElements == null)
			return;

		int start = 0;
		while(start < mangasOnDisplay.length && !viewElements[mangasOnDisplay[start]].isInVisibleRect()) start++;

		int end = start;
		ViewElement v;
		while(end < mangasOnDisplay.length &&  (v = viewElements[mangasOnDisplay[end]]).isInVisibleRect()) {
			visibleElements.add(v);
			end++;
		};

		for (int i = start - 16 > 0 ? start - 16 : 0 ; 
		i < (end + 16 < mangasOnDisplay.length ? end + 16 : mangasOnDisplay.length); 
		i++) {
			viewElements[mangasOnDisplay[i]].addListeners();
		}
	}

	private void loadIconTextViewElements(){
		if(visibleElements.isEmpty())
			return;

		EventQueue.invokeLater(() -> visibleElements.forEach(ViewElement::load));
	}

	void reset(){

		EventQueue.invokeLater(() -> {
			

			centerPanel.removeAll();

			if(mangasOnDisplay.length == 0)
				return;

			for (int i : mangasOnDisplay) centerPanel.add(viewElements[i]);

			for (int i = 0; i < 24 && i < mangasOnDisplay.length; i++) {
				ViewElement v = viewElements[mangasOnDisplay[i]]; 
				v.addListeners();
				v.load();
			}

			verticalScrollBar.setValue(0);

			viewElementsloaderTimer.restart();
			EventQueue.invokeLater(() -> viewElements[mangasOnDisplay[0]].requestFocus());

			
		});
	}

	/**
	 * this is used to change type of ViewElements
	 * @param elementtype
	 */
	void changeElementType(ViewElementType elementtype) {
		if(getCurrentElementType() == elementtype)
			return;
		

		isElementTypeThumb = elementtype == ViewElementType.THUMB  ||  elementtype == ViewElementType.RECENT_THUMB;

		centerPanel.setLayout(isElementTypeThumb ? thumbViewLayoutManeger : listViewLayoutManeger);
		centerPanel.setBackground(isElementTypeThumb ? THUMBVIEW_DOCK_BACKGROUND : LISTVIEW_DOCK_BACKGROUND);

		ViewElement.setCurrentElementType(elementtype);

		Stream.of(viewElements).forEach(ViewElement::changeElementType);

		verticalScrollBar.setUnitIncrement(viewElements[0].getHeight()/4);

		EventQueue.invokeLater(() -> verticalScrollBar.setValue(0));
		
	}

	/**
	 * LocationOnScreen of element is relative to ElementsView, but we need LocationOnScreen of element relative to centraPanel (since we are scrolling on centralPanel)
	 * this can be obtain by element.LocationOnScreen - sortingPanel.size, this methods just to do that
	 * @param element
	 * @return
	 */
	private Rectangle calculateRect(ViewElement element) {
		Point p = new Point(element.getLocationOnScreen());
		p.y -= sortingPanel.getHeight();
		return new Rectangle(p, element.getSize());
	}

	public void updateCurrentMangaViewElement() {
		if(viewElements != null)
			viewElements[mangaManeger.getCurrentManga().ARRAY_INDEX].resetText();
	}

	void focusCurrentManga(){
		if(viewElements != null)
			EventQueue.invokeLater(() -> viewElements[mangaManeger.getCurrentManga().ARRAY_INDEX].requestFocus());
	}
}
