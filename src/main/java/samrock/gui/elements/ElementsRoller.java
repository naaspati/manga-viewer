package samrock.gui.elements;

import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;

import java.awt.EventQueue;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import samrock.api.Change;
import samrock.api.Changer;
import samrock.api.ViewElementType;
import samrock.manga.MinimalManga;

class ElementsRoller extends JScrollPane implements MouseListener, KeyListener, FocusListener {
    private static final long serialVersionUID = 6655338092761018139L;

    private MinimalManga selectedManga; 

    private final Changer changer;

    private final ElementsPanel content; 
    final JScrollBar vBar;
    final JViewport viewport;
    private final ElementsView elementsView;

    public ElementsRoller(Changer changer, ElementsView elementsView) {
        super(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.changer = changer;
        this.elementsView = elementsView;

        content = new ElementsPanel(); 
        setViewportView(content);

        vBar = getVerticalScrollBar();
        viewport = getViewport();

        ViewElement.initConstants(this, this, this);
    }
    private void doMangaClick(ComponentEvent e) {
        doMangaClick(get(e));
    }
    private void doMangaClick(ViewElement e) {
        selectedManga = e.getManga();
        changer.changeTo(Change.VIEW_ELEMENT_CLICKED);
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() > 1) doMangaClick(e);
        else e.getComponent().requestFocus();
    }

    private ViewElement element;
    
    ViewElement get(ComponentEvent e) {
        return (ViewElement)e.getSource();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if(content.isEmpty())
            return;

        element = get(e);
        int key = e.getKeyCode();


        switch (key) {
            case VK_RIGHT: right();  break;
            case VK_LEFT: left();  break;

            case VK_UP: up();  break;
            case VK_DOWN: down();  break;

            case VK_ENTER: doMangaClick(e);  break;
            case VK_DELETE: content.deleteManga(element);  break;

            default:
                break;
        }
    }

    private void down() {
        if(!content.isElementTypeThumb()){
            content.ensureNext(element);
            element.transferFocus();
            return;
        }

        if(element == content.last()) 
            content.toFirst();
        else
            content.gotoIndex(element.getModIndex()+content.elementPerRow());
    }
    
    private void up() {
        if(!content.isElementTypeThumb()){
            content.ensurePrevious(element);
            element.transferFocusBackward();
            return;
        }
        
        if(element == content.first()) 
            content.toLast();
        else
        	content.gotoIndex(element.getModIndex() - content.elementPerRow());
    }
    private void left() {
        if(content.isElementTypeThumb())
            content.gotoIndex(element.getModIndex()-1);
    }
    private void right() {
        if(content.isElementTypeThumb())
            content.gotoIndex(element.getModIndex()+1);
    }
    @Override
    public void focusLost(FocusEvent e) {
        get(e).setInFocus(false);
    }

    @Override
    public void focusGained(FocusEvent e) {
        ViewElement element = get(e);
        int index = element.getIndex();

        if(element.getVisibleRect().height < element.getHeight()){
            if(index == 0)
                vBar.setValue(0);
            else if(index == content.length() - 1)
                vBar.setValue(vBar.getMaximum());
            else
                scrollTo(element);
        }
        element.setInFocus(true);
    }

    private void scrollTo(ViewElement element) {
        viewport.scrollRectToVisible(elementsView.calculateRect(element));
    }
    public void focusCurrentManga() {
    	ViewElement v = content.getSelectedMangaView();
    	EventQueue.invokeLater(() -> {
    		v.requestFocus();
    		//TODO if v not taking focus uncomment this.. EventQueue.invokeLater(() -> scrollTo(v));
        });
    }

    public ViewElementType getCurrentElementType(){
        return content.getCurrentElementType();
    }
    public void setCurrentElementType(ViewElementType elementtype) {
        if(getCurrentElementType() == elementtype)
            return;

        content.setCurrentElementType(elementtype);
        vBar.setUnitIncrement(content.first().getHeight()/4);
        EventQueue.invokeLater(() -> vBar.setValue(0));
    }
    public void updateCurrentMangaViewElement() {
    	content.updateCurrentMangaViewElement();
    }
    @Deprecated
    public void sleep() {
        content.removeAll();
        //TODO
    }
    public MinimalManga getSelectedManga() {
        return selectedManga;
    }
    /** FIXME to be DELETED
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
     */


}
