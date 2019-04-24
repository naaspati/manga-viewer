package samrock.gui.chapter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import sam.string.StringUtils;
import samrock.Utils;
import samrock.api.AppConfig;
import samrock.api.Change;
import samrock.api.Changer;
import samrock.manga.Chapter;
import samrock.manga.Chapters;
import samrock.manga.Manga;
import samrock.manga.Order;
import samrock.manga.maneger.api.MangaManeger;
import samrock.manga.recents.ChapterSavePoint;

public final class ChaptersListView extends JPanel {
    private static final long serialVersionUID = -5991830145164113289L;

    private Chapters chapters;
    private int selectChapterIndex = -1;

    private final JButton refreshButton;
    private final JButton editChaptersButton;
    private final JButton chaptersSorting_1_to_9_button;
    private final JButton chaptersSorting_9_to_1_button;
    private final JScrollPane chapterListScrollpane;
    private final JPanel chaptersListPanel;
    private static final Color CONTROL_BACKGROUND;

    /**
     * COUNT_OF_ELEMENT_TO_SHOW_AT_ONCE,
     * <br> number of chapter should be loaded at once in ChapterList
     */
    private static final int MAX_COUNT;
    
    static {
    	AppConfig config = Utils.config();
        CONTROL_BACKGROUND = config.getColor("chapterspanel.background");
        MAX_COUNT = config.getInt("chapterspanel.count_of_element_to_show_at_once");
    }

    private final ChapterLabel[] chapterLabels;

    /**
     *this arrays contains chapters indices of those chapters which are visible right now on screen<br>
     *if any value of is greater than {@link #mangaChapterCount}, than the value is set to -1 
     */
    private final int[] currentChapterIndices;

    /**
     * contains a closed range(min and max are included)
     * int[0] = min index<br>
     * int[1] = max index<br>
     */
    private final JComboBox<int[]> pageNumberBox;
    private final Changer changer;
	private Manga manga;
	private MangaManeger mangaManeger;

    public ChaptersListView(
    		Changer changer, 
    		AppConfig config,
    		MangaManeger mangaManeger
    		) {
        super(new BorderLayout(), false);

        this.changer = changer;

        chapterLabels = new ChapterLabel[MAX_COUNT];
        currentChapterIndices = new int[MAX_COUNT];

        Arrays.fill(currentChapterIndices, -1);

        chaptersListPanel = new JPanel(new GridLayout(MAX_COUNT, 1), false);
        chaptersListPanel.setOpaque(true);
        chaptersListPanel.setBackground(config.getColor("chapterspanel.list.background"));

        //filling chapterLabels
        for (int i = 0; i < chapterLabels.length; i++){
            chapterLabels[i] = new ChapterLabel(i);
            chaptersListPanel.add(chapterLabels[i]);
        }

        chapterListScrollpane = new JScrollPane(chaptersListPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chapterListScrollpane.setDoubleBuffered(false);

        add(chapterListScrollpane, BorderLayout.CENTER);

        String sideIconLocFormatString = config.getString("chapterspanel.icon.location.format");
        int sideImageCount = config.getInt("chapterspanel.icons.count");

        JLabel sideLabel = new JLabel(new ImageIcon(String.format(sideIconLocFormatString, new Random().nextInt(sideImageCount))));
        sideLabel.setDoubleBuffered(false);
        sideLabel.setOpaque(true);
        sideLabel.setBackground(Color.black);

        add(sideLabel, BorderLayout.WEST);

        JPanel control = new JPanel(new BorderLayout(), false);
        control.setOpaque(true);
        control.setBackground(CONTROL_BACKGROUND);

        refreshButton = Utils.createButton("chapterspanel.button.refresh.icon", "chapterspanel.button.refresh.tooltip",null,null, e -> {
            chapters.reload();
            reset();
            Utils.showHidePopup("Chapters Resetted", 1000);
        }, config); 

        editChaptersButton = Utils.createButton("chapterspanel.button.openeditchapter.icon", "chapterspanel.button.openeditchapter.tooltip",null,null, e -> changer.changeTo(Change.START_CHAPTER_EDITOR), config); 
        chaptersSorting_1_to_9_button = Utils.createButton("chapterspanel.sort.button.1to9.icon", "chapterspanel.sort.button.1to9.tooltip",null,null, e -> changeChapterOrder(), config);
        chaptersSorting_9_to_1_button = Utils.createButton("chapterspanel.sort.button.9to1.icon", "chapterspanel.sort.button.9to1.tooltip",null,null, e -> changeChapterOrder(), config);

        pageNumberBox = new JComboBox<>();

        pageNumberBox.setRenderer((JList<? extends int[]> list, int[] value, int index,
                boolean isSelected, boolean cellHasFocus) -> {

                    if(chapters == null){
                        pageNumberBox.setToolTipText(null);
                        return new JLabel(Arrays.toString(value));
                    }

                    Chapter c1 = chapters.get(value[0]);
                    Chapter c2 = chapters.get(value[1]);

                    JLabel ll = new JLabel(StringUtils.doubleToString(c1.getNumber()) + " - "+StringUtils.doubleToString(c2.getNumber()));
                    ll.setToolTipText("<html>"+c1.getTitle()+"<br>"+c2.getTitle()+"</html>");

                    ll.setFont(DEFAULT_FONT);
                    return ll;
                });
        pageNumberBox.addActionListener(e -> { 
            int[] page = pageNumberBox.getItemAt(pageNumberBox.getSelectedIndex());

            int j = 0;

            for (int i = page[0]; i <= page[1]; j++, i++)
                currentChapterIndices[j] = i;

            for (;  j < MAX_COUNT; j++)
                currentChapterIndices[j] = -1;

            EventQueue.invokeLater(() -> {
                resetAllChapterLabels();
                pageNumberBox.revalidate();
                pageNumberBox.repaint();
            });});

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT), false);
        buttonsPanel.setOpaque(true);
        buttonsPanel.setBackground(CONTROL_BACKGROUND);

