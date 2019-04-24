package samrock.gui.elements;

import static samrock.Utils.createJPanel;
import static samrock.Utils.showHidePopup;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import samrock.RH;
import samrock.Utils;
import samrock.api.Changer;
import samrock.api.ViewElementType;
import samrock.manga.maneger.MangaManeger;
import samrock.manga.maneger.Mangas;
import samrock.manga.maneger.SortingMethod;

public final class ElementsView extends JPanel {
	private static final long serialVersionUID = -969394743514855344L;

	private final ElementsRoller elementsRoller;

	/**
	 * arrayIndicesOfMangasOnDisplay
	 */
	final Mangas mod;
	private final JPanel sortingPanel;

	private int selectedIndex;
	/**
	 * currentElementType == ViewElementType.THUMB || currentElementType = ViewElementType.RECENT_THUMB
	 */
	private boolean isElementTypeThumb;
	private boolean isSleeping = false;

	/**
	 * FIXME to-be deleted use {@link WeakReference}
	 */
	@Deprecated
	//public void hibernate(){ 
	public void hibernate(){
		if(isSleeping)
			return;

		isSleeping = true;
		elementsRoller.sleep();
		//TODO
	}
	/**
	 * FIXME to-be deleted use {@link WeakReference}
	 */
	@Deprecated
	// public void wakeUp(){
	public void wakeUp(){
		if(!isSleeping)
			return;

		isSleeping = false;
		elementsRoller.reset0();
	}
	public ElementsView(Changer changer) {
		super(new BorderLayout(), false);
		this.isElementTypeThumb = RH.getStartupViewElementType() == ViewElementType.THUMB; 
		this.elementsRoller = new ElementsRoller(changer, this);

		this.mod = MangaManeger.mangas();
		
		//NORTH------------------------------------------------
		this.sortingPanel = sortingPanel();
		
		add(sortingPanel, BorderLayout.NORTH);
		add(elementsRoller, BorderLayout.CENTER);
	}
	
	private JPanel sortingPanel() {
		JPanel sortingPanel = new JPanel(false);
		sortingPanel.setLayout(new GridLayout(1, 2));
		sortingPanel.setOpaque(true);
		sortingPanel.setBackground(RH.getColor("viewpanel.sortingpanel.background"));

		JPanel p = createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));
		JButton menuButton = createMenuButton();
		p.add(menuButton);
		sortingPanel.add(p);

		JPanel p2 = createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));
		p2.add(Box.createGlue());

		final JButton[] sortingButtons = Stream.of(SortingMethod.values())
				.filter(s  -> s != SortingMethod.DELETE_QUEUED && s != SortingMethod.FAVORITES)
				.map(method -> {
					String key = "viewpanel.sortingpanel.button."+method.toString().toLowerCase();
					JButton j =  createButton(key+".icon", key+".tooltip", null, null, e ->mod.sort(method, true));
					j.setName(method.toString());
					j.setAlignmentX(Component.RIGHT_ALIGNMENT);
					j.setAlignmentY(Component.TOP_ALIGNMENT);
					p2.add(j);
					return j;
				}).toArray(JButton[]::new);

		sortingPanel.add(p2);
		
		mod.addChangeListener((indices, code) -> {
			if(code == MOD_MODIFIED){
				elementsRoller.reset0();

				SortingMethod sm = indices.getSorting() ;
				String sms = sm.reverse().toString();
				boolean b = !mod.isEmpty();
				for (JButton bt : sortingButtons) bt.setVisible(b && sms.equals(bt.getName()));
				menuButton.setVisible(b);
				elementsRoller.restartTimer();
			}
		});
		
		return sortingPanel;
	}
	
	private JButton createMenuButton() {
		JPopupMenu popupMenu = createpopup();
		JButton menuButton = Utils.createMenuButton(e -> popupMenu.show((JButton)e.getSource(), 0, 0));

		menuButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		menuButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		return menuButton;
	}
	private JPopupMenu createpopup() {
		JPopupMenu popupMenu = new JPopupMenu(); 

		BiConsumer<String, ActionListener> miCreate = (text, actionListener) -> {
			JMenuItem mi = new JMenuItem(text);
			mi.setDoubleBuffered(false);
			if(actionListener == null)
				mi.addActionListener(e -> showHidePopup("Not(Working yet)", 1000));
			else
				mi.addActionListener(actionListener);

			popupMenu.add(mi);
		};

		miCreate.accept("Select", null);
		miCreate.accept("Unselect All", null);
		miCreate.accept("Delete Selected", null);

		return popupMenu;
	}
	public ViewElementType getCurrentElementType(){
		return elementsRoller.getCurrentElementType();
	}

	public int getArrayIndexOfSelectedManga() {
		return selectedIndex;
	}

	/**
	 * this is used to change type of ViewElements
	 * @param elementtype
	 */
	public void changeElementType(ViewElementType elementtype) {
		if(getCurrentElementType() == elementtype)
			return;
		
		elementsRoller.setCurrentElementType(elementtype);
	}

	/**
	 * LocationOnScreen of element is relative to ElementsView, but we need LocationOnScreen of element relative to centraPanel (since we are scrolling on centralPanel)
	 * this can be obtain by element.LocationOnScreen - sortingPanel.size, this methods just to do that
	 * @param element
	 * @return
	 */
	Rectangle calculateRect(ViewElement element) {
		Point p = new Point(element.getLocationOnScreen());
		p.y -= sortingPanel.getHeight();
		return new Rectangle(p, element.getSize());
	}
	public void updateCurrentMangaViewElement() {
		elementsRoller.updateCurrentMangaViewElement();
	}
	public void focusCurrentManga(){
		elementsRoller.focusCurrentManga();
	}
}
