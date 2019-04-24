package samrock.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.codejargon.feather.Provides;
import org.slf4j.Logger;

import sam.collection.ArraysUtils;
import sam.di.FeatherInjector;
import sam.di.Injector;
import sam.myutils.Checker;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import sam.reference.WeakAndLazy;
import sam.swing.SwingPopupShop;
import sam.swing.SwingPopupShop.SwingPopupWrapper;
import samrock.Utils;
import samrock.api.AppConfig;
import samrock.api.Change;
import samrock.api.Changer;
import samrock.api.OnSleep;
import samrock.api.OnSleepListener;
import samrock.api.ViewElementType;
import samrock.api.Views;
import samrock.gui.chapter.ChaptersEditorView;
import samrock.gui.chapter.ChaptersListView;
import samrock.gui.elements.ElementsView;
import samrock.gui.front.DataView;
import samrock.gui.front.WestControl;
import samrock.manga.maneger.api.MangaManeger;
import samrock.viewer.MangaViewer;

public final class SamRock implements Changer, OnSleep {
	private static final EnsureSingleton singleton = new EnsureSingleton();

	private final static Logger logger = Utils.getLogger(SamRock.class);

	private Views currentView = Views.VIEWELEMENTS_VIEW;

	private ElementsView elementsView;
	private WestControl westControl;
	private ChaptersEditorView chaptersEditorView;;
	private ChaptersListView chaptersListView;
	private DataView dataView;
	private MangaManeger mangaManeger;
	
	private Provider<MangaViewer> mangaViewerProvider;
	private MangaViewer mangaViewer;

	private JPanel viewContainer; 
	private JFrame frame;
	
	private OnSleepListener[] onSleepListeners = {};

