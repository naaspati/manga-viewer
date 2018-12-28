package samrock.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import sam.logging.MyLoggerFactory;
import sam.swing.SwingPopupShop;
import sam.swing.SwingPopupShop.SwingPopupWrapper;
import samrock.gui.chapter.ChaptersEditorView;
import samrock.gui.chapter.ChaptersListView;
import samrock.gui.elements.ElementsView;
import samrock.gui.front.DataView;
import samrock.gui.front.WestControl;
import samrock.manga.maneger.MangaManeger;
import samrock.utils.PrintFinalize;
import samrock.utils.RH;
import samrock.utils.Utils;
import samrock.utils.ViewElementType;
import samrock.utils.Views;
import samrock.viewer.MangaViewer;

//this will be the GUI the this project
public final class SamRock extends JFrame {

	private static final long serialVersionUID = -7783411284501874638L;
	private static Logger logger = MyLoggerFactory.logger(SamRock.class);

	private static JFrame main;
	public static JFrame getMain() {
		return main;
	}

	private Views currentView = Views.VIEWELEMENTS_VIEW;

	private final ElementsView elementsView;
	private final WestControl westControl;
	private final SoftAndLazyWrapper<ChaptersEditorView> chaptersEditorView;;
	private final SoftAndLazyWrapper<ChaptersListView> chaptersListView;
	private final SoftAndLazyWrapper<DataView> dataView;
	private final SoftAndLazyWrapper<MangaViewer> mangaViewer = new SoftAndLazyWrapper<>(MangaViewer::new);

	private final SoftAndLazyWrapper[] wrappers;

	private final JPanel viewContainer; 

	private final Changer changer;
	private TimerTask clearTask;
	private Timer timer;

