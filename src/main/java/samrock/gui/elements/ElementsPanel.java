package samrock.gui.elements;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javafx.application.Platform;
import sam.reference.ReferenceType;
import samrock.RH;
import samrock.ViewElementType;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.DeleteQueue;
import samrock.manga.maneger.MangaManeger;
import samrock.manga.maneger.MangaManegerStatus;
import samrock.manga.maneger.Mangas;
import samrock.manga.maneger.Operation;


class ElementsPanel  extends JPanel {
	private static final long serialVersionUID = -1587989499833629180L;

	static final LayoutManager thumbViewLayoutManeger = new WrapLayout(FlowLayout.CENTER, 15, 15);
	static final LayoutManager listViewLayoutManeger = new GridLayout(0, 1, 0, 10);

	private static final Color THUMBVIEW_DOCK_BACKGROUND;
	private static final Color LISTVIEW_DOCK_BACKGROUND;

	static {
		THUMBVIEW_DOCK_BACKGROUND = RH.getColor("thumbview.dock.color");
		LISTVIEW_DOCK_BACKGROUND = RH.getColor("listview.dock.color");
	}

	private ViewElementType currentElementType ;

	private boolean isElementTypeThumb;
	private LinkedList<ViewElement> added = new LinkedList<>();
	private ReferenceList<ViewElement> removed = new ReferenceList<>(ReferenceType.SOFT, ViewElement::new) ;
	private final ArrayList<ViewElement> visibleElements = new ArrayList<>();
	private final int rowCount;
	private int start, elementPerRow;
	private final Mangas mod;
	private boolean mangaDeletedInternally = false;
	private final DeleteQueue deleteQueue;

	public ElementsPanel() {
		super(true);

		currentElementType = RH.getStartupViewElementType();

		this.rowCount = RH.getInt("thumbview.thumbs.row.count");

		setFocusCycleRoot(true);
		setOpaque(true);

		setIsElementTypeThumb();
		setCurrentElementType(currentElementType);

		this.mod = MangaManeger.mangas();
		deleteQueue = mod.getDeleteQueue();

		deleteQueue.addChangeListener((m, type) -> {
			if(mangaDeletedInternally)
				return;
			ViewElement e = get(m);
			if(e != null)
				e.setMangaDeleted(type == Operation.ADDED);
		});
		this.mod.addChangeListener((md, status) -> {
			if(status == MangaManegerStatus.MOD_MODIFIED)
				reset(false);
		});
		
		Platform.runLater(() -> reset(true));
	}

	protected void deleteManga(ViewElement e) {
		if(JOptionPane.showConfirmDialog(null, "<html>sure to delete?<br>"+e.getManga().getMangaName()) != JOptionPane.YES_OPTION)
			return;
		mangaDeletedInternally = true;
		deleteQueue.add(e.getManga());
		e.setMangaDeleted(true);
		mangaDeletedInternally = false;
	}

	private void setIsElementTypeThumb() {
		this.isElementTypeThumb = currentElementType == ViewElementType.THUMB  ||  currentElementType == ViewElementType.RECENT_THUMB;
	}
	public ViewElementType getCurrentElementType() {
		return currentElementType;
	}
	void setCurrentElementType(ViewElementType elementType) {
		setIsElementTypeThumb();
		ViewElement.setCurrentElementType(elementType);
		currentElementType = elementType;

		each(ViewElement::changeElementType);
		removed.forEach(ViewElement::changeElementType);

		setLayout(isElementTypeThumb ? thumbViewLayoutManeger : listViewLayoutManeger);
		setBackground(isElementTypeThumb ? THUMBVIEW_DOCK_BACKGROUND : LISTVIEW_DOCK_BACKGROUND);
	}

	private void each(Consumer<ViewElement> c) {
		for (int i = 0; i < length(); i++) 
			c.accept(getAt(i));
	}
	public boolean isElementTypeThumb() {
		return isElementTypeThumb;
	}

	public ViewElement getAt(int n) {
		return added.get(n);
	}
	public Component firstComponent() {
		return added.getFirst();
	}
	public Component lastComponent() {
		return added.getLast();
	}
	public ViewElement first() {
		return getAt(0);
	}
	public ViewElement last() {
		return getAt(length() - 1);
	}

	ViewElement get(MinimalManga manga) {
		for (ViewElement v : added) {
			if(manga == v.getManga())
				return v;
		}
		return null;
	}
	public void loadIconTextViewElements() {
		if(visibleElements.isEmpty()) return;
		EventQueue.invokeLater(() -> visibleElements.forEach(ViewElement::load));
	}
	public int length() {
		return added.size();
	}

	private boolean fullResetPending;
	
