package samrock.viewer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.function.IntConsumer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import samrock.manga.Chapter;
import samrock.manga.ChapterSavePoint;
import samrock.manga.Manga;
import samrock.manga.MangaManeger;
import samrock.utils.RH;
import samrock.utils.Utils;
public class MangaViewer extends JFrame{
	private static final long serialVersionUID = 9222652000321437542L;
	public static final int STARTED = 0x800;
	public static final int CLOSED = 0x801;
	public static final int OPEN_MOST_RECENT_CHAPTER = 0x802*-1;

	private static MangaViewer instance;

	/**
	 * @param mangaViewerWatcher
	 * @param chapterIndex use {@link #OPEN_MOST_RECENT_CHAPTER}, if you wish to open most_recent_chapter in current manga, otherwise a chapter index, 
	 * if index < 0 || index > chpatersCount - 1 , than most_recent_chapter will be opened  
	 */
	public static void openMangaViewer(IntConsumer mangaViewerWatcher, int chapterIndex) {
		if (instance != null)
			return;
		instance = new MangaViewer(mangaViewerWatcher, chapterIndex);
	}

	private final Manga manga;
	/**
	 * chapterStrip
	 */
	private final MangaChapterStrip chapterStrip;
	private static final Cursor simpleCursor = Cursor.getDefaultCursor();
	private static final Cursor invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(), "Gayab");;

	/**
	 * dont modify directly, use {@link #changeChapter(int)}
	 */
	private int chapter_index;
	private Chapter chapter;

	private long mouseMovedTime = 0;
	private final Timer timer;

	private final MangaManeger mangaManeger;
	private final IntConsumer watcher;
	/**
	 * chapterOrdering Is in Increasing order ? 
	 */
	private final boolean chaptersOrder;

	/**
	 * use this constructor to start mangaViewer most_recent_chapter in currentManga
	 */
	private MangaViewer(IntConsumer mangaViewerWatcher){
		this(mangaViewerWatcher, -1);
	}

	private final HashMap<Chapter, ChapterSavePoint> savePoints;

	/**
	 * this is  used when MangaViewer is closed with some error, and not disposed
	 * on exit this method will dispose it 
	 */
	private final Runnable disposer;

	/**
	 * @param mangaViewerWatcher 
	 * @param chapterIndex index of chapter with which app will start
	 */
	private MangaViewer(IntConsumer mangaViewerWatcher, int chapterIndex){
		super("Manga Viewer");

		this.watcher = mangaViewerWatcher; 
		mangaManeger = MangaManeger.getInstance();
		manga = mangaManeger.getCurrentManga();
		chaptersOrder = manga.isChaptersInIncreasingOrder(); 

		savePoints = new HashMap<>(manga.getChaptersCount());

		if(chapterIndex < 0 || chapterIndex >= manga.getChaptersCount())
			chapterIndex = -1;

		Utils.addExitTasks(disposer = () -> {
			Utils.logError("MangaViewer disposer is used",MangaViewer.class,103/*{LINE_NUMBER}*/, null);
			dispose();
		});

		if(chapterIndex == -1){
			chapter_index = chaptersOrder ? 0 : manga.getChaptersCount() - 1;

			ChapterSavePoint savePoint = mangaManeger.getCurrentSavePoint();

			for (int i = 0; i < manga.getChaptersCount(); i++) {
				if(manga.getChapter(i).getFileName().equals(savePoint.getChapterFileName())){
					chapter_index = i;
					savePoints.put(manga.getChapter(i), savePoint);
					break;
				}
			}
		}
		else
			chapter_index = chapterIndex;

		chapterStrip = new MangaChapterStrip();

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setUndecorated(true);
		getContentPane().add(chapterStrip);
		getContentPane().setBackground(Color.black);
		setIconImage(RH.getImageIcon("app.icon").getImage());

		timer = new Timer(2500, e -> {
			if(mouseMovedTime != -1 && System.currentTimeMillis() - mouseMovedTime > 2000){
				setCursor(invisibleCursor);
				mouseMovedTime = -1;
			}
		});

		timer.start();

		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) { doIt(); }
			@Override
			public void mouseDragged(MouseEvent e) { doIt(); }

			private void doIt() {
				if(mouseMovedTime > 0)
					return;
				
				mouseMovedTime = System.currentTimeMillis();
				setCursor(simpleCursor);
			}
		});
		
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
					chapterStrip.scrollUp();
					break;
				case KeyEvent.VK_PAGE_UP:
					chapterStrip.scrollUp();
					break;
				case KeyEvent.VK_DOWN:
					chapterStrip.scrollDown();
					break;
				case KeyEvent.VK_PAGE_DOWN:
					chapterStrip.scrollDown();
					break;
				case KeyEvent.VK_LEFT:
					chapterStrip.scrollLeft();
					break;
				case KeyEvent.VK_RIGHT:
					chapterStrip.scrollRight();
					break;
				case KeyEvent.VK_ADD:
					chapterStrip.zoomIn();
					break ;
				case KeyEvent.VK_SUBTRACT:
					chapterStrip.zoomOut();
					break;
				case KeyEvent.VK_Z:
					chapterStrip.doThis(MangaChapterStrip.CHANGE_ZOOM);
					break;
				case KeyEvent.VK_1: 
					chapterStrip.zoom(1.0d);
					break;
				case KeyEvent.VK_NUMPAD1: 
					chapterStrip.zoom(1.0d);
					break;
				case KeyEvent.VK_2: 
					chapterStrip.zoom(1.5d);
					break;
				case KeyEvent.VK_NUMPAD2: 
					chapterStrip.zoom(1.5d);
					break;
				case KeyEvent.VK_3: 
					chapterStrip.zoom(1.75d);
					break;
				case KeyEvent.VK_NUMPAD3: 
					chapterStrip.zoom(1.75d);
					break;
				case KeyEvent.VK_4: 
					chapterStrip.zoom(2.0d);
					break;
				case KeyEvent.VK_NUMPAD4: 
					chapterStrip.zoom(2.0d);
					break;
				case KeyEvent.VK_5: 
					chapterStrip.zoom(2.25d);
					break;
				case KeyEvent.VK_NUMPAD5: 
					chapterStrip.zoom(2.25d);
					break;
				case KeyEvent.VK_6: 
					chapterStrip.zoom(2.5d);
					break;
				case KeyEvent.VK_NUMPAD6: 
					chapterStrip.zoom(2.5d);
					break;
				case KeyEvent.VK_SPACE://next chapter
					if(chapterStrip.scale != 1.0)
						chapterStrip.zoom(1.0);
					else{
						int c2 = chaptersOrder ? chapter_index + 1 : chapter_index - 1;
						if(c2 >= manga.getChaptersCount() || c2 < 0)
							Utils.showHidePopup("No New Chapters", 1000);
						else
							changeChapter(c2);
					}
					break;
				case KeyEvent.VK_BACK_SPACE:
					int c2 = chaptersOrder ? chapter_index - 1 : chapter_index + 1;
					if(c2 >= manga.getChaptersCount() || c2 < 0)
						Utils.showHidePopup("No Previous Chapters", 1000);
					else
						changeChapter(c2);
					break;
				case KeyEvent.VK_HOME:
					chapterStrip.doThis(MangaChapterStrip.GOTO_START);
					break;
				case KeyEvent.VK_END:
					chapterStrip.doThis(MangaChapterStrip.GOTO_END);
					break;
				case KeyEvent.VK_F2:
					String oldName = chapter.getName();
					String newName = JOptionPane.showInputDialog("<html>Rename?<br>any invalid characters for naming <br>a file will removed</html>", oldName);
					String status = chapter.rename(newName);

					if(status == null){
						chapterStrip.setChapterName(chapter.getName());
						chapterStrip.repaint();
						Utils.showHidePopup("chapter renamed", 1500);
					}
					else
						Utils.showHidePopup(status, 1500);
					break;
				case KeyEvent.VK_ESCAPE:
					exit();
					break;
				case KeyEvent.VK_G:
					chapterStrip.doThis(MangaChapterStrip.GOTO);
					break;
				case KeyEvent.VK_M:
					setState(JFrame.ICONIFIED);
					break;
				case KeyEvent.VK_H:
					chapterStrip.doThis(MangaChapterStrip.OPEN_HELP_FILE);
					break;
				case KeyEvent.VK_R:
					if(chapter != null ) chapter.setRead(!chapter.isRead());
					Utils.showHidePopup("Chapter set ".concat(chapter.isRead() ? "Read" : "Unread"), 1000);
					break;
				case KeyEvent.VK_S: 
					chapterStrip.doThis(MangaChapterStrip.CHANGE_SCROLL);
					break;
				case KeyEvent.VK_DELETE:
					if(JOptionPane.showConfirmDialog(null, "confirm to delete") != JOptionPane.YES_OPTION)
						break;

					if(chapter.delete()){
						Utils.showHidePopup("chapter deleted", 1500);
						if(manga.getChaptersCount() == 0){
							Utils.showHidePopup( "no chapters in manga", 1500);
							exit();
							break;
						}
						savePoints.remove(chapter);
						changeChapter(chapter_index >= manga.getChaptersCount() ? manga.getChaptersCount() - 1 : chapter_index);
					}
					else
						Utils.showHidePopup("chapter delete failed, see logs", 1500);
					break;
				default:
					break;
				}
			}
		});

		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.getWheelRotation() > 0)
					chapterStrip.scrollDown();
				else if(e.getWheelRotation() < 0)
					chapterStrip.scrollUp();
			}
		});

		Utils.setPopupRelativeTo(this);
		changeChapter(chapter_index);
		setVisible(true);
		toFront();
		watcher.accept(STARTED);
	}

	private void changeChapter(int chapterIndex) {
		if(manga.getChaptersCount() == 0){
			Utils.showHidePopup("No Chapters in Manga", 1500);
			exit();
			return;
		}
		
		if(chapter != null && !chapter.isDeleted())
			savePoints.put(chapter, new ChapterSavePoint(manga, chapter.getFileName(), chapterStrip.x, chapterStrip.y, chapterStrip.scale, System.currentTimeMillis()));

		this.chapter_index = chapterIndex;
		this.chapter = manga.getChapter(chapter_index);
		chapter.setRead(true);
		mouseMovedTime = 0;
		chapterStrip.load(chapter, savePoints.get(chapter), manga.MANGA_NAME, manga.getUnreadCount());
		chapterStrip.repaint();
		System.gc();
	}

	private void exit() {
		Utils.removeExitTasks(disposer);
		
		timer.stop();
		long time = System.currentTimeMillis();

		mangaManeger.getCurrentSavePoint().reset(chapter.getFileName(), chapterStrip.x, chapterStrip.y, chapterStrip.scale, time);
		manga.setLastReadTime(time);
		if(manga.getChapCountPc() == 0)
			mangaManeger.addMangaToDeleteQueue(manga);

		watcher.accept(CLOSED);
		dispose();
		instance = null;
		
	}
} 
