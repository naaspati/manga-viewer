package samrock.manga;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.sqlite.JDBC;

import samrock.utils.RH;
import samrock.utils.SortingMethod;
import samrock.utils.Utils;
import samrock.utils.ViewElementType;

public final class MangaManeger {
	/**
	 * MOD = MANGAS_ON_DISPLAY
	 */
	public static final int MOD_MODIFIED = 0x700;
	/**
	 * MOD = MANGAS_ON_DISPLAY<br>
	 * When MangaManeger changes MOD by Itself<br>
	 * this variable is helpful for search maneger<br>
	 * e.g. when mangas are added or removed from delete queue, or favorite list,
	 * it signals search maneger of change is mod backup and redo search (if needed)      
	 */
	public static final int MOD_MODIFIED_INTERNALLY = 0x702;
	/**
	 * DQ = DELETE_QUEUE
	 */
	public static final int DQ_UPDATED = 0x703;

	private static MangaManeger instance;
	public synchronized static MangaManeger getInstance() { return instance; }
	/**
	 * used by mainMethod
	 * @throws SQLException
	 */
	public synchronized static void createInstance() throws SQLException {
		instance = new MangaManeger();
	}


	private final MinimalManga[] mangas; //contains mangas sorted by manga_id
	private final int[] mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id  
	private MinimalChapterSavePoint[] recents;

	public final int TAG_MIN_ID;
	public final int TAG_MAX_ID;
	private final String[] tagsArray;
	/**
	 * Array Indices of mangas currently showing on display
	 */
	private int[] mangasOnDisplay;
	private Manga currentManga;
	private ChapterSavePoint currentSavePoint;
	private final String DATABASE_CONNECTION_STRING;
	private final String[] thumbFolder; //
	private final String[][] thumbFolderListed;
	private SortingMethod currentSortingMethod = null;

	private MangaManeger() throws SQLException {
		
		DATABASE_CONNECTION_STRING = JDBC.PREFIX.concat(RH.getString("manga.database.path"));

		try (Connection c = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
				Statement s1 = c.createStatement();
				ResultSet rsMangaCount = s1.executeQuery("SELECT COUNT(manga_id) AS mangaCount FROM MangaData");
				Statement s2 = c.createStatement();
				ResultSet rsManga = s2.executeQuery(RH.getStartupViewElementType() == ViewElementType.THUMB ? MinimalManga.SELECT_SQL : MinimalListManga.SELECT_SQL);
				Statement s3 = c.createStatement();
				ResultSet rsTagNumbers = s3.executeQuery("SELECT MAX(id) as _max, MIN(id) as _min, count(id) AS _count FROM Tags WHERE add_to_list = 1");
				Statement s4 = c.createStatement();
				ResultSet rsTags = s4.executeQuery("SELECT id, name FROM Tags WHERE add_to_list = 1");
				) {
			c.setAutoCommit(false);

			TAG_MIN_ID = rsTagNumbers.getInt("_min");
			TAG_MAX_ID = rsTagNumbers.getInt("_max");
			int tagsCount =  rsTagNumbers.getInt("_count");

			if(tagsCount + 50 < TAG_MAX_ID - TAG_MIN_ID)
				Utils.openErrorDialoag(null, "tagsCount ("+tagsCount+") + 50 < tagMaxId ("+TAG_MAX_ID+") - tagMinId("+TAG_MIN_ID+") = true",MangaManeger.class,99/*{LINE_NUMBER}*/, null);

			tagsArray = new String[TAG_MAX_ID - TAG_MIN_ID + 1];
			int size = rsMangaCount.getInt("mangaCount");
			mangas = new MinimalManga[size];
			mangaIds = new int[size];
			thumbFolder = new String[size];
			thumbFolderListed = new String[size][];

			//read tags/categories
			while (rsTags.next()) tagsArray[rsTags.getInt("id") - TAG_MIN_ID] = rsTags.getString("name");

			//read manga
			int index = 0;
			while (rsManga.next()) {
				mangas[index] = new MinimalManga(rsManga, index);
				mangaIds[index] = rsManga.getInt("manga_id");				
				index++;
			}

			String[] thumbs = RH.thumbFolder().list();

			for (int i = 0; i < thumbs.length; i++){
				if(thumbs[i].equals("desktop.ini"))
					continue;
					
				index = getArrayIndex(Integer.parseInt(thumbs[i].replaceFirst("(?:_\\d+)?\\.jpe?g$", "").trim()));
				if(index >= 0)
					thumbFolder[index] = thumbs[i];
			}
		}

		
		Utils.addExitTasks(() -> {
			mangaManegerWatchers.clear();
			loadManga(SELF_INITIATED_MANGA_UNLOAD);
			processDeleteQueue();
		});
	}

