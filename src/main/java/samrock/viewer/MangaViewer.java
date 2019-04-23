package samrock.viewer;
import static java.awt.event.KeyEvent.VK_1;
import static java.awt.event.KeyEvent.VK_2;
import static java.awt.event.KeyEvent.VK_3;
import static java.awt.event.KeyEvent.VK_4;
import static java.awt.event.KeyEvent.VK_5;
import static java.awt.event.KeyEvent.VK_6;
import static java.awt.event.KeyEvent.VK_ADD;
import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_END;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_F2;
import static java.awt.event.KeyEvent.VK_G;
import static java.awt.event.KeyEvent.VK_H;
import static java.awt.event.KeyEvent.VK_HOME;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_M;
import static java.awt.event.KeyEvent.VK_NUMPAD1;
import static java.awt.event.KeyEvent.VK_NUMPAD2;
import static java.awt.event.KeyEvent.VK_NUMPAD3;
import static java.awt.event.KeyEvent.VK_NUMPAD4;
import static java.awt.event.KeyEvent.VK_NUMPAD5;
import static java.awt.event.KeyEvent.VK_NUMPAD6;
import static java.awt.event.KeyEvent.VK_PAGE_DOWN;
import static java.awt.event.KeyEvent.VK_PAGE_UP;
import static java.awt.event.KeyEvent.VK_R;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_S;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_SUBTRACT;
import static java.awt.event.KeyEvent.VK_UP;
import static java.awt.event.KeyEvent.VK_Z;
import static samrock.viewer.Actions.CHANGE_SCROLL;
import static samrock.viewer.Actions.CHANGE_ZOOM;
import static samrock.viewer.Actions.GOTO;
import static samrock.viewer.Actions.GOTO_END;
import static samrock.viewer.Actions.GOTO_START;
import static samrock.viewer.Actions.OPEN_HELP_FILE;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.slf4j.Logger;