        buttonsPanel.add(chaptersSorting_1_to_9_button);
        buttonsPanel.add(chaptersSorting_9_to_1_button);
        buttonsPanel.add(refreshButton);
        buttonsPanel.add(editChaptersButton);


        JPanel listBoxPanel = new JPanel(false);

        listBoxPanel.setOpaque(true);
        listBoxPanel.setBackground(CONTROL_BACKGROUND);
        listBoxPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 5, 0));

        JLabel l = new JLabel("Go to pages ");
        l.setFont(DEFAULT_FONT);
        l.setForeground(NORMAL_UNREAD_FOREGROUND);

        listBoxPanel.add(l);
        listBoxPanel.add(pageNumberBox);

        pageNumberBox.setFont(DEFAULT_FONT);

        JPanel p2 = new JPanel(new BorderLayout(), false);
        p2.setOpaque(true);
        p2.setBackground(CONTROL_BACKGROUND);


        p2.add(buttonsPanel, BorderLayout.EAST);
        p2.add(listBoxPanel, BorderLayout.WEST); 

        add(p2, BorderLayout.NORTH);
        reset();

    }

    public void reset() {
    	this.manga = mangaManeger.getSelectedManga();
        this.chapters = manga.getChapters();
        final int size = chapters.size();

        ArrayList<int[]> list = new ArrayList<>();

        for (int i = 0; ; i++) {
            if(i*MAX_COUNT >= size - 1){
                list.add(new int[]{i*MAX_COUNT , i*MAX_COUNT});
                break;
            }
            if((i+1)*MAX_COUNT >= size){
                list.add(new int[]{i*MAX_COUNT, size - 1});
                break;
            }
            list.add(new int[]{i*MAX_COUNT, (i+1)*MAX_COUNT - 1});
        }
        pageNumberBox.setModel(new DefaultComboBoxModel<>(new Vector<>(list)));
        int selectIndex = 0;
        ChapterSavePoint savepoint = manga.getSavePoint();
        String str = savepoint == null ? null : savepoint.getChapterFileName();

        if(str != null){
            loop:
                for (int i = 0; i < list.size(); i++) {
                    int[] range = list.get(i);
                    for (int j = range[0]; j <= range[1]; j++) {
                        if(chapters.get(j).getFileName().equals(str)){
                            selectIndex = i;
                            break loop;
                        }
                    }
                }
        }
        pageNumberBox.setSelectedIndex(selectIndex);

        EventQueue.invokeLater(() -> {
            chaptersSorting_9_to_1_button.setVisible(chapters.getOrder() == Order.INCREASING);
            chaptersSorting_1_to_9_button.setVisible(chapters.getOrder() == Order.DECREASING);
        });

        revalidate();
        repaint();

    }

    private void changeChapterOrder(){
        chapters.flip();
        reset();
        
        EventQueue.invokeLater(() -> {
            pageNumberBox.setSelectedIndex(0);
            chapterLabels[0].requestFocus();
        });
    }

    private void resetAllChapterLabels() {
        ChapterSavePoint savepoint = manga.getSavePoint();
        String str = savepoint == null ? null : savepoint.getChapterFileName();

        for (int i = 0; i < chapterLabels.length; i++){
            String str2 = chapterLabels[i].reset();
            if(str2 != null && str2.equals(str))
                chapterLabels[i].requestFocus();
        }
    }

    public int getSelectChapterIndex() {
        return selectChapterIndex;
    }

    //ChapterLabel Constants
    private static final Font DEFAULT_FONT;
    private static final Color NORMAL_READ_FOREGROUND;
    private static final Color NORMAL_UNREAD_FOREGROUND;
    private static final Color NORMAL_BACKGROUND;

    //FNT = FILE_NOT_FOUND
    private static final Color FNT_READ_FOREGROUND;
    private static final Color FNT_UNREAD_FOREGROUND;
    private static final Color FNT_BACKGROUND;

    //IF = IN_FOCUS
    private static final Color IF_READ_FOREGROUND;
    private static final Color IF_UNREAD_FOREGROUND;
    private static final Color IF_BACKGROUND;

    private static final Border BORDER;
    
    static {
    	AppConfig config = Utils.config();
    	
        //ChapterLabel Constants
        NORMAL_READ_FOREGROUND=config.getColor("chapterlabel.normal.read.foreground");
        NORMAL_UNREAD_FOREGROUND=config.getColor("chapterlabel.normal.unread.foreground");
        NORMAL_BACKGROUND=config.getColor("chapterlabel.normal.background");

        FNT_READ_FOREGROUND=config.getColor("chapterlabel.fnt.read.foreground");
        FNT_UNREAD_FOREGROUND=config.getColor("chapterlabel.fnt.unread.foreground");
        FNT_BACKGROUND=config.getColor("chapterlabel.fnt.background");

        IF_READ_FOREGROUND=config.getColor("chapterlabel.if.read.foreground");
        IF_UNREAD_FOREGROUND=config.getColor("chapterlabel.if.unread.foreground");
        IF_BACKGROUND=config.getColor("chapterlabel.if.background");

        DEFAULT_FONT  = config.getFont("chapterlabel.font");
        Color borderLineColor = config.getColor("chapterlabel.borderlinecolor");
        Border bottomborder = BorderFactory.createMatteBorder(0, 0, 2, 0, borderLineColor);

        BORDER = BorderFactory.createCompoundBorder(bottomborder, BorderFactory.createEmptyBorder(DEFAULT_FONT.getSize(), 0, DEFAULT_FONT.getSize(), 0));
    }

    private final class ChapterLabel extends JLabel{
        private static final long serialVersionUID = 4687356692755251133L;
        private final int indexToFollow;

        ChapterLabel(int indexToFollow) {
            setDoubleBuffered(false);

            if(indexToFollow < 0 || indexToFollow >= MAX_COUNT)
                throw new ArrayIndexOutOfBoundsException("indexToFollow should be < " +MAX_COUNT +"\t and > " + 0+"\tbut was"+indexToFollow);

            this.indexToFollow = indexToFollow;

            setBorder(BORDER);
            setFont(DEFAULT_FONT);
            setBackground(NORMAL_BACKGROUND);
            setBorder(BORDER);

            setOpaque(true);
            setFocusable(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(e.getClickCount() == 1)
                        requestFocus();
                    else
                        startMangaViewer(currentChapterIndices[indexToFollow]);
                }
            });

            addFocusListener(new FocusListener() {

                @Override
                public void focusLost(FocusEvent e) {
                    Chapter c = chapters.get(currentChapterIndices[indexToFollow]);
                    setBackground(c.chapterFileExists() ? NORMAL_BACKGROUND : FNT_BACKGROUND);
                    setForeground(c.chapterFileExists() ? (c.isRead() ? NORMAL_READ_FOREGROUND : NORMAL_UNREAD_FOREGROUND) : (c.isRead() ? FNT_READ_FOREGROUND : FNT_UNREAD_FOREGROUND));
                }

                @Override
                public void focusGained(FocusEvent e) {
                    if(!isVisible())
                        return;

                    if(currentChapterIndices[indexToFollow] == -1)
                        transferFocusBackward();
                    else{
                        //shitty level calculations, but works
                        if(indexToFollow == 0)
                            chapterListScrollpane.getVerticalScrollBar().setValue(0);
                        else if(indexToFollow == MAX_COUNT - 1)
                            chapterListScrollpane.getVerticalScrollBar().setValue(chapterListScrollpane.getVerticalScrollBar().getMaximum());
                        else if(getVisibleRect().height != getHeight()){
                            chapterListScrollpane.getVerticalScrollBar().setValue(getBounds().height);
                            if(getVisibleRect().height != getHeight())
                                chapterListScrollpane.getVerticalScrollBar().setValue(indexToFollow*getHeight()*2);
                        }

                        setBackground(IF_BACKGROUND);
                        setForeground(chapters.get(currentChapterIndices[indexToFollow]).isRead() ? IF_READ_FOREGROUND : IF_UNREAD_FOREGROUND);
                    }
                }
            });

            addKeyListener(new KeyAdapter() {

                @Override
                public void keyReleased(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER)
                        startMangaViewer(currentChapterIndices[indexToFollow]);
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_UP){
                        if(indexToFollow == 0 && changePage(-1))
                            chapterLabels[MAX_COUNT - 1].requestFocus();
                        else if(indexToFollow != 0)
                            transferFocusBackward();
                    }
                    else if(e.getKeyCode() == KeyEvent.VK_DOWN){
                        if(indexToFollow == MAX_COUNT - 1 && changePage(+1))
                            chapterLabels[0].requestFocus();
                        else if(indexToFollow != MAX_COUNT - 1 && currentChapterIndices[indexToFollow + 1] != -1)
                            transferFocus();
                    }
                    else if(e.getKeyCode() == KeyEvent.VK_LEFT && changePage(-1))
                        EventQueue.invokeLater(chapterLabels[0]::requestFocus);
                    else if(e.getKeyCode() == KeyEvent.VK_RIGHT && changePage(+1))
                        EventQueue.invokeLater(chapterLabels[0]::requestFocus);
                }
            });
        }
        private void startMangaViewer(int index){
            selectChapterIndex = -1;
            if(!chapters.get(index).chapterFileExists())
                Utils.showHidePopup( "Chapter File Not Exists", 1000);
            else {
                selectChapterIndex = index;
                changer.changeTo(Change.START_MANGA_VIEWER);
            }
        }
        private boolean changePage(int changeBy){
            int choice = pageNumberBox.getSelectedIndex() + changeBy;

            if(choice < 0 || choice >= pageNumberBox.getItemCount())
                return false;
            else{
                pageNumberBox.setSelectedIndex(choice);
                resetAllChapterLabels();
                return true;
            }
        }

        /**
         * 
         * @return chapter.fileNamee
         */
        String reset(){
            int index = currentChapterIndices[indexToFollow];
            if(index == -1){
                setVisible(false);
                return null;
            }
            else if(!isVisible())
                setVisible(true);

            Chapter chapter = chapters.get(index);

            setText("<html>&emsp;&emsp;"+chapter.getTitle()+"</html>");
            setBackground(chapter.chapterFileExists() ? NORMAL_BACKGROUND : FNT_BACKGROUND);
            setForeground(chapter.chapterFileExists() ? (chapter.isRead() ? NORMAL_READ_FOREGROUND : NORMAL_UNREAD_FOREGROUND) : (chapter.isRead() ? FNT_READ_FOREGROUND : FNT_UNREAD_FOREGROUND));

            return chapter.getFileName();
        }
    }
}



