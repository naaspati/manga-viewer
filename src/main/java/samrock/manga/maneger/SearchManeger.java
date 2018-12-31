package samrock.manga.maneger;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createLineBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static sam.manga.samrock.mangas.MangasMeta.CATEGORIES;
import static sam.manga.samrock.mangas.MangasMeta.DESCRIPTION;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.READ_COUNT;
import static sam.manga.samrock.mangas.MangasMeta.STATUS;
import static sam.manga.samrock.mangas.MangasMeta.TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.UNREAD_COUNT;
import static samrock.manga.maneger.Status.COMPLETED;
import static samrock.manga.maneger.Status.ONGOING;
import static samrock.manga.maneger.Status.READ_0;
import static samrock.manga.maneger.Status.READ_ALL;
import static samrock.manga.maneger.Status.READ_NOT_0;
import static samrock.manga.maneger.Status.STATUS_ALL;
import static samrock.manga.maneger.Status.UNREAD_0;
import static samrock.manga.maneger.Status.UNREAD_ALL;
import static samrock.manga.maneger.Status.UNREAD_NOT_0;

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
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import sam.collection.IntSet;
import sam.logging.MyLoggerFactory;
import sam.manga.samrock.mangas.MangaUtils;
import sam.sql.querymaker.QueryMaker;
import samrock.utils.IntArray;
import samrock.utils.RH;
import samrock.utils.SortingMethod;
import samrock.utils.Utils;

public final class SearchManeger implements ChangeListener<Mangas, MangaManegerStatus> { 
	private static Logger logger = MyLoggerFactory.logger(SearchManeger.class);

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
		previousTagStatus = null;

		textSearchResult = null;
		tagsSearchResult = null;

		sortingMethodBackup = null;