	public void loadAllMinimalListMangas() {
		if(Stream.of(mangas).allMatch(m -> m instanceof MinimalListManga))
			return;

		databaseQuery(MinimalListManga.SELECT_SQL, rs -> {
			int index = 0;
			try {
				while(rs.next()){
					if(getMangaId(index) == rs.getInt("manga_id"))
						mangas[index] = new MinimalListManga(rs, index);
					else{
						int index2 = getArrayIndex(rs.getInt("manga_id"));
						mangas[index2] = new MinimalListManga(rs, index2);
					}
					index++;
				}
			} catch (SQLException|ArrayIndexOutOfBoundsException e) {
				Utils.openErrorDialoag(null, "Error while loadAllMinimalListMangas, App Will Close",MangaManeger.class,155/*{LINE_NUMBER}*/, e);
				System.exit(0);
			}
		});

		
	}

	public SortingMethod getCurrentSortingMethod() {
		return currentSortingMethod;
	}

	/**
	 * 
	 * @param manga_id
	 * @return index of array where manga_id is stored 
	 */
	public int getArrayIndex(int manga_id) {
		return Arrays.binarySearch(mangaIds, manga_id);
	}
	/**
	 * 
	 * @param array_index
	 * @return manga_id is stored in this array_index 
	 */
	public int getMangaId(int array_index) {
		return mangaIds[array_index];
	}

	public String getTag(int tagId) {
		return tagId <= TAG_MAX_ID && tagId >= TAG_MIN_ID ? tagsArray[tagId - TAG_MIN_ID] : null;
	}

	/**
	 * other possible name -> loadAllRecents
	 */
	public void loadAllMinimalChapterSavePoints(){
		if (recents != null)
			return;
		

		try (Connection c = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
				Statement s1 = c.createStatement();
				ResultSet rs = s1.executeQuery(MinimalChapterSavePoint.SELECT_SQL);
				Statement st = c.createStatement();
				) {
			c.setAutoCommit(false);

			recents = new MinimalChapterSavePoint[mangas.length];

			while (rs.next()) {
				int id = rs.getInt("manga_id");
				int index = getArrayIndex(id);

				if(index >= 0)
					recents[index] = new MinimalChapterSavePoint(rs, index);
				else
					st.executeUpdate("DELETE FROM Recents WHERE manga_id = "+id);
			}	
			c.commit();
		} catch (SQLException e) {
			Utils.openErrorDialoag(null, "Error while loading all recents(), App Will Close",MangaManeger.class,216/*{LINE_NUMBER}*/, e);
			System.exit(0);
		}

		
	}

	public Manga getCurrentManga() {
		return currentManga;
	}

	public ChapterSavePoint getCurrentSavePoint() {
		return currentSavePoint;
	}

	public String parseTags(String tags) {
		Pattern p = Pattern.compile("\\.(\\d+)\\.");
		Matcher m = p.matcher(tags);

		String f = "<span bgcolor=red>$1</span>";

		StringBuffer b = new StringBuffer();

		while(m.find()){
			int index =Integer.parseInt(m.group(1));

			String tag = getTag(index);

			m.appendReplacement(b, tag == null ? f : Matcher.quoteReplacement(tag+", "));
		}

		return b.toString();

	}

	public synchronized void  databaseQuery(String query, Consumer<ResultSet> resultSetEater) {
		if(query == null || query.trim().isEmpty())
			throw new NullPointerException("Query cannot be "+(query == null ? "null" : "empty string"));

		try (Connection c = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
				Statement s1 = c.createStatement();
				ResultSet rs = s1.executeQuery(query)) {
			resultSetEater.accept(rs);	
		} catch (SQLException e) {
			Utils.openErrorDialoag(null, "Error while executing query:\r\n"+query+System.lineSeparator(),MangaManeger.class,260/*{LINE_NUMBER}*/, e);
		}
	}

