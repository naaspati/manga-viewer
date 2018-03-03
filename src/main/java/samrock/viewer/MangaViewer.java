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
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import samrock.gui.Change;
import samrock.gui.Changer;
import samrock.manga.Manga;
import samrock.manga.chapter.BadChapterNameException;
import samrock.manga.chapter.Chapter;
import samrock.manga.chapter.ChapterSavePoint;
import samrock.manga.maneger.MangaManeger;
import samrock.utils.RH;
import samrock.utils.Utils;
public class MangaViewer extends JFrame implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 9222652000321437542L;

    private static MangaViewer instance;
    public static final int OPEN_MOST_RECENT_CHAPTER = 0x802*-1;

    /**
     * @param mangaViewerWatcher
     * @param chapterIndex use {@link #OPEN_MOST_RECENT_CHAPTER}, if you wish to open most_recent_chapter in current manga, otherwise a chapter index, 
     * if index < 0 || index > chpatersCount - 1 , than most_recent_chapter will be opened  
     */
    public static void openMangaViewer(Changer mangaViewerWatcher, int chapterIndex) {
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
    private final Changer changer;

    /**
     * chapterOrdering Is in Increasing order ? 
     */
    private final boolean chaptersOrder;

    /**
     * use this constructor to start mangaViewer most_recent_chapter in currentManga
     */
    private MangaViewer(Changer  mangaViewerWatcher){
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
    private MangaViewer(Changer mangaViewerWatcher, int chapterIndex){
        super("Manga Viewer");

        this.changer = mangaViewerWatcher; 
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

        addMouseMotionListener(this);
        addKeyListener(this);
        addMouseWheelListener(this);

        Utils.setPopupRelativeTo(this);
        changeChapter(chapter_index);
        setVisible(true);
        toFront();
        changer.changeTo(Change.STARTED);
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
            case VK_BACK_SPACE:
                int c2 = chaptersOrder ? chapter_index - 1 : chapter_index + 1;
                if(c2 >= manga.getChaptersCount() || c2 < 0)
                    Utils.showHidePopup("No Previous Chapters", 1000);
                else
                    changeChapter(c2);
                break;
            case VK_HOME:
                chapterStrip.doThis(GOTO_START);
                break;
            case VK_END:
                chapterStrip.doThis(GOTO_END);
                break;
            case VK_F2:
                String oldName = chapter.getName();
                String newName = JOptionPane.showInputDialog("<html>Rename?<br>any invalid characters for naming <br>a file will removed</html>", oldName);
                try {
                    boolean status = chapter.rename(newName);
                    if(status){
                        chapterStrip.setChapterName(chapter.getName());
                        chapterStrip.repaint();
                        Utils.showHidePopup("chapter renamed", 1500);
                    }                        
                } catch (BadChapterNameException e1) {
                    Utils.openErrorDialoag("failed to rename chapter", e1);
                }

                break;
            case VK_ESCAPE: exit(); break;
            case VK_G: chapterStrip.doThis(GOTO); break;
            case VK_M: setState(JFrame.ICONIFIED); break;
            case VK_H: chapterStrip.doThis(OPEN_HELP_FILE); break;
            case VK_R:
                if(chapter != null ) chapter.setRead(!chapter.isRead());
                Utils.showHidePopup("Chapter set ".concat(chapter.isRead() ? "Read" : "Unread"), 1000);
                break;
            case VK_S: chapterStrip.doThis(CHANGE_SCROLL); break;
            case VK_DELETE:
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
            default: break;
        }
    }
    
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
        chapterStrip.load(chapter, savePoints.get(chapter), manga.getMangaName(), manga.getUnreadCount());
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

        changer.changeTo(Change.CLOSED);
        dispose();
        instance = null;

    }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}
} 
