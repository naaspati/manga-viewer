package samrock.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.IntConsumer;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import samrock.manga.MangaManeger;
import samrock.utils.RH;
import samrock.utils.Utils;
import samrock.utils.ViewElementType;
import samrock.utils.Views;
import samrock.viewer.MangaViewer;

//this will be the GUI the this project
public final class SamRock extends JFrame {

	private static final long serialVersionUID = -7783411284501874638L;

	private Views currentView;

	private final MangaManeger mangaManeger ;
	private final ElementsView elementsView;
	private final WestControl westControl;
	private final Timer cleanerTimer;
	private final JPanel viewContainer; 

	private final IntConsumer unifiedWatcher;

	public SamRock() {
		super("Samrock - "+Main.VERSION);
		setLayout(new BorderLayout(1, 1));

		mangaManeger = MangaManeger.getInstance();
		unifiedWatcher = getUnifiedWatcher();
		currentView = Views.VIEWELEMENTS_VIEW;
		viewContainer = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));

		Utils.setPopupRelativeTo(this);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setIconImage(RH.getImageIcon("app.icon").getImage());
		setUndecorated("1".equals(RH.getString("app.setundecorated")));

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) { closeApp(); }
		});

		//first initialize elementsView, than westControl otherwise elementsView wont respond to first click performed by westControl, on startup of app 
		elementsView = ElementsView.getInstance(unifiedWatcher);
		westControl = WestControl.getInstance(unifiedWatcher);

		viewContainer.add(elementsView);

		add(westControl, BorderLayout.WEST);
		JPanel p = Utils.createJPanel(new GridLayout(1, 1));
		p.add(viewContainer);
		add(p);

		cleanerTimer  = getTimer();

		cleanerTimer.setRepeats(false);		
	}
	private boolean sleeping = false;

	private Timer getTimer() {
		return new Timer(60*1000, e -> {
			sleeping = true;
			viewContainer.removeAll();

			chaptersListView = null;
			chaptersEditorView = null;
			dataView = null;

			elementsView.hibernate();
			westControl.hibernate();
		});
	}

	private int lastRequest  = -1;
	private IntConsumer getUnifiedWatcher() {
		return requestCode -> {
			if(lastRequest == requestCode)
				return;

			lastRequest = requestCode;
			

			int id = Utils.showPopup("Wait");
			switch(requestCode){
			//ElementsView
			case ElementsView.VIEW_ELEMENT_CLICKED:
				mangaManeger.loadManga(elementsView.getArrayIndexOfSelectedManga());
				if(ViewElement.getCurrentElementType() == ViewElementType.RECENT_THUMB || ViewElement.getCurrentElementType() == ViewElementType.RECENT_LIST)
					MangaViewer.openMangaViewer(unifiedWatcher, MangaViewer.OPEN_MOST_RECENT_CHAPTER); 
				else
					changeView(mangaManeger.getCurrentManga().getStartupView());
				break;

				//WestControl.
			case WestControl.BACK_TO_DOCK : 
				elementsView.updateCurrentMangaViewElement();
				if(currentView == Views.CHAPTERS_EDIT_VIEW){
					chaptersEditorView.cancel();
					changeView(Views.CHAPTERS_LIST_VIEW);
				}
				else{
					changeView(Views.VIEWELEMENTS_VIEW);
					elementsView.focusCurrentManga();
				}
				break;
			case WestControl.CHANGEVIEW_DATA_VIEW :
				changeView(Views.DATA_VIEW);
				break;
			case WestControl.CHANGEVIEW_CHAPTERS_LIST_VIEW :
				changeView(Views.CHAPTERS_LIST_VIEW);
				break;
			case WestControl.CHANGETYPE_LIST :
				mangaManeger.loadAllMinimalListMangas(); 
				elementsView.changeElementType(ViewElementType.LIST);
				break;
			case WestControl.CHANGETYPE_THUMB :
				elementsView.changeElementType(ViewElementType.THUMB);
				break;
			case WestControl.CHANGETYPE_RECENT :
				mangaManeger.loadAllMinimalChapterSavePoints();
				ViewElementType t = elementsView.getCurrentElementType();
				elementsView.changeElementType(t == ViewElementType.LIST || t == ViewElementType.RECENT_LIST ? ViewElementType.RECENT_LIST : ViewElementType.RECENT_THUMB);
				break;
			case WestControl.CHANGETYPE_NORMAL :
				t = elementsView.getCurrentElementType();
				elementsView.changeElementType(t == ViewElementType.LIST || t == ViewElementType.RECENT_LIST ? ViewElementType.LIST : ViewElementType.THUMB);
				break;
			case WestControl.OPEN_MOST_RECENT_CHAPTER :
				if(currentView == Views.VIEWELEMENTS_VIEW)
					mangaManeger.loadMostRecentManga();
				MangaViewer.openMangaViewer(unifiedWatcher, MangaViewer.OPEN_MOST_RECENT_CHAPTER);
				break;
			case WestControl.OPEN_MOST_RECENT_MANGA :
				mangaManeger.loadMostRecentManga();
				changeView(Views.DATA_VIEW);
				break;
			case WestControl.ICONFY_APP :
				setState(JFrame.ICONIFIED);
				break;
			case WestControl.CLOSE_APP :
				closeApp();
				break;

				//ChaptersListView
			case ChaptersListView.START_CHAPTER_EDITOR:
				changeView(Views.CHAPTERS_EDIT_VIEW);
				break;
			case ChaptersListView.START_MANGA_VIEWER:
				MangaViewer.openMangaViewer(unifiedWatcher, chaptersListView.getSelectChapterIndex());
				break;

			case MangaViewer.STARTED:
				setEnabled(false);
				setVisible(false);
				cleanerTimer.start();
				break;
			case MangaViewer.CLOSED:
				cleanerTimer.stop();

				if(sleeping){
					elementsView.wakeUp();
					viewContainer.add(elementsView);
					westControl.wakeUp();
					sleeping = false;
					
				}
				else
					elementsView.updateCurrentMangaViewElement();
				
				changeView(currentView);
				setPopupRelative();
				setEnabled(true);
				setVisible(true);
				toFront();
				System.gc();
				break;
			default: 
				Utils.logError("unifiedWatcher failed to recognize resoponse code : "+requestCode,SamRock.class,208/*{LINE_NUMBER}*/, null);
			}

			Utils.hidePopup(id, 500);
			lastRequest = -1;

		};
	}

	private void setPopupRelative() { Utils.setPopupRelativeTo(this); }

	private void closeApp(){
		if(currentView == Views.CHAPTERS_EDIT_VIEW)
			chaptersEditorView.cancel();

		setEnabled(false);
		Utils.exit();
		dispose();
	}

	ChaptersEditorView chaptersEditorView;
	ChaptersListView chaptersListView;
	DataView dataView;

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
			if(chaptersEditorView == null){
				chaptersEditorView = new ChaptersEditorView();
				viewContainer.add(chaptersEditorView);
			}
			else{
				chaptersEditorView.changeManga();
				chaptersEditorView.setVisible(true);
			}
			break;
		case CHAPTERS_LIST_VIEW:
			if(chaptersListView == null){
				chaptersListView = new ChaptersListView(unifiedWatcher);
				viewContainer.add(chaptersListView);
			}
			else{
				chaptersListView.changeManga();
				chaptersListView.setVisible(true);
			}
			break;
		case DATA_VIEW:
			if(dataView == null){
				dataView = new DataView();
				viewContainer.add(dataView);
				dataView.revalidate();
				dataView.repaint();
			}
			else{
				dataView.changeManga();
				dataView.setVisible(true);
			}
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

