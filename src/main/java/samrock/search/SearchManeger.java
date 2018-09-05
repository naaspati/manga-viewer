package samrock.search;

import static sam.manga.newsamrock.mangas.MangasMeta.CATEGORIES;
import static sam.manga.newsamrock.mangas.MangasMeta.DESCRIPTION;
import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.newsamrock.mangas.MangasMeta.READ_COUNT;
import static sam.manga.newsamrock.mangas.MangasMeta.STATUS;
import static sam.manga.newsamrock.mangas.MangasMeta.TABLE_NAME;
import static sam.manga.newsamrock.mangas.MangasMeta.UNREAD_COUNT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.sql.querymaker.QueryMaker;
import samrock.manga.maneger.MangaManeger;
import samrock.manga.maneger.MangaManegerStatus;
import samrock.manga.maneger.MangaMangerWatcher;
import samrock.utils.IntArray;
import samrock.utils.RH;
import samrock.utils.SortingMethod;
import samrock.utils.Utils;

public final class SearchManeger {
    private static Logger logger = LoggerFactory.getLogger(SearchManeger.class);

    public static void main(String[] args) throws ClassNotFoundException{
        Utils.load();
        MangaManeger.getInstance().changeCurrentSortingMethod(SortingMethod.UPDATE_TIME_DECREASING, false);

        JFrame fm = new JFrame("testing");
        fm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SearchManeger s = new SearchManeger();
        JButton button = new JButton("button");
        button.addActionListener(e -> s.openTagsSearch());

        fm.add(s.getTextSearchComponent(), BorderLayout.NORTH);
        fm.add(button, BorderLayout.SOUTH);

        fm.pack();
        fm.setLocationRelativeTo(null);
        fm.setVisible(true);
    } 
    /**
     * in basic clear only the search component is removed
     *  
     */
    public void basicClear(){
        textSearch = null;
        TEXT_SEARCH_KEY = null;
        previousTextSearch = null;

        TAGS_SEARCH_KEYS = null;
        if(tagsSearch != null)
            tagsSearch.tagsContainer.dispose();
        tagsSearch = null;
        previousTagsSearch = null;

        textSearchResult = null;
        tagsSearchResult = null;

        sortingMethodBackup = null;

        mangaOnDisplayBackup = null;
    }

    public void fullClear() {
        mangaManeger.removeMangaManegerWatcher(mangaManegerWatcher);

        basicClear();

        mangaNames = null;
        mangaDescriptions = null;

        tagsData = null;
        readCountZeroArrayIndices = null;
        unreadCountZeroArrayIndices = null;
        completedStatusArrayIndices = null;
        searchDataLoaded = false;
        searchDataLoading = false;
    }

    /**
     * @return true if instance can be set null, (there is no active search) 
     * otherwise use wakeUp() to restore SearchManeger to previous state
     */
    public boolean hibernate(){
        if((mangaOnDisplayBackup == null || mangaOnDisplayBackup.length == mangaManeger.getMangasOnDisplayCount()) ||  
                (textSearch == null && tagsSearch == null) ||
                ((previousTextSearch == null || previousTextSearch.isEmpty()) && previousTagsSearch.isEmpty())
                ){
            fullClear();
            return true;
        }
        else {
            String text = previousTextSearch;
            HashSet<String> tags = previousTagsSearch;
            int[] b = mangaOnDisplayBackup;
            SortingMethod s = sortingMethodBackup;

            fullClear();

            previousTagsSearch = TAGS_SEARCH_KEYS = tags;
            previousTextSearch = TEXT_SEARCH_KEY = text;
            mangaOnDisplayBackup = b;
            sortingMethodBackup = s;
            return false;
        }
    } 