		mangasOnDisplayBackup = null;
	}

	/**
	 * use softreference
	 */
	@Deprecated
	public void fullClear() {
		mangasOnDisplay.removeChangeListener(this);

		basicClear();

		mangaNames = null;
		mangaDescriptions = null;

		tagsData = null;
		statusMap.clear();
		searchDataLoaded = false;
		searchDataLoading = false;
	}

	/**
	 * @return true if instance can be set null, (there is no active search) 
	 * otherwise use wakeUp() to restore SearchManeger to previous state
	 */
	@Deprecated
	public boolean hibernate(){
		if((mangasOnDisplayBackup == null || mangasOnDisplayBackup.length == mangasOnDisplay.length()) ||  
				(textSearch == null && tagsSearch == null) ||
				((previousTextSearch == null || previousTextSearch.isEmpty()) && previousTagStatus.isEmpty())
				){
			fullClear();
			return true;
		}
		else {
			String text = previousTextSearch;
			TagStatus tags = previousTagStatus;
			int[] b = mangasOnDisplayBackup;
			SortingMethod s = sortingMethodBackup;

			fullClear();

			previousTagStatus = TAGS_SEARCH_KEYS = tags;
			previousTextSearch = TEXT_SEARCH_KEY = text;
			mangasOnDisplayBackup = b;
			sortingMethodBackup = s;
			return false;
		}
	} 


	@Deprecated
	public boolean[] wakeUp(){
		boolean[] b = new boolean[2];

		if(mangasOnDisplayBackup == null)
			return b;
		if(previousTextSearch != null && !previousTextSearch.isEmpty()){
			textSearch = new TextSearch();
			b[0] = true;
		}

		if(previousTagStatus != null && !previousTagStatus.isEmpty()){
			tagsSearch = new TagsSearch();
			b[1] = true;
		}

		if(b[0] || b[1]){
			mangasOnDisplay.addChangeListener(this);
			loadData();
		}
		return b;
	}


	private final MangaManeger mangaManeger;

	private String TEXT_SEARCH_KEY;
	private TextSearch textSearch;
	/**last text search*/
	private String previousTextSearch = null;
	private String[] mangaNames;
	private String[] mangaDescriptions;

	private TagStatus TAGS_SEARCH_KEYS;
	private TagsSearch tagsSearch;
	/**last tagsSearch*/
	TagStatus previousTagStatus;
	private int[][] tagsData;
	private EnumMap<Status, BitSet> statusMap; 

	private final Mangas mangasOnDisplay;
	/**
	 * do not modify this (internally)
	 * this can be passed mangamanger
	 */
	private int[] mangasOnDisplayBackup;
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
		mangasOnDisplay = MangaManeger.mangas();

		mangasOnDisplay.addChangeListener(this);
	}

	@Override
	public void changed(Mangas e, MangaManegerStatus code) {
		if(mangasOnDisplayBackup == null)
			return;

		if(code == MangaManegerStatus.MOD_MODIFIED_INTERNALLY){
			mangasOnDisplayBackup = mangasOnDisplay.getCopy();
			tagsSearchResult = null;
			textSearchResult = null;
			previousTagStatus.clear();
			previousTextSearch = "";
			haltSetMangaOnDisplay = true;
			searchTags();
			searchText();
			haltSetMangaOnDisplay = false;
			setMangasOnDisplay();

		}
		else if(sortingMethodBackup != e.getSorting()){
			mangasOnDisplayBackup = e.sortArray(mangasOnDisplayBackup);
			textSearchResult = textSearchResult == null || textSearchResult.length < 2 ? textSearchResult : e.sortArray(textSearchResult);
			tagsSearchResult = tagsSearchResult == null || tagsSearchResult.length < 2 ? tagsSearchResult : e.sortArray(tagsSearchResult);

			sortingMethodBackup = e.getSorting();
		}
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

			previousTagStatus = new TagStatus() ;
			TAGS_SEARCH_KEYS = previousTagStatus;
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
		if(textSearch == null || mangasOnDisplayBackup == null || mangasOnDisplayBackup.length == 0)
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
				key.contains(previousTextSearch) ? textSearchResult : mangasOnDisplayBackup;    

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
		if(tagsSearch == null || mangasOnDisplayBackup == null || mangasOnDisplayBackup.length == 0)
			return;


		while(!searchDataLoaded) {}

		TagStatus key = tagsSearch.tagStatus;
		boolean containsAll = key.containsAll(previousTagStatus);

		if(containsAll && previousTagStatus.size() == key.size())
			return;

		if(key.isEmpty()){
			TAGS_SEARCH_KEYS = null;
			tagsSearchResult = null;
			previousTagStatus.clear();
			setMangasOnDisplay();
			return;
		}

		tagsSearchResult = tagsSearchResult != null && 
				containsAll ? tagsSearchResult : Arrays.copyOf(mangasOnDisplayBackup, mangasOnDisplayBackup.length);    

		previousTagStatus.addAll(key);
		key.process(tagsSearchResult);

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

		if(mangasOnDisplayBackup == null){
			logger.log(Level.SEVERE, "mangaOnDisplayBackup == null in setMangasOnDisplay()");
			return;
		}


		if(textSearchResult == null && tagsSearchResult == null)
			mangasOnDisplay.set(mangasOnDisplayBackup);
		else if(textSearchResult == null)
			mangasOnDisplay.set(tagsSearchResult);
		else if(tagsSearchResult == null)
			mangasOnDisplay.set(textSearchResult);
		else{
			if(tagsSearchResult.length == 0 &&  textSearchResult.length == 0){
				mangasOnDisplay.set(tagsSearchResult);
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
			mangasOnDisplay.set(index == b.length ? b : Arrays.copyOf(b, index));
		}
	}

	private void makeMangaOnDisplayBackup() {
		if(mangasOnDisplayBackup == null){

			mangasOnDisplayBackup = mangasOnDisplay.getCopy();
			sortingMethodBackup = mangasOnDisplay.getSorting();
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

			mangaNames = new String[MangaManeger.getMangasCount()];
			for (int i = 0; i < mangaNames.length; i++) mangaNames[i] = MangaManeger.getManga(i).getMangaName().toLowerCase();

			try {
				//TODO

				MangaManeger.samrock()
				.executeQuery(QueryMaker.getInstance().select(MANGA_ID, DESCRIPTION, CATEGORIES, READ_COUNT, UNREAD_COUNT, STATUS).from(TABLE_NAME).build(), 
						rs -> {
							IntArray mangaIds =  MangaManeger.getMangaIds();
							int count = mangaIds.length();

							mangaDescriptions = new String[count];
							tagsData = new int[count][];
							BitSet read_0 = new BitSet(count);
							BitSet unread_0 = new BitSet(count);
							BitSet completed = new BitSet(count);

							statusMap = new EnumMap<>(Status.class);
							statusMap.put(READ_0, read_0);
							statusMap.put(UNREAD_0, unread_0);
							statusMap.put(COMPLETED, completed);

							while(rs.next()){
								int index = mangaIds.indexOfMangaId(rs.getInt("manga_id"));
								String des = rs.getString("description");
								String tag = rs.getString("categories");

								mangaDescriptions[index] = des == null ? "" : des.toLowerCase();
								tagsData[index] = tag == null ? new int[0] : MangaUtils.tagsToIntArray(tag);

								if(rs.getInt("read_count") == 0)
									read_0.set(index);
								if(rs.getInt("unread_count") == 0)
									unread_0.set(index);
								if(rs.getBoolean("status"))
									completed.set(index);
							}
							return null;
						});
			} catch (SQLException  e2) {
				logger.log(Level.SEVERE, "Error while extracting search data from datbase : ",e2);
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

	@SuppressWarnings("serial")
	private class JCheckBox2 extends JCheckBox {
		private int tagId;

		public JCheckBox2(String tagName, int tagId) {
			super(tagName);
			this.tagId = tagId;

			setDoubleBuffered(false);
			setBackground(tagSelectedBackground);
			setOpaque(false);
			setFont(tagsFont);
			addItemListener(tagsSearch);

			if(tagsSearch.tagStatus.contains(tagId))
				doClick();
		}
	}

	private static final Font tagsFont = RH.getFont("searchmaneger.tagssearch.font");
	private static final Color tagSelectedBackground = RH.getColor("searchmaneger.tagssearch.selected.background");
	private static final Color tagSelectedForeground = RH.getColor("searchmaneger.tagssearch.selected.foreground");
	private static final Color tagUnselectedForeground = RH.getColor("searchmaneger.tagssearch.unselected.foreground");

	@SuppressWarnings("serial")
	private class JRadioButton2 extends JRadioButton{
		private final Status status;

		public JRadioButton2(Status status) {
			super(status.text);
			this.status = status;

			setDoubleBuffered(false);
			setBackground(tagSelectedBackground);
			setOpaque(false);
			setFont(tagsFont);
			addItemListener(tagsSearch);

			if(tagsSearch.tagStatus.contains(status))
				doClick();
		}
	}

	public class TagStatus {
		final IntSet tags;
		final EnumSet<Status> statuses;

		public TagStatus() {
			this.tags = new IntSet();
			this.statuses = EnumSet.noneOf(Status.class);
		}
		public void process(int[] tagsSearchResult) {
			processWithStatus(tagsSearchResult);
			processWithTags(tagsSearchResult);
		}
		private void processWithTags(final int[] result) {
			if(tags.isEmpty()) return;

			for (Integer tagid : tags) {
				int count = 0;
				for (int i = 0; i < result.length; i++) {
					if(result[i] < 0) {
						count++;
						continue;
					}
					if(Arrays.binarySearch(tagsData[result[i]], tagid) < 0) {
						result[i] = -1;
						count++;
					}
				}
				if(count == result.length)
					break;
			}
		}
		private void processWithStatus(final int[] result) {
			if(statuses.isEmpty()) return;

			BitSet read_0 = statusMap.get(READ_0);
			BitSet unread_0 = statusMap.get(UNREAD_0);
			BitSet completed = statusMap.get(COMPLETED);

			for (int i = 0; i < result.length; i++) {
				for (Status s : statuses) {
					if(result[i] == -1)
						break;

					switch (s) {
					case ONGOING:
						if(completed.get(result[i]))
							result[i] =  -1;
						break;
					case COMPLETED:
						if(!completed.get(result[i]))
							result[i] =  -1;
						break;
					case READ_0:
						if(!read_0.get(result[i]))
							result[i] =  -1;
					case READ_NOT_0:
						if(read_0.get(result[i]))
							result[i] =  -1;
					case UNREAD_0:
						if(!unread_0.get(result[i]))
							result[i] =  -1;
					case UNREAD_NOT_0:
						if(unread_0.get(result[i]))
							result[i] =  -1;
					default:
						break;
					}
				}
			}
		}
		public Object size() {
			return tags.size() + statuses.size();
		}
		public void addAll(TagStatus pt) {
			tags.addAll(pt.tags);
			statuses.addAll(pt.statuses);
		}
		public boolean isEmpty() {
			return tags.isEmpty()
					&& statuses.isEmpty();
		}
		public boolean containsAll(TagStatus pt) {
			return tags.containsAll(pt.tags)
					&& statuses.containsAll(pt.statuses);
		}
		public void add(JToggleButton cc) {
			if(cc instanceof JRadioButton2)
				statuses.add(((JRadioButton2)cc).status);
			else
				tags.add(((JCheckBox2)cc).tagId);
		}
		public void add(int tagId) {
			tags.add(tagId);
		}
		public void add(Status status) {
			statuses.add(status);
		}
		public void remove(JToggleButton cc) {
			if(cc instanceof JRadioButton2)
				statuses.remove(((JRadioButton2)cc).status);
			else
				tags.remove(((JCheckBox2)cc).tagId);
		}
		public void remove(int tagId) {
			tags.remove(tagId);
		}
		public void remove(Status status) {
			statuses.remove(status);
		}
		public boolean contains(JToggleButton cc) {
			if(cc instanceof JRadioButton2)
				return statuses.remove(((JRadioButton2)cc).status);
			else
				return tags.remove(((JCheckBox2)cc).tagId);
		}
		public boolean contains(int tagId) {
			return tags.remove(tagId);
		}
		public boolean contains(Status status) {
			return statuses.remove(status);
		}
		public void clear() {
			tags.clear();
			statuses.clear();
		}
		public IntSet getTags() {
			return tags;
		}
	}

	private final class TagsSearch implements ItemListener {
		final JDialog tagsContainer;
		final TagStatus tagStatus;

		private TagsSearch() {
			tagStatus = previousTagStatus != null ? previousTagStatus : new TagStatus();

			tagsContainer = new JDialog(null, "Tags Search", ModalityType.APPLICATION_MODAL);
			tagsContainer.setResizable(false);

			JPanel mainPanel = new JPanel(false);

			BoxLayout boxl = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
			mainPanel.setLayout(boxl);
			mainPanel.setBorder(createEmptyBorder(10, 10, 10, 10));
			mainPanel.setBackground(RH.getColor("searchmaneger.tagssearch.background"));
			mainPanel.setOpaque(true);

			JPanel ptags = Utils.createJPanel(new GridLayout(0, RH.getInt("searchmaneger.tagssearch.tagsperline"),5,2));
			TagsDAO dao = MangaManeger.tagsDao();

			List<JCheckBox2> tagsCheckBoxes = IntStream.rangeClosed(dao.min, dao.max)
					.mapToObj(i -> new JCheckBox2(dao.getTag(i), i))
					.filter(j -> j.getText() != null)
					.sorted(Comparator.comparing(JCheckBox::getText))
					.peek(ptags::add)
					.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

			EnumMap<Status, JRadioButton2> statusButtons = Arrays
					.stream(Status.values())
					.map(JRadioButton2::new)
					.collect(Collectors.toMap(j -> j.status, j -> j, (p,n) -> n, () -> new EnumMap<>(Status.class)));

			JPanel pread_unread = Utils.createJPanel(new GridLayout(1, 2,2,2));
			JPanel pStatus = create(statusButtons, ONGOING, COMPLETED, STATUS_ALL);
			JPanel pRead = create(statusButtons, READ_0, READ_NOT_0, READ_ALL);
			JPanel pUnread = create(statusButtons, UNREAD_0, UNREAD_NOT_0, UNREAD_ALL);

			pread_unread.add(pRead);
			pread_unread.add(pUnread);

			Color titleBorderColor = RH.getColor("searchmaneger.tagssearch.titled.border.color");
			Color titleforeground = RH.getColor("searchmaneger.tagssearch.titled.border.foreground");

			ptags.setBorder(createCompoundBorder(createTitledBorder(createLineBorder(titleBorderColor), "Tags", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), createEmptyBorder(20, 20, 20, 20)));
			pStatus.setBorder(createCompoundBorder(createTitledBorder(createLineBorder(titleBorderColor), "Status", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), createEmptyBorder(20, 20, 20, 20)));
			pread_unread.setBorder(createCompoundBorder(createTitledBorder(createLineBorder(titleBorderColor), "Read/Unread Count", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), createEmptyBorder(20, 20, 20, 20)));
			pRead.setBorder(createCompoundBorder(createTitledBorder(createLineBorder(titleBorderColor), "Read Count", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), createEmptyBorder(20, 20, 20, 20)));
			pUnread.setBorder(createCompoundBorder(createTitledBorder(createLineBorder(titleBorderColor), "Unread Count", TitledBorder.CENTER, TitledBorder.TOP, tagsFont, titleforeground), createEmptyBorder(20, 20, 20, 20)));

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
				statusButtons.get(STATUS_ALL).doClick();
				statusButtons.get(READ_ALL).doClick();
				statusButtons.get(UNREAD_ALL).doClick();

				for (JCheckBox c : tagsCheckBoxes) c.setSelected(false);
				tagStatus.clear();
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
						statusButtons.forEach((s,j) -> {
							if(previousTagStatus.contains(s))
								j.doClick();
						});
						for (JCheckBox2 c : tagsCheckBoxes) {
							if(previousTagStatus.contains(c.tagId)) 
								c.setSelected(true);
						} 
					}
				}
			});

		}
		private JPanel create(EnumMap<Status, JRadioButton2> map, Status...s) {
			JPanel panel =  Utils.createJPanel(new GridLayout(s.length, 1,2,2));
			ButtonGroup grp = new ButtonGroup();

			for (Status ss : s) {
				panel.add(map.get(ss));
				grp.add(map.get(ss));
			}

			return panel;
		}
		public void show() {
			EventQueue.invokeLater(() -> tagsContainer.setVisible(true));
		}
		@Override
		public void itemStateChanged(ItemEvent e) {
			if(e.getStateChange() == ItemEvent.SELECTED){
				JToggleButton cc = (JToggleButton)e.getItem();
				cc.setForeground(tagSelectedForeground);
				cc.setOpaque(true);

				tagStatus.add(cc);
			}
			else if(e.getStateChange() == ItemEvent.DESELECTED){
				JToggleButton cc = (JToggleButton)e.getItem();
				cc.setForeground(tagUnselectedForeground);
				cc.setOpaque(false);

				tagStatus.remove(cc);
			}
		}
	}

	public void dispose() {
		if(tagsSearch != null)
			tagsSearch.tagsContainer.dispose();

	}

	public Set<String> activeTagSearch() {
		return TAGS_SEARCH_KEYS == null ? Collections.emptySet() : TAGS_SEARCH_KEYS.getTags();
	}
	public String activeTextSearch() {
		return TEXT_SEARCH_KEY;
	}
}