	public SamRock(String version) throws Exception {
		super("Samrock - "+(version == null ? "" : version));
		setLayout(new BorderLayout(1, 1));
		SamRock.main = this;

		MangaManeger.init();
		changer = getChanger();
		viewContainer = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));

		SwingPopupShop.setPopupsRelativeTo(this);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setIconImage(RH.getImageIcon("app.icon").getImage());
		setUndecorated("1".equals(RH.getString("app.setundecorated")));

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) { closeApp(); }
		});

		//first initialize elementsView, than westControl otherwise elementsView wont respond to first click performed by westControl, on startup of app 
		elementsView = new ElementsView(changer);
		westControl = new WestControl(changer);
		chaptersEditorView = wl(() -> {
			ChaptersEditorView c = new ChaptersEditorView();
			viewContainer.add(c);
			return c;
		}) ;
		chaptersListView = wl(() -> {
			ChaptersListView c = new ChaptersListView(changer);
			viewContainer.add(c);
			return c;
		});
		dataView = wl(() -> {
			DataView dataView = new DataView();
			viewContainer.add(dataView);
			dataView().revalidate();
			dataView().repaint();
			return dataView;
		});

		wrappers = new SoftAndLazyWrapper[]{
				chaptersEditorView,
				chaptersListView,
				dataView,
				mangaViewer};

		viewContainer.add(elementsView);

		add(westControl, BorderLayout.WEST);
		JPanel p = Utils.createJPanel(new GridLayout(1, 1));
		p.add(viewContainer);
		add(p);
	}
	private <E extends PrintFinalize> SoftAndLazyWrapper<E> wl(Supplier<E> s) {
		return new SoftAndLazyWrapper<>(s);
	}
	private DataView  dataView() { return dataView.get(); }
	private ChaptersEditorView chaptersEditorView() { return chaptersEditorView.get(); }
	private MangaViewer mangaViewer() { return mangaViewer.get(); }
	private ChaptersListView chaptersListView() { return chaptersListView.get(); }

	@SuppressWarnings("rawtypes")
	private void clearSolidRefrences() {
		for (SoftAndLazyWrapper w : wrappers) w.clear();
	}

	private void cancelClearTask() {
		if(clearTask != null)
			clearTask.cancel();
		clearTask = null;
	}
	public void addClearTask() {
		if(timer == null)  
			timer = new Timer(true);

		cancelClearTask();   

		clearTask = new TimerTask() {
			@Override
			public void run() {
				viewContainer.removeAll();
				clearSolidRefrences();
			}
		};
		timer.schedule(clearTask, 20000);
	} 

	private Change lastRequest;
	private boolean sleeping;

	private Changer getChanger() {
		return requestCode -> {
			if(lastRequest == requestCode)
				return;

			lastRequest = requestCode;

			SwingPopupWrapper id = SwingPopupShop.showPopup("Wait");
			switch(requestCode){
			//ElementsView
			case VIEW_ELEMENT_CLICKED:
				MangaManeger.loadManga(elementsView.getArrayIndexOfSelectedManga());
				if(getCurrentElementType() == ViewElementType.RECENT_THUMB || getCurrentElementType() == ViewElementType.RECENT_LIST)
					mangaViewer().start(changer, MangaViewer.OPEN_MOST_RECENT_CHAPTER); 
				else
					changeView(MangaManeger.getCurrentManga().getStartupView());
				break;

				//
			case BACK_TO_DOCK : 
				elementsView.updateCurrentMangaViewElement();
				if(currentView == Views.CHAPTERS_EDIT_VIEW){
					chaptersEditorView().cancel();
					changeView(Views.CHAPTERS_LIST_VIEW);
				}
				else{
					changeView(Views.VIEWELEMENTS_VIEW);
					elementsView.focusCurrentManga();
				}
				break;
			case CHANGEVIEW_DATA_VIEW :
				changeView(Views.DATA_VIEW);
				break;
			case CHANGEVIEW_CHAPTERS_LIST_VIEW :
				changeView(Views.CHAPTERS_LIST_VIEW);
				break;
			case CHANGETYPE_LIST :
				//FIXME if implemented MangaManeger.loadAllMinimalListMangas(); 
				elementsView.changeElementType(ViewElementType.LIST);
				break;
			case CHANGETYPE_THUMB :
				elementsView.changeElementType(ViewElementType.THUMB);
				break;
			case CHANGETYPE_RECENT :
				//TODO lazy loading is used now -- MangaManeger.loadAllMinimalChapterSavePoints();
				ViewElementType t = getCurrentElementType();
				elementsView.changeElementType(t == ViewElementType.LIST || t == ViewElementType.RECENT_LIST ? ViewElementType.RECENT_LIST : ViewElementType.RECENT_THUMB);
				break;
			case CHANGETYPE_NORMAL :
				t = getCurrentElementType();
				elementsView.changeElementType(t == ViewElementType.LIST || t == ViewElementType.RECENT_LIST ? ViewElementType.LIST : ViewElementType.THUMB);
				break;
			case OPEN_MOST_RECENT_CHAPTER :
				if(currentView == Views.VIEWELEMENTS_VIEW)
					MangaManeger.loadMostRecentManga();
				mangaViewer().start(changer, MangaViewer.OPEN_MOST_RECENT_CHAPTER);
				break;
			case OPEN_MOST_RECENT_MANGA :
				MangaManeger.loadMostRecentManga();
				changeView(Views.DATA_VIEW);
				break;
			case ICONFY_APP :
				setState(JFrame.ICONIFIED);
				break;
			case CLOSE_APP :
				closeApp();
				break;

				//ChaptersListView
			case START_CHAPTER_EDITOR:
				changeView(Views.CHAPTERS_EDIT_VIEW);
				break;
			case START_MANGA_VIEWER:
				mangaViewer().start(changer, chaptersListView().getSelectChapterIndex());
				break;
			case STARTED:
				setEnabled(false);
				setVisible(false);
				addClearTask();                     
				break;
			case CLOSED:
				cancelClearTask();
				mangaViewer.clear();

				if(sleeping){
					elementsView.wakeUp();
					viewContainer.add(elementsView);
					westControl.wakeUp();
					sleeping = false;

				}
				else
					elementsView.updateCurrentMangaViewElement();

				changeView(currentView);
				SwingPopupShop.setPopupsRelativeTo(this);
				setEnabled(true);
				setVisible(true);
				toFront();
				break;
			default: 
				logger.warning(() -> "unifiedChanger failed to recognize resoponse code : "+requestCode);
			}
			SwingPopupShop.hidePopup(id, 500);
			lastRequest = null;

		};
	}
	private ViewElementType getCurrentElementType() {
		return elementsView.getCurrentElementType();
	}
	private void closeApp(){
		if(currentView == Views.CHAPTERS_EDIT_VIEW)
			chaptersEditorView().cancel();

		setEnabled(false);
		Utils.exit();
		dispose();
	}
	//change view
	void changeView(Views view){
		currentView = view;

		for (Component c : viewContainer.getComponents()) {
			c.setVisible(false);
			if(c instanceof JLabel)
				viewContainer.remove(c);
		}

		westControl.viewChanged(view);

		switch (view) {
		case CHAPTERS_EDIT_VIEW:
			chaptersEditorView().changeManga();
			chaptersEditorView().setVisible(true);
			break;
		case CHAPTERS_LIST_VIEW:
			chaptersListView().reset();
			chaptersListView().setVisible(true);
			break;
		case DATA_VIEW:
			dataView().changeManga();
			dataView().setVisible(true);
			break;
		case NOTHING_FOUND_VIEW:
			viewContainer.add(Utils.getNothingfoundlabel("Nothing"));
			break;
		case VIEWELEMENTS_VIEW:
			elementsView.setVisible(true);
			break;
		}

		viewContainer.revalidate();
		viewContainer.repaint();		
	}
}