    /**
     * 
     * @return [0] true of textSearch is active, [1] is true if tagSearch is active 

     */
    public boolean[] wakeUp(){
        boolean[] b = new boolean[2];

        if(mangaOnDisplayBackup == null)
            return b;
        if(previousTextSearch != null && !previousTextSearch.isEmpty()){
            textSearch = new TextSearch();
            b[0] = true;
        }

        if(previousTagsSearch != null && !previousTagsSearch.isEmpty()){
            tagsSearch = new TagsSearch();
            b[1] = true;
        }

        if(b[0] || b[1]){
            mangaManeger.addMangaManegerWatcher(mangaManegerWatcher);
            loadData();
        }
        return b;
    }


    private final MangaManeger mangaManeger;
    private final MangaMangerWatcher mangaManegerWatcher;

    public static String TEXT_SEARCH_KEY;
    private TextSearch textSearch;
    /**last text search*/
    private String previousTextSearch = null;
    private String[] mangaNames;
    private String[] mangaDescriptions;

    public static HashSet<String> TAGS_SEARCH_KEYS;
    private TagsSearch tagsSearch;
    /**last tagsSearch*/
    private HashSet<String> previousTagsSearch;
    private String[] tagsData;
    private int[] readCountZeroArrayIndices;
    private int[] unreadCountZeroArrayIndices;
    private int[] completedStatusArrayIndices;

    /**
     * do not modify this (internally)
     * this can be passed mangamanger
     */
    private int[] mangaOnDisplayBackup;
    /**
     * do not modify this (internally)
     * this can be passed mangamanger
     */
    private int[] textSearchResult;
    /**
     * do not modify this (internally)
     * this can be passed mangamanger
     */
    private int[] tagsSearchResult;

    private SortingMethod sortingMethodBackup;
    private boolean haltSetMangaOnDisplay = false;

    public SearchManeger() {
        mangaManeger  =  MangaManeger.getInstance();

        mangaManegerWatcher = code -> {
            if(mangaOnDisplayBackup == null)
                return;

            if(code == MangaManegerStatus.MOD_MODIFIED_INTERNALLY){

                mangaOnDisplayBackup = mangaManeger.getMangasOnDisplay();
                tagsSearchResult = null;
                textSearchResult = null;
                previousTagsSearch.clear();
                previousTextSearch = "";
                haltSetMangaOnDisplay = true;
                searchTags();
                searchText();
                haltSetMangaOnDisplay = false;
                setMangasOnDisplay();

            }
            else if(sortingMethodBackup != mangaManeger.getCurrentSortingMethod()){

                mangaOnDisplayBackup = mangaManeger.sortArray(mangaOnDisplayBackup);
                textSearchResult = textSearchResult == null || textSearchResult.length < 2 ? textSearchResult : mangaManeger.sortArray(textSearchResult);
                tagsSearchResult = tagsSearchResult == null || tagsSearchResult.length < 2 ? tagsSearchResult : mangaManeger.sortArray(tagsSearchResult);

                sortingMethodBackup = mangaManeger.getCurrentSortingMethod();

            }
        };

        mangaManeger.addMangaManegerWatcher(mangaManegerWatcher);
    }

    public JTextField getTextSearchComponent(){
        if(textSearch != null)
            return textSearch;

        loadData();

        previousTextSearch = "";
        makeMangaOnDisplayBackup();
        return textSearch = new TextSearch();
    }

    public void openTagsSearch(){
        if(tagsSearch != null)
            tagsSearch.show();
        else{
            loadData();

            previousTagsSearch = new HashSet<>();
            TAGS_SEARCH_KEYS = previousTagsSearch;
            (tagsSearch = new TagsSearch()).show();
            makeMangaOnDisplayBackup();
        }
    }

    public void resetTextSearch() {
        TEXT_SEARCH_KEY = null;
        textSearchResult = null;
        previousTextSearch = "";
        setMangasOnDisplay();
    }