	ArrayList<IntConsumer> mangaManegerWatchers = new ArrayList<>();
	/**
	 * 
	 * @param mangasOnDisplay
	 */
	public void setMangasOnDisplay(int[] mangasOnDisplay) {
		if(Arrays.equals(this.mangasOnDisplay, mangasOnDisplay))
			return;

		this.mangasOnDisplay = mangasOnDisplay;
		notifyWatchers(MOD_MODIFIED);
	}

	/**
	 * @param mangaManegerWatcher will notify the subscriber any changes in mangamaneger
	 */
	public void addMangaManegerWatcher(IntConsumer mangaManegerWatcher) {
		mangaManegerWatchers.add(mangaManegerWatcher);
	}

	/**
	 * @param mangaManegerWatcher will notify the subscriber any changes in mangamaneger
	 */
	public void removeMangaManegerWatcher(IntConsumer mangaManegerWatcher) {
		mangaManegerWatchers.remove(mangaManegerWatcher);
	}

	int randomNumber = 0;
	public String getRandomThumbPath(int arrayIndex) {
		if(thumbFolder[arrayIndex] == null)
			return null;

		if(thumbFolderListed[arrayIndex] == null){
			File file = new File(RH.thumbFolder(), thumbFolder[arrayIndex]);

			if(!file.exists()){
				thumbFolder[arrayIndex] = null;
				return null;
			}

			if(file.isFile()){
				thumbFolderListed[arrayIndex] = new String[]{thumbFolder[arrayIndex]};
				return thumbFolder[arrayIndex];
			}
			else{
				thumbFolderListed[arrayIndex] = file.list();
				if(thumbFolderListed[arrayIndex].length == 0){
					file.delete();
					thumbFolder[arrayIndex] = null;
					thumbFolderListed[arrayIndex] = null;
					return null;
				}
				else{
					String str = thumbFolder[arrayIndex].concat("/");
					for (int i = 0; i < thumbFolderListed[arrayIndex].length; i++) 
						thumbFolderListed[arrayIndex][i] = str.concat(thumbFolderListed[arrayIndex][i]);
				}
			}
		}

		else if(thumbFolderListed[arrayIndex].length == 0)
			return thumbFolder[arrayIndex];
		else if(thumbFolderListed[arrayIndex].length == 1)
			return thumbFolderListed[arrayIndex][0]; 

		return thumbFolderListed[arrayIndex][(randomNumber++)%thumbFolderListed[arrayIndex].length];
	}

	public String[] getThumbsPaths(int arrayIndex){
		getRandomThumbPath(arrayIndex);
		if(thumbFolderListed[arrayIndex] == null || thumbFolderListed[arrayIndex].length == 0)
			return null;

		return thumbFolderListed[arrayIndex];
	} 

	/**
	 * this method will reCheck icons for current manga
	 */
	public void reListIcons() {
		File file = new File(RH.thumbFolder(), thumbFolder[currentManga.ARRAY_INDEX]);

		if(!file.exists()){
			file = new File(RH.thumbFolder(), String.valueOf(currentManga.MANGA_ID));
			if(file.exists())
				thumbFolder[currentManga.ARRAY_INDEX]  = file.getName();
			else if((file = new File(RH.thumbFolder(), String.valueOf(currentManga.MANGA_ID)+".jpg" )).exists())
				thumbFolder[currentManga.ARRAY_INDEX]  = file.getName();
			else if((file = new File(RH.thumbFolder(), String.valueOf(currentManga.MANGA_ID)+"_0.jpg" )).exists())
				thumbFolder[currentManga.ARRAY_INDEX]  = file.getName();
		}

		thumbFolderListed[currentManga.ARRAY_INDEX] = null;
	}

	/**
	 * 
	 * @return mangas.length
	 */
	public int getMangasCount() {
		return mangas.length;
	}

	/**
	 * 
	 * @param arrayIndex 
	 * @return MinimalManga or MinimalListManga stored in this index in mangas array
	 */
	public MinimalManga getManga(int arrayIndex) {
		return mangas[arrayIndex];
	}


	private static final int SELF_INITIATED_MANGA_UNLOAD = 0x705*-1;
	private static final int LOAD_MOST_RECENT_MANGA = 0x706*-1;