	public void start (String version) throws Exception {
		singleton.init();
		
		Injector injector = Injector.init(new FeatherInjector(this));
		Utils.load(injector.instance(AppConfig.class));

		mangaManeger = injector.instance(MangaManeger.class);

		frame = new JFrame("Samrock - "+(version == null ? "" : version));
		frame.setLayout(new BorderLayout(1, 1));

		viewContainer = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));

		SwingPopupShop.setPopupsRelativeTo(frame);
		AppConfig setting = injector.instance(AppConfig.class);
		
		frame.addWindowListener(closeAppListener());
		
		elementsView = injector.instance(ElementsView.class);
		viewContainer.add(elementsView);

		westControl = injector.instance(WestControl.class);

		frame.add(westControl, BorderLayout.WEST);
		JPanel p = Utils.createJPanel(new GridLayout(1, 1));
		p.add(viewContainer);
		frame.add(p);
		
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setIconImage(setting.getImageIcon("app.icon").getImage());
		frame.setUndecorated("1".equals(setting.getString("app.setundecorated")));

		EventQueue.invokeLater(() -> frame.setVisible(true));
	}

	private DataView dataView() {
		return Junk.notYetImplemented(); /* FIXME */ 
	}

	private ChaptersEditorView chaptersEditorView() {
		return Junk.notYetImplemented(); /* FIXME */ 
	}

	private MangaViewer mangaViewer() {
		return Junk.notYetImplemented(); /* FIXME */ 
	}

	private ChaptersListView chaptersListView() {
		return Junk.notYetImplemented(); /* FIXME */ 
	}

	@Provides
	public Changer changer() {
		return this;
	}

	private Change lastRequest;
	private boolean sleeping;
	@Override
	public void changeTo(Change requestCode) {
		if(lastRequest == requestCode)
			return;

		lastRequest = requestCode;

		SwingPopupWrapper id = SwingPopupShop.showPopup("Wait");
		switch(requestCode){
			//ElementsView
			case VIEW_ELEMENT_CLICKED:
				//FIXME mangaManeger.loadManga(elementsView.getArrayIndexOfSelectedManga());
				if(getCurrentElementType() == ViewElementType.RECENT_THUMB || getCurrentElementType() == ViewElementType.RECENT_LIST) {
					// TODO mangaViewer().start(changer, MangaViewer.OPEN_MOST_RECENT_CHAPTER);
				} else
					changeView(mangaManeger.getSelectedManga().getStartupView());
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
				//FIXME if implemented mangaManeger.loadAllMinimalListMangas(); 
				elementsView.changeElementType(ViewElementType.LIST);
				break;
			case CHANGETYPE_THUMB :
				elementsView.changeElementType(ViewElementType.THUMB);
				break;
			case CHANGETYPE_RECENT :
				//TODO lazy loading is used now -- mangaManeger.loadAllMinimalChapterSavePoints();
				ViewElementType t = getCurrentElementType();
				elementsView.changeElementType(t == ViewElementType.LIST || t == ViewElementType.RECENT_LIST ? ViewElementType.RECENT_LIST : ViewElementType.RECENT_THUMB);
				break;
			case CHANGETYPE_NORMAL :
				t = getCurrentElementType();
				elementsView.changeElementType(t == ViewElementType.LIST || t == ViewElementType.RECENT_LIST ? ViewElementType.LIST : ViewElementType.THUMB);
				break;
			case OPEN_MOST_RECENT_CHAPTER :
				/* FIXME
				 * if(currentView == Views.VIEWELEMENTS_VIEW)
					mangaManeger.loadMostRecentManga();
					mangaViewer().start(changer, MangaViewer.OPEN_MOST_RECENT_CHAPTER);
				 */
				break;
			case OPEN_MOST_RECENT_MANGA :
				// FIXME mangaManeger.loadMostRecentManga();
				changeView(Views.DATA_VIEW);
				break;
			case ICONFY_APP :
				frame.setState(JFrame.ICONIFIED);
				break;
			case CLOSE_APP :
				closeApp();
				break;

				//ChaptersListView
			case START_CHAPTER_EDITOR:
				changeView(Views.CHAPTERS_EDIT_VIEW);
				break;
			case START_MANGA_VIEWER:
				//FIXME mangaViewer().start(changer, chaptersListView().getSelectChapterIndex());
				break;
			case STARTED:
				frame.setEnabled(false);
				frame.setVisible(false);
				//FIXME addClearTask();                     
				break;
			case CLOSED:
				//FIXME cancelClearTask();
				//FIXME mangaViewer.clear();

				if(sleeping){
					elementsView.wakeUp();
					viewContainer.add(elementsView);
					westControl.wakeUp();
					sleeping = false;

				}
				else
					elementsView.updateCurrentMangaViewElement();

				changeView(currentView);
				SwingPopupShop.setPopupsRelativeTo(frame);
				frame.setEnabled(true);
				frame.setVisible(true);
				frame.toFront();
				break;
			default: 
				logger.warn("unifiedChanger failed to recognize resoponse code : {}", requestCode);
		}
		SwingPopupShop.hidePopup(id, 500);
		lastRequest = null;

	}
	private ViewElementType getCurrentElementType() {
		return elementsView.getCurrentElementType();
	}
	private WindowAdapter closeAppListener() {
		return new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) { closeApp(); }
		};
	}
	private void closeApp(){
		if(currentView == Views.CHAPTERS_EDIT_VIEW)
			chaptersEditorView().cancel();

		frame.setEnabled(false);
		Utils.exit();
		frame.dispose();
	}

	private final WeakAndLazy<Component> nothingLabel = new WeakAndLazy<>(() -> Utils.getNothingfoundlabel("Nothing", Injector.getInstance().instance(AppConfig.class)));

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
				viewContainer.add(nothingLabel.get());
				break;
			case VIEWELEMENTS_VIEW:
				elementsView.setVisible(true);
				break;
		}

		viewContainer.revalidate();
		viewContainer.repaint();		
	}

	@Override
	public void addListener(OnSleepListener listener) {
		Objects.requireNonNull(listener);
		
		if(!Checker.anyMatch(t -> t == listener, onSleepListeners)) {
			OnSleepListener[] c = new OnSleepListener[onSleepListeners.length + 1];
			System.arraycopy(onSleepListeners, 0, c, 0, onSleepListeners.length);
			c[c.length - 1] = listener;
			onSleepListeners = c;
		}
	}
}