    private void searchText() {
        if(textSearch == null || mangaOnDisplayBackup == null || mangaOnDisplayBackup.length == 0)
            return;



        if(!searchDataLoaded){
            textSearch.timer.restart();
            return;
        }

        String key = textSearch.getText();

        if(key.equals(previousTextSearch))
            return;

        if(key.trim().isEmpty()){
            resetTextSearch();
            return;
        }

        int[] array = textSearchResult != null && 
                key.contains(previousTextSearch) ? textSearchResult : mangaOnDisplayBackup;    

        previousTextSearch = TEXT_SEARCH_KEY = key;

        textSearchResult = new int[array.length];


        int index = 0;
        for (int i : array) {
            if(mangaNames[i].contains(key) || mangaDescriptions[i].contains(key))
                textSearchResult[index++] = i; 
        }

        textSearchResult = Arrays.copyOf(textSearchResult, index);
        setMangasOnDisplay();

    }

    private void searchTags(){
        if(tagsSearch == null || mangaOnDisplayBackup == null || mangaOnDisplayBackup.length == 0)
            return;


        while(!searchDataLoaded) {}

        HashSet<String> key = tagsSearch.currentAddedTags;

        boolean containsAll = key.containsAll(previousTagsSearch);

        if(containsAll && previousTagsSearch.size() == key.size())
            return;

        if(key.isEmpty()){
            TAGS_SEARCH_KEYS = null;
            tagsSearchResult = null;
            previousTagsSearch.clear();
            setMangasOnDisplay();
            return;
        }

        tagsSearchResult = tagsSearchResult != null && 
                containsAll ? tagsSearchResult : Arrays.copyOf(mangaOnDisplayBackup, mangaOnDisplayBackup.length);    

        previousTagsSearch.addAll(key);

        Map<Boolean, List<String>> map = key.stream().collect(Collectors.groupingBy(s -> s.matches("\\.\\d+\\.")));

        List<String> tagsList = map.get(false);
        if(tagsList != null && !tagsList.isEmpty()){
            for (int i = 0; i < tagsSearchResult.length; i++) {
                for (String s : tagsList) {
                    if(tagsSearchResult[i] == -1)
                        break;

                    switch (s) {
                    case "On Going":
                        tagsSearchResult[i] = Arrays.binarySearch(completedStatusArrayIndices, tagsSearchResult[i]) < 0 ? tagsSearchResult[i] : -1; 
                        break;
                    case "Completed":
                        tagsSearchResult[i] = Arrays.binarySearch(completedStatusArrayIndices, tagsSearchResult[i]) >= 0 ? tagsSearchResult[i] : -1;
                        break;
                    case "Read = 0":
                        tagsSearchResult[i] = Arrays.binarySearch(readCountZeroArrayIndices, tagsSearchResult[i]) >= 0 ? tagsSearchResult[i] : -1;
                        break;
                    case "Read != 0":
                        tagsSearchResult[i] = Arrays.binarySearch(readCountZeroArrayIndices, tagsSearchResult[i]) < 0 ? tagsSearchResult[i] : -1;
                        break;
                    case "Unread = 0":
                        tagsSearchResult[i] = Arrays.binarySearch(unreadCountZeroArrayIndices, tagsSearchResult[i]) >= 0 ? tagsSearchResult[i] : -1;
                        break;
                    case "Unread != 0":
                        tagsSearchResult[i] = Arrays.binarySearch(unreadCountZeroArrayIndices, tagsSearchResult[i]) < 0 ? tagsSearchResult[i] : -1;
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        tagsList = map.get(true);
        if(tagsList != null && !tagsList.isEmpty()){
            for (int k = 0; k < tagsSearchResult.length; k++) {
                if(tagsSearchResult[k] == -1)
                    continue;

                String tag = tagsData[tagsSearchResult[k]];

                int j = 0;
                boolean contains  = tag.contains(tagsList.get(j++));

                while(contains && j < tagsList.size()) contains = tag.contains(tagsList.get(j++));

                if(!contains)
                    tagsSearchResult[k] = -1;
            }
        }

        int[] array =  new int[tagsSearchResult.length];

        int index = 0;
        for (int i = 0; i < array.length; i++) {
            if(tagsSearchResult[i] != -1)
                array[index++] = tagsSearchResult[i]; 
        }

        tagsSearchResult = Arrays.copyOf(array, index);

        setMangasOnDisplay();

    }

    private void setMangasOnDisplay() {
        if(haltSetMangaOnDisplay)
            return;

        if(mangaOnDisplayBackup == null){
            logger.error("mangaOnDisplayBackup == null in setMangasOnDisplay()");
            return;
        }

        if(textSearchResult == null && tagsSearchResult == null)
            mangaManeger.setMangasOnDisplay(mangaOnDisplayBackup);
        else if(textSearchResult == null)
            mangaManeger.setMangasOnDisplay(tagsSearchResult);
        else if(tagsSearchResult == null)
            mangaManeger.setMangasOnDisplay(textSearchResult);
        else{
            if(tagsSearchResult.length == 0 &&  textSearchResult.length == 0){
                mangaManeger.setMangasOnDisplay(tagsSearchResult);
                return;
            }

            boolean bool = tagsSearchResult.length <= textSearchResult.length;
            int[] small = bool ? tagsSearchResult : textSearchResult;
            int[] large = bool ? textSearchResult : tagsSearchResult;


            int[] a = Arrays.copyOf(small, small.length);
            Arrays.sort(a);

            int[] b = new int[a.length];

            int index = 0;
            for (int i = 0; i < large.length && index < b.length; i++) {
                if(Arrays.binarySearch(a, large[i]) >= 0)
                    b[index++] = large[i];
            }


            mangaManeger.setMangasOnDisplay(index == b.length ? b : Arrays.copyOf(b, index));
        }
    }

    private void makeMangaOnDisplayBackup() {
        if(mangaOnDisplayBackup == null){

            mangaOnDisplayBackup = mangaManeger.getMangasOnDisplay();
            sortingMethodBackup = mangaManeger.getCurrentSortingMethod();

        }
    }

    private volatile boolean searchDataLoaded = false;
    private volatile boolean searchDataLoading = false;
    /**
     * @param loadTextSearchData if true loads {@link #mangaNames}, {@link #mangaDescriptions}, other wise {@link #tagsData}
     */
    private void loadData() {
        if(searchDataLoaded || searchDataLoading)
            return;

        searchDataLoading = true;

        new Thread(() -> {

            mangaNames = new String[mangaManeger.getMangasCount()];
            for (int i = 0; i < mangaNames.length; i++) mangaNames[i] = mangaManeger.getManga(i).getMangaName().toLowerCase();

            try {
            	mangaManeger.connection()
                .executeQuery(QueryMaker.getInstance().select(MANGA_ID, DESCRIPTION, CATEGORIES, READ_COUNT, UNREAD_COUNT, STATUS).from(TABLE_NAME).build(), rs -> {
                    int count = mangaManeger.getMangasCount();
                    mangaDescriptions = new String[count];
                    tagsData = new String[count];
                    readCountZeroArrayIndices = new int[count];
                    unreadCountZeroArrayIndices = new int[count];
                    completedStatusArrayIndices = new int[count];

                    int i = 0,//read_count 
                            j = 0,//unread_count 
                            k = 0;//status
                    
                    IntArray mangaIds =  mangaManeger.getMangaIds();

                    while(rs.next()){
                        int index = mangaIds.indexOf(rs.getInt("manga_id"));
                        String des = rs.getString("description");
                        String tag = rs.getString("categories");

                        mangaDescriptions[index] = des == null ? "" : des.toLowerCase();
                        tagsData[index] = tag == null ? "" : tag;

                        if(rs.getInt("read_count") == 0)
                            readCountZeroArrayIndices[i++] = index;
                        if(rs.getInt("unread_count") == 0)
                            unreadCountZeroArrayIndices[j++] = index;
                        if(rs.getBoolean("status"))
                            completedStatusArrayIndices[k++] = index;
                    }

                    readCountZeroArrayIndices  = Arrays.copyOf(readCountZeroArrayIndices , i);
                    unreadCountZeroArrayIndices  = Arrays.copyOf(unreadCountZeroArrayIndices , j);
                    completedStatusArrayIndices  = Arrays.copyOf(completedStatusArrayIndices , k);

                    Arrays.sort(readCountZeroArrayIndices );
                    Arrays.sort(unreadCountZeroArrayIndices );
                    Arrays.sort(completedStatusArrayIndices );
                    
                    return null;
                });
            } catch (SQLException  e2) {
                logger.error("Error while extracting search data from datbase : ",e2);
                System.exit(0);
            }
            searchDataLoaded = true;
            searchDataLoading = false;
        }).start();

    }

    //	TEXT SEARCH---------------------------------------------------------------------------------------------------------------

    private final class TextSearch extends JTextField{
        private static final long serialVersionUID = -3058846626098012925L;
        final Timer timer;

        private TextSearch() {
            super(previousTextSearch == null ? "" : previousTextSearch);
            setDoubleBuffered(false);
            setToolTipText(RH.getString("searchmaneger.textsearch.tooltip"));
            setAlignmentY(Component.TOP_ALIGNMENT);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setFont(RH.getFont("searchmaneger.textsearch.font"));

            timer = new Timer(RH.getInt("searchmaneger.textsearch.delay"), e -> searchText());
            timer.setRepeats(false);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {timer.restart();}
            });
        }
    }

    //	TAGS SEARCH---------------------------------------------------------------------------------------------------------------

    private final class TagsSearch {
        final JDialog tagsContainer;

        final HashSet<String> currentAddedTags;

        private TagsSearch() {
            currentAddedTags = previousTagsSearch == null ? new HashSet<>() : new HashSet<>(previousTagsSearch);

            tagsContainer = new JDialog(null, "Tags Search", ModalityType.APPLICATION_MODAL);
            tagsContainer.setResizable(false);

            JPanel mainPanel = new JPanel(false);

            BoxLayout boxl = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
            mainPanel.setLayout(boxl);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            mainPanel.setBackground(RH.getColor("searchmaneger.tagssearch.background"));
            mainPanel.setOpaque(true);

            Font tagsFont = RH.getFont("searchmaneger.tagssearch.font");
            Color tagSelectedBackground = RH.getColor("searchmaneger.tagssearch.selected.background");
            Color tagSelectedForeground = RH.getColor("searchmaneger.tagssearch.selected.foreground");
            Color tagUnselectedForeground = RH.getColor("searchmaneger.tagssearch.unselected.foreground");

            JPanel ptags = Utils.createJPanel(new GridLayout(0, RH.getInt("searchmaneger.tagssearch.tagsperline"),5,2));

            ItemListener itemListener = e -> {
                if(e.getStateChange() == ItemEvent.SELECTED){
                    JToggleButton cc = (JToggleButton)e.getItem();
                    cc.setForeground(tagSelectedForeground);
                    cc.setOpaque(true);
                    String code = cc.getName();

                    if(code != null)
                        currentAddedTags.add(code);
                }
                else if(e.getStateChange() == ItemEvent.DESELECTED){
                    JToggleButton cc = (JToggleButton)e.getItem();
                    cc.setForeground(tagUnselectedForeground);
                    cc.setOpaque(false);
                    String code = cc.getName();
                    if(code != null)
                        currentAddedTags.remove(code);
                }
            };

            JCheckBox[] temp = new JCheckBox[mangaManeger.TAG_MAX_ID - mangaManeger.TAG_MIN_ID];

            String format = ".%d.";

            int z = 0;
            for (int i = mangaManeger.TAG_MIN_ID; i <= mangaManeger.TAG_MAX_ID; i++) {
                String tag = mangaManeger.getTag(i);

                if(tag == null)
                    continue;

                JCheckBox c =  new JCheckBox(tag);
                c.setName(String.format(format, i));
                c.setDoubleBuffered(false);
                c.setBackground(tagSelectedBackground);
                c.setOpaque(false);
                c.setFont(tagsFont);
                c.addItemListener(itemListener);
                ptags.add(c);
                temp[z++] = c;

                if(currentAddedTags.contains(c.getName()))
                    c.doClick();
            }

            JCheckBox[] tagsCheckBoxes =  Arrays.copyOf(temp, z); 


            String[][] str = {
                    {"On Going", "Completed", null},
                    {"Read = 0", "Read != 0", null},
                    {"Unread = 0", "Unread != 0", null}
            };

            JPanel pStatus = Utils.createJPanel(new GridLayout(1, 3,2,2));
            JPanel pread_unread = Utils.createJPanel(new GridLayout(1, 2,2,2));
            JPanel pRead = Utils.createJPanel(new GridLayout(3, 1,2,2));
            JPanel pUnread = Utils.createJPanel(new GridLayout(3, 1,2,2));

            JPanel[] panels = {pStatus, pRead, pUnread};

            int i = 0,//panels[i] 
                    j = 0, //tagsRadioButtons[j]
                    k = 0; //nullNameRadioButtons[k]

            JRadioButton[] tagsRadioButtons = new JRadioButton[9];
            //those radiobuttons j, where j.getName() == null;
            JRadioButton[] nullNameRadioButtons = new JRadioButton[3];

            for (String[] s2 : str) {
                ButtonGroup bg = new ButtonGroup();
                boolean found = false;
                for (String s : s2) {
                    JRadioButton rb = new JRadioButton(s == null ? "All" : s);	
                    rb.setName(s);
                    rb.setDoubleBuffered(false);
                    rb.setBackground(tagSelectedBackground);
                    rb.setOpaque(false);
                    rb.setFont(tagsFont);
                    rb.addItemListener(itemListener);

                    bg.add(rb);
                    panels[i].add(rb);
                    tagsRadioButtons[j++] = rb;
                    if(s == null)
                        nullNameRadioButtons[k++] = rb;

                    if(!found){
                        found = s == null ? true : currentAddedTags.contains(s);

                        if(found)
                            rb.doClick();
                    }
                }
                i++;
            }

            pread_unread.add(pRead);
            pread_unread.add(pUnread);

            Color titleBorderColor = RH.getColor("searchmaneger.tagssearch.titled.border.color");
            Color titleforeground = RH.getColor("searchmaneger.tagssearch.titled.border.foreground");

            ptags.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(titleBorderColor), "Tags", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
            pStatus.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(titleBorderColor), "Status", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
            pread_unread.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(titleBorderColor), "Read/Unread Count", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
            pRead.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(titleBorderColor), "Read Count", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
            pUnread.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(titleBorderColor), "Unread Count", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), BorderFactory.createEmptyBorder(20, 20, 20, 20)));

            mainPanel.add(ptags);
            mainPanel.add(pStatus);
            mainPanel.add(pread_unread);

            JButton ok = new JButton("OK");
            JButton clear = new JButton("Clear");
            ok.setDoubleBuffered(false);
            clear.setDoubleBuffered(false);

            JPanel p = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));
            p.add(ok);
            p.add(Box.createHorizontalStrut(10));
            p.add(clear);			
            mainPanel.add(p, BorderLayout.SOUTH);


            boolean[] clearedTags = {false};

            clear.addActionListener(e -> {
                clearedTags[0] = true;
                for (JRadioButton r : nullNameRadioButtons) r.doClick();
                for (JCheckBox c : tagsCheckBoxes) c.setSelected(false);
                currentAddedTags.clear();

            });

            ok.addActionListener(e-> {
                clearedTags[0] = false;
                searchTags(); 
                tagsContainer.setVisible(false);
            });

            tagsContainer.add(mainPanel);
            tagsContainer.pack();
            tagsContainer.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if(clearedTags[0]){
                        for (JRadioButton r : tagsRadioButtons) if(previousTagsSearch.contains(r.getName())) r.doClick();
                        for (JCheckBox c : tagsCheckBoxes) if(previousTagsSearch.contains(c.getName())) c.setSelected(true);
                    }
                }
            });

        }
        public void show() {
            EventQueue.invokeLater(() -> tagsContainer.setVisible(true));
        }

    }


    public void dispose() {
        if(tagsSearch != null)
            tagsSearch.tagsContainer.dispose();

    }

}