import sam.nopkg.Junk;
import sam.reference.ReferenceUtils;
import sam.swing.SwingPopupShop;
import samrock.Utils;
import samrock.api.AppSetting;
import samrock.gui.Change;
import samrock.gui.Changer;
import samrock.manga.Chapter;
import samrock.manga.Manga;
import samrock.manga.Order;
import samrock.manga.maneger.api.MangaManeger;
import samrock.manga.recents.ChapterSavePoint;
public class MangaViewer extends JFrame implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
	private static final long serialVersionUID = 9222652000321437542L;
	private static Logger logger = Utils.getLogger(MangaViewer.class);

	public static final int OPEN_MOST_RECENT_CHAPTER = 0x802*-1;

	//TODO private static MangaViewer instance;
	/**
	 * @param mangaViewerWatcher
	 * @param chapterIndex use {@link #OPEN_MOST_RECENT_CHAPTER}, if you wish to open most_recent_chapter in current manga, otherwise a chapter index, 
	 * if index < 0 || index > chpatersCount - 1 , than most_recent_chapter will be opened  
	 */
	/*
	 * public static void openMangaViewer(Changer mangaViewerWatcher, int chapterIndex) {
        if (instance != null)
            return;
        instance = new MangaViewer(mangaViewerWatcher, chapterIndex);
    }
	 */
	
	private static Image _icon;

	private final MangaManeger mangaManeger;
	private List<Chapter> chapters;
	private Manga manga;
	private ListIterator<Chapter> iter;
	/**
	 * chapterStrip
	 */
	private final MangaChapterStrip chapterStrip;
	private static final Cursor simpleCursor = Cursor.getDefaultCursor();
	private static final Cursor invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(), "Gayab");;

	private final IdentityHashMap<Chapter, SoftReference<ChapterSavePoint>> savePoints = new IdentityHashMap<>(); 

	private long mouseMovedTime = 0;
	private final Timer cursorHider;

	private final Changer changer;

	/**
	 * @param mangaViewerWatcher 
	 * @param chapterIndex index of chapter with which app will start
	 */
	@Inject
	public MangaViewer(MangaManeger m, Changer changer, Provider<AppSetting> setting){
		super("Manga Viewer");
		this.mangaManeger = m;
		this.changer = changer;
		chapterStrip = new MangaChapterStrip();

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setUndecorated(true);
		getContentPane().add(chapterStrip);
		getContentPane().setBackground(Color.black);
		
		if(_icon == null)
			_icon = setting.get().getImageIcon("app.icon").getImage();
		
		setIconImage(_icon);

		cursorHider = new Timer(2500, e -> {
			if(mouseMovedTime != -1 && System.currentTimeMillis() - mouseMovedTime > 2000){
				setCursor(invisibleCursor);
				mouseMovedTime = -1;
			}
		});

		addMouseMotionListener(this);
		addKeyListener(this);
		addMouseWheelListener(this);
	}
	
	public void start(int chapterIndex) {
		manga = mangaManeger.getCurrentManga();
		chapters = manga.getChapters();

		if(chapterIndex < 0 || chapterIndex >= chapters.size())
			chapterIndex = -1;

		if(chapterIndex == -1){
			ChapterSavePoint savePoint = manga.getSavePoint();

			if(savePoint == null || savePoint.chapter == null) {
				savePoint = new ChapterSavePoint(manga, chapters.get(0), System.currentTimeMillis());
				chapterIndex = 0;
			} else {
				chapterIndex = chapters.indexOf(savePoint.chapter);
			}
		}

		chapterIndex = chapterIndex < 0 || chapterIndex >= chapters.size() ? 0 : chapterIndex;
		iter = chapters.listIterator(chapterIndex);

		changer.changeTo(Change.STARTED);

		SwingPopupShop.setPopupsRelativeTo(this);
		changeChapter(iter.next());
		setVisible(true);
		toFront();

		cursorHider.start();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if(e.getWheelRotation() > 0)
			chapterStrip.scrollDown();
		else if(e.getWheelRotation() < 0)
			chapterStrip.scrollUp();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode(); 
		if(code == VK_DOWN) {
			chapterStrip.scrollDown();
			return;
		}
		if(code == VK_UP) {
			chapterStrip.scrollUp();
			return;
		}

		switch (code) {
		case VK_PAGE_UP: chapterStrip.scrollUp(); break;
		case VK_PAGE_DOWN: chapterStrip.scrollDown(); break;
		case VK_LEFT: chapterStrip.scrollLeft(); break;
		case VK_RIGHT: chapterStrip.scrollRight(); break;
		case VK_ADD: chapterStrip.zoomIn(); break ;
		case VK_SUBTRACT: chapterStrip.zoomOut(); break;
		case VK_Z: chapterStrip.doThis(CHANGE_ZOOM); break;
		case VK_1: chapterStrip.zoom(1.0d); break;
		case VK_NUMPAD1: chapterStrip.zoom(1.0d); break;
		case VK_2: chapterStrip.zoom(1.5d); break;
		case VK_NUMPAD2: chapterStrip.zoom(1.5d); break;
		case VK_3: chapterStrip.zoom(1.75d); break;
		case VK_NUMPAD3: chapterStrip.zoom(1.75d); break;
		case VK_4: chapterStrip.zoom(2.0d); break;
		case VK_NUMPAD4: chapterStrip.zoom(2.0d); break;
		case VK_5: chapterStrip.zoom(2.25d); break;
		case VK_NUMPAD5: chapterStrip.zoom(2.25d); break;
		case VK_6: chapterStrip.zoom(2.5d); break;
		case VK_NUMPAD6: chapterStrip.zoom(2.5d); break;
		case VK_SPACE://next chapter
			if(e.isControlDown() ||  chapterStrip.scale == 1.0)
				nextChapter();
			else
				chapterStrip.zoom(1.0);
			break;
		case VK_BACK_SPACE:
			previousChapter();
			break;
		case VK_HOME:
			chapterStrip.doThis(GOTO_START);
			break;
		case VK_END:
			chapterStrip.doThis(GOTO_END);
			break;
		case VK_F2:
			JOptionPane.showMessageDialog(null, "RENAMING NOT SUPPORTED");
			/* FIXME
			 * String oldName = chapter.getTitle();
			String newName = JOptionPane.showInputDialog("<html>Rename?<br>any invalid characters for naming <br>a file will removed</html>", oldName);
			try {
				boolean status = chapters.setTitle(newName);
				if(status){
					chapterStrip.setChapterName(chapter.getTitle());
					chapterStrip.repaint();
					Utils.showHidePopup("chapter renamed", 1500);
				}                        
			} catch (BadChapterNameException e1) {
				logger.log(Level.SEVERE, "failed to rename chapter", e1);
			}
			 */

			break;
		case VK_ESCAPE: exit(); break;
		case VK_G:
			 if(e.isControlDown())
				 chapterStrip.doThis(GOTO);
			 else {
				 System.gc();
				 SwingPopupShop.showHidePopup("System.gc()", 2000);
			 }
			break;
		case VK_M: setState(JFrame.ICONIFIED); break;
		case VK_H: chapterStrip.doThis(OPEN_HELP_FILE); break;
		case VK_R:
			if(chapter != null ) { 
				chapter.setRead(!chapter.isRead());
				Utils.showHidePopup("Chapter set ".concat(chapter.isRead() ? "Read" : "Unread"), 1000);
			}
			break;
		case VK_S: chapterStrip.doThis(CHANGE_SCROLL); break;
		case VK_DELETE:
			if(JOptionPane.showConfirmDialog(null, "confirm to delete") != JOptionPane.YES_OPTION)
				break;
			delete();
			break;
		default: break;
		}
	}

	private void delete() {
		try {
			if(iter.delete()) {
				Utils.showHidePopup("chapter deleted", 1500);
				if(chapters.isEmpty()){
					Utils.showHidePopup( "no chapters in manga", 1500);
					exit();
					return;
				}
				savePoints.remove(chapter);
				changeChapter(iter.current());	
			}
		} catch (IOException  e) {
			logger.log(Level.SEVERE, "chapter delete failed, see logs"+chapter, e);
		}
	}
	private void previousChapter() {
		if(!iter.hasPrevious())
			Utils.showHidePopup("No "+(chapters.getOrder() == Order.INCREASING ? "Previous" : "New")+" Chapters", 1000);
		else
			changeChapter(iter.previous());
	}

	private void nextChapter() {
		if(!iter.hasNext())
			Utils.showHidePopup("No "+(chapters.getOrder() == Order.INCREASING ?  "New" : "Previous")+" Chapters", 1000);
		else
			changeChapter(iter.next());
	}

	@Override public void mouseMoved(MouseEvent e) { doIt(); }
	@Override public void mouseDragged(MouseEvent e) { doIt(); }

	private void doIt() {
		if(mouseMovedTime > 0)
			return;

		mouseMovedTime = System.currentTimeMillis();
		setCursor(simpleCursor);
	}

	private Chapter chapter;
	private void changeChapter(Chapter chap) {
		if(chapters == null){
			Utils.showHidePopup("No Chapters in Manga", 1500);
			chapterStrip.clear();
			exit();
			return;
		}
		if(chapter != null)
			savePoints.put(chapter, new SoftReference<>(new ChapterSavePoint(manga, chapter, chapterStrip.x, chapterStrip.y, chapterStrip.scale, System.currentTimeMillis())));

		this.chapter = chap;
		chapter.setRead(true);
		mouseMovedTime = 0;
		chapterStrip.load(chapter, ReferenceUtils.get(savePoints.get(chapter)), manga.getMangaName(), manga.getUnreadCount());
		chapterStrip.repaint();
	}
	
	private void exit() {
		savePoints.clear();
		cursorHider.stop();
		chapterStrip.clear();
		
		manga.setSavePoint(chapter, chapterStrip.x, chapterStrip.y, chapterStrip.scale, System.currentTimeMillis());
		chapter = null;
		
		iter.close();
		setVisible(false);
		changer.changeTo(Change.CLOSED);
		//TODO dispose();
	}
	@Override public void mouseClicked(MouseEvent e) {/* NOT USING */}
	@Override public void mousePressed(MouseEvent e) {/* NOT USING */}
	@Override public void mouseReleased(MouseEvent e) {/* NOT USING */}
	@Override public void mouseEntered(MouseEvent e) {/* NOT USING */}
	@Override public void mouseExited(MouseEvent e) {/* NOT USING */}
	@Override public void keyTyped(KeyEvent e) {/* NOT USING */}
	@Override public void keyReleased(KeyEvent e) {/* NOT USING */}

	@Override
	protected void finalize() throws Throwable {
		printFinalize();
		super.finalize();
	}
} 