	/**
	 * load corresponding manga, ChapterSavePoint and set to currentManga and currentSavePoint  
	 * @param arrayIndex
	 */
	public void loadManga(int arrayIndex) {
		
		if(arrayIndex >= 0 && currentManga != null && currentManga.ARRAY_INDEX == arrayIndex){
			
			return;
		}

		if(arrayIndex == LOAD_MOST_RECENT_MANGA && currentManga != null && currentManga.getLastReadTime() > Utils.START_UP_TIME){
			
			return;
		}

		try (Connection c = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
				Statement stmnt = c.createStatement();
				) {
			c.setAutoCommit(false);
			unloadCurrentManga(c, arrayIndex != SELF_INITIATED_MANGA_UNLOAD);

			if(arrayIndex  != SELF_INITIATED_MANGA_UNLOAD){
				String manga_id;
				if(arrayIndex == LOAD_MOST_RECENT_MANGA){
					ResultSet rs = stmnt.executeQuery("SELECT manga_id FROM Recents WHERE _time = (SELECT MAX(last_read_time) FROM MangaData)");
					manga_id = rs.getString("manga_id");
					arrayIndex = getArrayIndex(Integer.parseInt(manga_id)); 
					rs.close();
				}
				else
					manga_id = String.valueOf(getMangaId(arrayIndex));
				
				//old sql
				//"SELECT md.*,mu.mangafox FROM MangaData md INNER JOIN MangaUrls mu ON mu.manga_id =  md.manga_id WHERE md.manga_id =
				
				ResultSet rs = stmnt.executeQuery("SELECT mangafox FROM MangaUrls WHERE manga_id = ".concat(manga_id));
				String url = rs.next() ? rs.getString("mangafox") : null;
				rs.close();
				
				rs = stmnt.executeQuery("SELECT * FROM MangaData WHERE manga_id = ".concat(manga_id));
				
				currentManga = new Manga(rs, arrayIndex, url);
				mangas[arrayIndex] = currentManga;
				rs.close();

				rs = stmnt.executeQuery("SELECT * FROM Recents WHERE manga_id = ".concat(manga_id));

				if(rs.next())
					currentSavePoint = new ChapterSavePoint(rs, arrayIndex);
				else
					currentSavePoint = new ChapterSavePoint(currentManga);

				if(recents != null)
					recents[arrayIndex] = currentSavePoint;

				rs.close();
			}

		} catch (SQLException | IOException | ClassNotFoundException e) {
			Utils.openErrorDialoag(null, "error while loading "+
					(arrayIndex == LOAD_MOST_RECENT_MANGA ? " LOAD_MOST_RECENT_MANGA " 
							: arrayIndex == SELF_INITIATED_MANGA_UNLOAD ? " SELF_INITIATED_MANGA_UNLOAD " : mangas[arrayIndex].toString()),MangaManeger.class,434/*{LINE_NUMBER}*/, e);
		}
	}

	public void  loadMostRecentManga(){loadManga(LOAD_MOST_RECENT_MANGA);}

	private void unloadCurrentManga(Connection c, boolean notSelfInitiated) throws SQLException, IOException {
		if(currentManga == null)
			return;

		if(currentManga.isModified()){
			PreparedStatement pm = c.prepareStatement(Manga.UPDATE_SQL);
			currentManga.unload(pm);
			pm.executeBatch();
			pm.close();

			if(favorites != null && notSelfInitiated){
				int index = 0;
				boolean found = false;
				int arrayIndex = currentManga.ARRAY_INDEX;
				for (; index < favorites.length; index++)  if(found = favorites[index] == arrayIndex) break;

				if(currentManga.isFavorite()){
					if(!found){
						favorites = Arrays.copyOf(favorites, favorites.length + 1);
						index = favorites.length - 1;
					}

					for (; index > 0; index--) 
						favorites[index] = favorites[index - 1];

					favorites[0] = arrayIndex;
				}
				else if(found){
					for (; index < favorites.length - 1; index++) 
						favorites[index] = favorites[index + 1];

					favorites = Arrays.copyOf(favorites, favorites.length - 1);
				}
			}
		}

		if(currentSavePoint != null && currentSavePoint.isModified()){
			Statement stmnt = c.createStatement();
			ResultSet rs = stmnt.executeQuery("SELECT COUNT(manga_id) AS counts FROM Recents WHERE manga_id =  ".concat(String.valueOf(currentSavePoint.MANGA_ID)));
			int count = rs.getInt("counts");

			stmnt.close();
			rs.close();

			PreparedStatement pc = c.prepareStatement(count == 0 ? ChapterSavePoint.UPDATE_SQL_NEW : ChapterSavePoint.UPDATE_SQL_OLD);
			currentSavePoint.unload(pc);
			pc.executeBatch();
			pc.close();

			if(recents != null && notSelfInitiated)
				recents[currentSavePoint.ARRAY_INDEX] = new MinimalChapterSavePoint(currentSavePoint);

			if(readTimeDecreasing != null && notSelfInitiated){
				int index = 0;
				int value = currentSavePoint.ARRAY_INDEX;

				for (; index < readTimeDecreasing.length; index++) {
					if(readTimeDecreasing[index] == value)
						break;
				}

				for (; index > 0; index--) 
					readTimeDecreasing[index] = readTimeDecreasing[index - 1];

				readTimeDecreasing[0] = value;
			}

			currentSavePoint.setUnmodifed();
		}

		c.commit();

		if(notSelfInitiated){
			mangas[currentManga.ARRAY_INDEX] = mangas[currentManga.ARRAY_INDEX == 0 ? 1 : 0]  instanceof MinimalListManga ? new MinimalListManga(currentManga) :  new MinimalManga(currentManga);

			if(currentSortingMethod == SortingMethod.READ_TIME_DECREASING || 
					currentSortingMethod == SortingMethod.READ_TIME_INCREASING ||
					currentSortingMethod == SortingMethod.FAVORITES || 
					currentSortingMethod == SortingMethod.DELETE_QUEUED 
					){
				changeCurrentSortingMethod(currentSortingMethod, false);
				notifyWatchers(MOD_MODIFIED_INTERNALLY);
			}
		}
	}

