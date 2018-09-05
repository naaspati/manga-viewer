package samrock.gui;

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
import samrock.manga.Manga;
import samrock.manga.chapter.Chapter;
import samrock.manga.chapter.ChapterSavePoint;
import samrock.manga.maneger.MangaManeger;
import samrock.utils.RH;
import samrock.utils.Utils;

final class ChaptersListView extends JPanel {
    private static final long serialVersionUID = -5991830145164113289L;

    private Manga manga;
    private int selectChapterIndex = -1;

    private final JButton refreshButton;
    private final JButton editChaptersButton;
    private final JButton chaptersSorting_1_to_9_button;
    private final JButton chaptersSorting_9_to_1_button;
    private final JScrollPane chapterListScrollpane;
    private final JPanel chaptersListPanel;
    private final Color CONTROL_BACKGROUND;

    /**
     * COUNT_OF_ELEMENT_TO_SHOW_AT_ONCE,
     * <br> number of chapter should be loaded at once in ChapterList
     */
    private final int MAX_COUNT;

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

    public ChaptersListView(Changer changer) {
        super(new BorderLayout(), false);

        this.manga = MangaManeger.getInstance().getCurrentManga(); 
        this.changer = changer;


        //ChapterLabel Constants
        NORMAL_READ_FOREGROUND=RH.getColor("chapterlabel.normal.read.foreground");
        NORMAL_UNREAD_FOREGROUND=RH.getColor("chapterlabel.normal.unread.foreground");
        NORMAL_BACKGROUND=RH.getColor("chapterlabel.normal.background");

        FNT_READ_FOREGROUND=RH.getColor("chapterlabel.fnt.read.foreground");
        FNT_UNREAD_FOREGROUND=RH.getColor("chapterlabel.fnt.unread.foreground");
        FNT_BACKGROUND=RH.getColor("chapterlabel.fnt.background");

        IF_READ_FOREGROUND=RH.getColor("chapterlabel.if.read.foreground");
        IF_UNREAD_FOREGROUND=RH.getColor("chapterlabel.if.unread.foreground");
        IF_BACKGROUND=RH.getColor("chapterlabel.if.background");

        DEFAULT_FONT  = RH.getFont("chapterlabel.font");
        Color borderLineColor = RH.getColor("chapterlabel.borderlinecolor");
        Border bottomborder = BorderFactory.createMatteBorder(0, 0, 2, 0, borderLineColor);

        BORDER = BorderFactory.createCompoundBorder(bottomborder, BorderFactory.createEmptyBorder(DEFAULT_FONT.getSize(), 0, DEFAULT_FONT.getSize(), 0));


        //------------------------------------------

        CONTROL_BACKGROUND = RH.getColor("chapterspanel.background");
        MAX_COUNT = RH.getInt("chapterspanel.count_of_element_to_show_at_once");

        chapterLabels = new ChapterLabel[MAX_COUNT];
        currentChapterIndices = new int[MAX_COUNT];

        Arrays.fill(currentChapterIndices, -1);

        chaptersListPanel = new JPanel(new GridLayout(MAX_COUNT, 1), false);
        chaptersListPanel.setOpaque(true);
        chaptersListPanel.setBackground(RH.getColor("chapterspanel.list.background"));

        //filling chapterLabels
        for (int i = 0; i < chapterLabels.length; i++){
            chapterLabels[i] = new ChapterLabel(i);
            chaptersListPanel.add(chapterLabels[i]);
        }

        chapterListScrollpane = new JScrollPane(chaptersListPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chapterListScrollpane.setDoubleBuffered(false);

        add(chapterListScrollpane, BorderLayout.CENTER);

        String sideIconLocFormatString = RH.getString("chapterspanel.icon.location.format");
        int sideImageCount = RH.getInt("chapterspanel.icons.count");

        JLabel sideLabel = new JLabel(new ImageIcon(String.format(sideIconLocFormatString, new Random().nextInt(sideImageCount))));
        sideLabel.setDoubleBuffered(false);
        sideLabel.setOpaque(true);
        sideLabel.setBackground(Color.black);

        add(sideLabel, BorderLayout.WEST);

        JPanel control = new JPanel(new BorderLayout(), false);
        control.setOpaque(true);
        control.setBackground(CONTROL_BACKGROUND);

        refreshButton = Utils.createButton("chapterspanel.button.refresh.icon", "chapterspanel.button.refresh.tooltip",null,null, e -> {
            manga.resetChapters();
            changeManga();
            Utils.showHidePopup("Chapters Resetted", 1000);
        }); 

        editChaptersButton = Utils.createButton("chapterspanel.button.openeditchapter.icon", "chapterspanel.button.openeditchapter.tooltip",null,null, e -> changer.changeTo(Change.START_CHAPTER_EDITOR)); 
        chaptersSorting_1_to_9_button = Utils.createButton("chapterspanel.sort.button.1to9.icon", "chapterspanel.sort.button.1to9.tooltip",null,null, e -> changeChapterOrder());
        chaptersSorting_9_to_1_button = Utils.createButton("chapterspanel.sort.button.9to1.icon", "chapterspanel.sort.button.9to1.tooltip",null,null, e -> changeChapterOrder());

        pageNumberBox = new JComboBox<>();

        pageNumberBox.setRenderer((JList<? extends int[]> list, int[] value, int index,
                boolean isSelected, boolean cellHasFocus) -> {

                    if(manga == null){
                        pageNumberBox.setToolTipText(null);
                        return new JLabel(Arrays.toString(value));
                    }

                    Chapter c1 = manga.getChapter(value[0]);
                    Chapter c2 = manga.getChapter(value[1]);

                    JLabel ll = new JLabel(StringUtils.doubleToString(c1.getNumber()) + " - "+StringUtils.doubleToString(c2.getNumber()));
                    ll.setToolTipText("<html>"+c1.getName()+"<br>"+c2.getName()+"</html>");

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
        changeManga();

    }

    void changeManga() {
        manga  = MangaManeger.getInstance().getCurrentManga();
        int mangaChapterCount = manga.getChaptersCount();

        ArrayList<int[]> list = new ArrayList<>();

        for (int i = 0; ; i++) {
            if(i*MAX_COUNT >= mangaChapterCount - 1){
                list.add(new int[]{i*MAX_COUNT , i*MAX_COUNT});
                break;
            }
            if((i+1)*MAX_COUNT >= mangaChapterCount){
                list.add(new int[]{i*MAX_COUNT, mangaChapterCount - 1});
                break;
            }
            list.add(new int[]{i*MAX_COUNT, (i+1)*MAX_COUNT - 1});
        }
        pageNumberBox.setModel(new DefaultComboBoxModel<>(new Vector<>(list)));
        int selectIndex = 0;
        ChapterSavePoint savepoint = MangaManeger.getInstance().getCurrentSavePoint();
        String str = savepoint == null ? null : savepoint.getChapterFileName();

        if(str != null){
            loop:
                for (int i = 0; i < list.size(); i++) {
                    int[] range = list.get(i);
                    for (int j = range[0]; j <= range[1]; j++) {
                        if(manga.getChapter(j).getFileName().equals(str)){
                            selectIndex = i;
                            break loop;
                        }
                    }
                }
        }
        pageNumberBox.setSelectedIndex(selectIndex);

        EventQueue.invokeLater(() -> {
            chaptersSorting_9_to_1_button.setVisible(manga.isChaptersInIncreasingOrder());
            chaptersSorting_1_to_9_button.setVisible(!manga.isChaptersInIncreasingOrder());
        });

        revalidate();
        repaint();

    }

    private void changeChapterOrder(){
        manga.reverseChaptersOrder();
        changeManga();
        EventQueue.invokeLater(() -> {
            pageNumberBox.setSelectedIndex(0);
            chapterLabels[0].requestFocus();
        });
    }

    private void resetAllChapterLabels() {
        ChapterSavePoint savepoint = MangaManeger.getInstance().getCurrentSavePoint();
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
    private final Font DEFAULT_FONT;
    private final Color NORMAL_READ_FOREGROUND;
    private final Color NORMAL_UNREAD_FOREGROUND;
    private final Color NORMAL_BACKGROUND;

    //FNT = FILE_NOT_FOUND
    private final Color FNT_READ_FOREGROUND;
    private final Color FNT_UNREAD_FOREGROUND;
    private final Color FNT_BACKGROUND;

    //IF = IN_FOCUS
    private final Color IF_READ_FOREGROUND;
    private final Color IF_UNREAD_FOREGROUND;
    private final Color IF_BACKGROUND;

    private final Border BORDER;


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
                    Chapter c = manga.getChapter(currentChapterIndices[indexToFollow]);
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
                        setForeground(manga.getChapter(currentChapterIndices[indexToFollow]).isRead() ? IF_READ_FOREGROUND : IF_UNREAD_FOREGROUND);
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
            if(!manga.getChapter(index).chapterFileExists())
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

            Chapter chapter = manga.getChapter(index);

            setText("<html>&emsp;&emsp;"+chapter.getName()+"</html>");
            setBackground(chapter.chapterFileExists() ? NORMAL_BACKGROUND : FNT_BACKGROUND);
            setForeground(chapter.chapterFileExists() ? (chapter.isRead() ? NORMAL_READ_FOREGROUND : NORMAL_UNREAD_FOREGROUND) : (chapter.isRead() ? FNT_READ_FOREGROUND : FNT_UNREAD_FOREGROUND));

            return chapter.getFileName();
        }
    }
}