	private void reset(boolean full) {
		removeAll0();
		start = 0;

		if(mod.isEmpty()) {
			fullResetPending = full;
			return;
		}

		if(isElementTypeThumb()) {
			if(fullResetPending || full) 
				compute();

			int length = rowCount*elementPerRow;

			for (int i = 0; i < length; i++) 
				add0();
		} else {
			for (int i = 0; i < 50; i++) 
				add0();
			elementPerRow = -1;
		}
		revalidate();
		repaint();
	}

	private void compute() {
		fullResetPending = false;
		elementPerRow = 0;
		int y = 0;

		while(true) {
			ViewElement v = add0();
			validate();

			if(y == 0)
				y = v.getLocationOnScreen().y;

			if(y != v.getLocationOnScreen().y) {
				removeLast();
				break;
			}
			elementPerRow++;

			if(elementPerRow > 50)
				throw new RuntimeException("more then 50 ViewElement per row");
		}
		removeAll0();
	}

	public void resetModIndices() {
		int length = length();

		for (int i = start, j = 0; start < mod.length() && j < length; i++,j++) {
			ViewElement e = getAt(j);
			e.setModIndex(i);
		}
	}

	public boolean isEmpty() {
		return length() == 0;
	}
	public boolean hasNext(ViewElement element) {
		return element != lastComponent();
	}

	private boolean hasPrevious(ViewElement element) {
		return element != firstComponent();
	}
	public void ensurePrevious(ViewElement element) {
		if(hasPrevious(element)) return;

		start--;
		removeLast();
		addFirst();
		resetModIndices();
	}
	public void ensureNext(ViewElement element) {
		if(hasNext(element)) return;

		start++;
		removeFirst();
		add0();
		resetModIndices();
	}
	public void ensureModIndex(int modIndex) {
		if(modIndex == start || modIndex <= last().getModIndex())
			return;

		boolean b = modIndex < start;

		start = start + elementPerRow*(b ? -1 : 1);
		int ln = length();

		for (int i = 0; i < elementPerRow && i < ln; i++) {
			if(b) removeLast();
			else removeFirst();
		} 

		ln = mod.length();
		for (int i = 0; i < elementPerRow && i < ln; i++) 
			add0(create(), b ? i : length() - 1);

		resetModIndices();
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int elementPerRow() {
		return elementPerRow;
	}

	@Override
	protected void addImpl(Component comp, Object constraints, int index) {
		// TODO Auto-generated method stub
		super.addImpl(comp, constraints, index);
	}

	@Override public Component add(Component comp) { throw new IllegalAccessError(); }
	@Override public Component add(String name, Component comp) { throw new IllegalAccessError(); }
	@Override public Component add(Component comp, int index) { throw new IllegalAccessError(); }
	@Override public void add(Component comp, Object constraints) { throw new IllegalAccessError(); }
	@Override public void add(Component comp, Object constraints, int index) { throw new IllegalAccessError(); }
	@Override public void remove(int index)  { throw new IllegalAccessError(); }
	@Override public void remove(Component comp)  { throw new IllegalAccessError(); }
	@Override public void removeAll()  { throw new IllegalAccessError(); }

	public void removeFirst() { remove0(0); }
	public void removeLast() { remove0(maxIndex()); }

	public void addFirst() { 
		added.addFirst(removed.poll());
	}
	private int maxIndex() {
		return length() -1;
	}

	private void remove0(int index) {
		ViewElement v;
		if(index == 0) 
			v = added.removeFirst();
		else if(index == maxIndex())
			v = added.removeLast();
		else
			v = added.remove(index);

		removed.add(v);
		v.setIndex(-1);
		super.remove(index);
	}
	@Deprecated
	private void add0(ViewElement e, int index) {
		super.add(e, index);
		added.add(index, e);
		e.setIndex(index);
	}
	private ViewElement add0() {
		ViewElement v = create();
		super.add(v);
		added.add(v);
		v.setIndex(maxIndex());
		return v;
	}
	private ViewElement create() {
		ViewElement v = removed.poll();
		v.setModIndex(-1);
		return v;
	}
	private void removeAll0() {
		added.forEach(s -> s.setIndex(-1));
		removed.addAll(added);
		added.clear();
		super.removeAll();
	}
	void toLast() {
		setStart(mod.length()*-1);
		last().requestFocus();
	}
	void toFirst() {
		setStart(0);
		first().requestFocus();
	}

	void gotoIndex(int index) {
		if(index <= 0)
			toFirst();
		if(index >= mod.length())
			toLast();
		else {
			ensureModIndex(index);
			getAt(index).requestFocus();
			//TODO ensure row
		}
	}

	public ViewElement getCurrentMangaView() {
		Manga m = manager.getCurrentManga();
		if(m == null) return null;

		return get(m);
	}

	public void updateCurrentMangaViewElement() {
		ViewElement v = get(manager.getCurrentManga());
		if(v != null)
			v.reset();
	}
}