	private void notifyWatchers(int responseCode){ for (IntConsumer c : mangaManegerWatchers)  c.accept(responseCode);}

	HashSet<Integer> deleteQueuedMangas;

	public void addMangaToDeleteQueue(MinimalManga m) {
		if( deleteQueuedMangas == null)
			deleteQueuedMangas = new HashSet<>();
		if(deleteQueuedMangas.add(m.ARRAY_INDEX))
			notifyWatchers(DQ_UPDATED);
	}

	public void removeMangaFromDeleteQueue(MinimalManga m) {
		if(deleteQueuedMangas.remove(m.ARRAY_INDEX))
			notifyWatchers(DQ_UPDATED);
	}

	public boolean isMangaInDeleteQueue(MinimalManga m) {
		return deleteQueuedMangas != null && deleteQueuedMangas.contains(m.ARRAY_INDEX);
	}
	public boolean mangasDeleteQueueIsEmpty(){
		return deleteQueuedMangas.isEmpty();
	}

	private void processDeleteQueue() {
		if(deleteQueuedMangas == null || deleteQueuedMangas.isEmpty())
			return;

		StringBuilder b = new StringBuilder();
		b.append('(');
		deleteQueuedMangas.forEach(i -> b.append(getMangaId(i)).append(','));
		b.deleteCharAt(b.length() - 1);
		b.append(')');

		String joinIds =  b.toString();

		try (Connection c = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
				Statement stmnt = c.createStatement();) {

			c.setAutoCommit(false);

			String sql = "SELECT manga_id, dir_name FROM MangaData WHERE manga_id IN"+joinIds;

			b.append("\r\nSQL").append(sql).append("\r\n\r\n");

			ResultSet rs = stmnt.executeQuery(sql);
			List<File> dirs = new ArrayList<>();

			while(rs.next()){
				String name = rs.getString("dir_name");
				int id = rs.getInt("manga_id");

				b.append("id: ")
				.append(id)
				.append(",  name:")
				.append(name);

				File root =  RH.mangaRootFolder().toFile();

				File dir = new File(root, name);

				boolean bool = dir.exists();

				b.append(",  file count: ")
				.append(bool ?  dir.list().length : " Dir does not exists")
				.append(System.lineSeparator());

				if(bool)
					dirs.add(dir);
			}
			rs.close();
			rs = null;

			JTextArea t = new JTextArea(b.toString(), 20, 40);
			t.setFont(new Font(null, 1, 20));
			int option = JOptionPane.showConfirmDialog(null, new JScrollPane(t), "R U Sure?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

			if(option == JOptionPane.OK_OPTION){
				dirs = dirs.stream()
						.peek(f -> Stream.of(f.listFiles()).forEach(File::delete))
						.filter(f -> !f.delete())
						.collect(Collectors.toList());

				if(!dirs.isEmpty()){
					b.append("\r\n\r\nFiles needs Attention \r\n");
					for (File f : dirs) b.append(f).append(System.lineSeparator());
				}

				stmnt.executeUpdate("DELETE FROM MangaData WHERE manga_id IN"+joinIds);
				stmnt.executeUpdate("DELETE FROM MangaUrls WHERE manga_id IN"+joinIds);
				stmnt.executeUpdate("DELETE FROM Recents WHERE manga_id IN"+joinIds);

				c.commit();
			}
			else 
				Utils.showHidePopup("delete cancelled", 1500);

		}
		catch (SQLException e) {
			Utils.openErrorDialoag(null, "error while deleting from database ids\r\n"+joinIds,MangaManeger.class,631/*{LINE_NUMBER}*/, e);
			return;
		}
	}

	public MinimalChapterSavePoint getChapterSavePoint(int array_index) {
		return recents[array_index];
	}

	/**
	 * 
	 * @return a <b>copy</b> of mangasOnDisplay
	 */
	public int[] getMangasOnDisplay() {
		return Arrays.copyOf(mangasOnDisplay, mangasOnDisplay.length);
	}

	/**
	 * @return mangasOnDisplay.length
	 */
	public int getMangasOnDisplayCount() {
		return mangasOnDisplay.length;
	}

	/**
	 * 
	 * @param sortingMethod by which mangas are sorted
	 * @param sortCurrentMangasOnDisplay if true and mangasOnDisplay is not null then current mangasOnDisplay is sorted, otherwise  mangasOnDisplay is set with new full mangas sorted array
	 */
	public void changeCurrentSortingMethod(SortingMethod sortingMethod, boolean sortCurrentMangasOnDisplay) {
		if(sortingMethod == null)
			Utils.openErrorDialoag(null, "sortingMethod = null, changeCurrentSortingMethod()",MangaManeger.class,662/*{LINE_NUMBER}*/, null);

		if(mangasOnDisplay == null)
			sortCurrentMangasOnDisplay = false;

		

		this.currentSortingMethod = sortingMethod;

		mangasOnDisplay = sortArray(sortCurrentMangasOnDisplay ? mangasOnDisplay : null);

		notifyWatchers(MOD_MODIFIED);

		
	}
	/**
	 * arrayToBeSorted is sorted with currentSortingMethod 
	 * 
	 * @param  
	 * @return a new sorted array if arrayToBeSorted = null, otherwise arrayToBeSorted is sorted and returned 
	 */
	public int[] sortArray(int[] arrayToBeSorted){
		if(arrayToBeSorted != null && arrayToBeSorted.length < 2)
			return arrayToBeSorted;

		if(arrayToBeSorted == null)
			return getSortedFullArray(currentSortingMethod, true);
		else{ 
			sortArray(getSortedFullArray(currentSortingMethod, false), arrayToBeSorted);
			return arrayToBeSorted;
		}
	}

	private void sortArray(int[] sortedArray, int[] arrayToBeSorted){
		if(arrayToBeSorted.length < 2)
			return;

		if(arrayToBeSorted.length == sortedArray.length)
			for (int i = 0; i < sortedArray.length; i++) arrayToBeSorted[i] = sortedArray[i];
		else{
			Arrays.sort(arrayToBeSorted);
			int[] array2 = new int[arrayToBeSorted.length];

			for (int i = 0, j = 0; i < array2.length; j++) {
				if(Arrays.binarySearch(arrayToBeSorted, sortedArray[j]) >= 0)
					array2[i++] = sortedArray[j];
			}
			for (int i = 0; i < array2.length; i++) arrayToBeSorted[i] = array2[i];
		}
	}

	/*
	 * Sorted Arrays (these are fixed, because i don't think they will change during one session)
	 */ 
	private int[] alphabeticallyIncreasing;
	private int[] updateTimeDecreasing;
	private int[] ranksIncreasing;
	//can change arrays
	private int[] readTimeDecreasing;
	private int[] favorites;

	/**
	 * 
	 * @param sortingMethod
	 * @param returnAcopy if true method will return a copy of sorted array, else original array, if there is a need for reverse, it will return a reversed copy, disregarding value return_copy      
	 * @return
	 */
	private int[] getSortedFullArray(SortingMethod sortingMethod, boolean returnAcopy){
		fillSortedArray(sortingMethod);

		switch (sortingMethod) {
		case ALPHABETICALLY_DECREASING:
			return arrayReversedCopy(alphabeticallyIncreasing);
		case ALPHABETICALLY_INCREASING:
			return returnAcopy ? arrayCopy(alphabeticallyIncreasing) : alphabeticallyIncreasing;
		case RANKS_DECREASING:
			return arrayReversedCopy(ranksIncreasing);
		case RANKS_INCREASING:
			return returnAcopy ? arrayCopy(ranksIncreasing) : ranksIncreasing;
		case READ_TIME_DECREASING:
			return returnAcopy ? arrayCopy(readTimeDecreasing) : readTimeDecreasing;
		case READ_TIME_INCREASING:
			return arrayReversedCopy(readTimeDecreasing);
		case UPDATE_TIME_DECREASING:
			return returnAcopy ? arrayCopy(updateTimeDecreasing) : updateTimeDecreasing;
		case UPDATE_TIME_INCREASING:
			return arrayReversedCopy(updateTimeDecreasing);
		case DELETE_QUEUED:
			return deleteQueuedMangas.stream().mapToInt(Integer::intValue).toArray();
		case FAVORITES:
			return returnAcopy ? arrayCopy(favorites) : favorites;
		default:
			return new int[0];
		}
	}

	private int[] arrayReversedCopy(int[] array) {
		int[] array2 = arrayCopy(array);
		reverse(array2);
		return array2;
	}

	private int[] arrayCopy(int[] array) {
		return Arrays.copyOf(array, array.length);
	}

	private void fillSortedArray(SortingMethod sortingMethod) {
		if(sortingMethod == SortingMethod.DELETE_QUEUED)
			return;
		

		if(alphabeticallyIncreasing == null && (sortingMethod == SortingMethod.ALPHABETICALLY_INCREASING || sortingMethod == SortingMethod.ALPHABETICALLY_DECREASING))
			alphabeticallyIncreasing = extractSortedArrayFromDatabase("SELECT manga_id FROM MangaData ORDER BY manga_name");
		else if(updateTimeDecreasing == null && (sortingMethod == SortingMethod.UPDATE_TIME_INCREASING || sortingMethod == SortingMethod.UPDATE_TIME_DECREASING))
			updateTimeDecreasing = extractSortedArrayFromDatabase("SELECT manga_id FROM MangaData ORDER BY last_update_time DESC");
		else if(readTimeDecreasing == null && (sortingMethod == SortingMethod.READ_TIME_INCREASING || sortingMethod == SortingMethod.READ_TIME_DECREASING))
			readTimeDecreasing = extractSortedArrayFromDatabase("SELECT manga_id FROM MangaData ORDER BY last_read_time DESC");
		else if(ranksIncreasing == null && (sortingMethod == SortingMethod.RANKS_INCREASING || sortingMethod == SortingMethod.RANKS_DECREASING))
			ranksIncreasing = extractSortedArrayFromDatabase("SELECT manga_id FROM MangaData ORDER BY rank");
		else if(favorites == null && sortingMethod == SortingMethod.FAVORITES)
			favorites = extractSortedArrayFromDatabase("SELECT manga_id FROM MangaData WHERE isFavorite = 1 ORDER BY last_update_time DESC");

		
	}

	private int[] extractSortedArrayFromDatabase(String sql) {
		int[] array = new int[mangas.length];
		boolean[] error = {false};
		int newLength[] = {0};

		databaseQuery(sql, rs -> {
			try {
				int i = 0;
				while(rs.next())
					array[i++] = rs.getInt("manga_id");

				mangaIdsToArrayIndices(array);

				newLength[0] = i;

			} catch (SQLException e) {
				Utils.openErrorDialoag(null, "Error while extracting with sql: \r\n" + sql,MangaManeger.class,803/*{LINE_NUMBER}*/, e);
				error[0] = true;
			}
		});

		if(!error[0]){
			if(array.length != newLength[0])
				return Arrays.copyOf(array, newLength[0]);
		}

		return error[0] ? null : array;
	}

	private void reverse(int[] array) {
		if(array.length < 2)
			return;

		for (int i = 0; i < array.length/2; i++) {
			int temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	/**
	 * manga_id -> corresponding array_index    
	 * @param array
	 */
	private void mangaIdsToArrayIndices(int[] array) {
		for (int i = 0; i < array.length; i++) array[i] = getArrayIndex(array[i]);
	}
}
