package samrock.manga;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import samrock.utils.RH;
import samrock.utils.Views;

public class Manga extends MinimalListManga {
	//constants

	public final int MANGA_ID;
	public final int BU_ID;
	public final String DIR_NAME;
	public final String DESCRIPTION;
	public final long LAST_UPDATE_TIME;
	public final String TAGS;
	public final String mangafoxUrl;

	private long lastReadTime;
	private Views startupView;
	/**
	 * chapter_ordering = true -> increasing order <br>
	 * chapter_ordering = false -> decreasing order<br> 
	 */
	private boolean chapterOrdering; // = isInIncreasingOrder

	private Chapter[] chapters;
	private final Path mangaFolder;
	private IntConsumer chapterWatcher;
	private boolean isModified = false;
	/**
	 * Used by chapterEditorPanel, this way manga can handle batch chapter modification 
	 */
	private boolean batchEditingMode = false;

	Manga(ResultSet rs, int arrayIndex, String mangafoxUrl) throws SQLException, ClassNotFoundException, IOException {
		super(rs, arrayIndex);

		MANGA_ID = rs.getInt("manga_id");
		long temp = rs.getLong("bu_id"); 
		BU_ID = temp >= Integer.MAX_VALUE ? -1 :(int)temp;
		DIR_NAME = rs.getString("dir_name");
		DESCRIPTION = rs.getString("description");
		lastReadTime = rs.getLong("last_read_time");
		LAST_UPDATE_TIME = rs.getLong("last_update_time"); 
		startupView = Views.valueOf(rs.getString("startup_view"));
		chapterOrdering = rs.getBoolean("chapter_ordering");
		TAGS = rs.getString("categories");
		mangaFolder = Files.notExists(RH.mangaRootFolder().resolve(DIR_NAME)) ? null : RH.mangaRootFolder().resolve(DIR_NAME);
		this.mangafoxUrl = mangafoxUrl; 

		chapterWatcher = code -> {
			if(batchEditingMode)
				return;

			isModified = true;
			if(code == Chapter.SET_READ || code == Chapter.SET_UNREAD){
				boolean b = code == Chapter.SET_READ;
				readCount = readCount + (b ? +1 : -1);
				unreadCount = unreadCount + (!b ? +1 : -1);
			}
			else if(code == Chapter.DELETED){
				boolean b = batchEditingMode; 
				batchEditingMode = true;
				finalizeMangaChanges();
				batchEditingMode = b;
			}
		};

		chapters = Chapter.bytesToChapters(rs.getBytes("chapters"));
		prepareChapters();
		
	}

	public boolean isModified() {
		return isModified;
	}

	/**
	 * To all Chapters 
	 * c.setWatcher(chapterWatcher);
	 * c.setMangaFolder(mangaFolder);
	 */
	private void prepareChapters() {
		for (Chapter c : chapters) {
			c.setWatcher(chapterWatcher);
			c.setMangaFolder(mangaFolder);
		}
	}

	public boolean isChaptersInIncreasingOrder(){return chapterOrdering;}

	public Chapter getChapter(int chapter_index) {return chapters[chapter_index];}
	/**
	 * @return chapters.length
	 */
	public int getChaptersCount() {
		return chapters.length;
	}

	public int getId() {return MANGA_ID;}

	public Views getStartupView() {return startupView;}

	public Path getMangaFolderPath() {return mangaFolder;}

	public long getLastReadTime() {return lastReadTime;}

	public void setLastReadTime(long lastReadTime) {isModified = true; this.lastReadTime = lastReadTime;}

	public long getLastUpdateTime() {return LAST_UPDATE_TIME;}

	void setStartupView(Views startupView) {
		isModified = true;
		if(startupView.equals(Views.CHAPTERS_LIST_VIEW) || startupView.equals(Views.DATA_VIEW))
			this.startupView = startupView;
	}

	/**
	 * Change Chapter SortingMethod
	 */
	public void reverseChaptersOrder() {
		isModified = true;
		chapterOrdering = !chapterOrdering;
		Chapter.reverse(chapters);
	}

	/**
	 * recounts again chapCountPc, readCount, unreadCount
	 */
	public void resetCounts(){
		isModified = true;
		//{read, unread}
		int[] counts = {0,0};

		long l = Stream.of(chapters)
				.peek(c -> {
					if(c.isRead())
						counts[0]++;
					else
						counts[1]++;
				})
				.map(c -> c.getFileName().replaceFirst(" - \\d+\\.jpe?g$", ""))
				.distinct()
				.count();

		chapCountPc = (int) l;
		readCount = counts[0];
		unreadCount = counts[1];
	}

	public static final String UPDATE_SQL = "UPDATE MangaData SET "
			+ "isFavorite = ?, "
			+ "last_read_time = ?, "
			+ "startup_view = ?, "
			+ "chapter_ordering = ?, "
			+ "chap_count_pc = ?, "
			+ "read_count = ?, "
			+ "unread_count = ?, "
			+ "chapters = ? "
			+ "WHERE manga_id = ?";

	/**
	 * sets all corresponding values to the given PreparedStatement and adds it to batch
	 * <br><b>note: doesn't execute the PreparedStatement only PreparedStatement.addBatch() is called</b>
	 * <br> and sets chapters = null  
	 * @param p
	 * @throws SQLException
	 * @throws IOException
	 */
	void unload(PreparedStatement p) throws SQLException, IOException{
		
		
		p.setBoolean(1, isFavorite());
		p.setLong(2, lastReadTime);
		p.setString(3, startupView.name());
		p.setBoolean(4, chapterOrdering);
		p.setInt(5, getChapCountPc());
		p.setInt(6, getReadCount());
		p.setInt(7, unreadCount);
		p.setBytes(8, Chapter.chaptersToBytes(chapters));
		p.setInt(9, MANGA_ID);
		p.addBatch();
		
	}

	public String getDescription(){
		if(DESCRIPTION == null || DESCRIPTION.trim().isEmpty())
			return "No Description";
		return DESCRIPTION;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Manga [MANGA_ID=").append(MANGA_ID).append(", BU_ID=").append(BU_ID).append(", DIR_NAME=")
		.append(DIR_NAME).append(", DESCRIPTION=").append(DESCRIPTION).append(", LAST_UPDATE_TIME=")
		.append(LAST_UPDATE_TIME).append(", TAGS=").append(TAGS).append(", lastReadTime=").append(lastReadTime)
		.append(", startupView=").append(startupView).append(", chapterOrdering=").append(chapterOrdering)
		.append(", mangaFolder=").append(mangaFolder).append(", mangafoxUrl=").append(mangafoxUrl)
		.append(", AUTHOR_NAME=").append(AUTHOR_NAME).append(", RANK=").append(RANK)
		.append(", CHAP_COUNT_MANGAROCK=").append(CHAP_COUNT_MANGAROCK).append(", chapCountPc=")
		.append(chapCountPc).append(", readCount=").append(readCount).append(", STATUS=").append(STATUS)
		.append(", unreadCount=").append(unreadCount).append(", MANGA_NAME=").append(MANGA_NAME).append("]");
		return builder.toString();
	}


	public void resetChapters() {
		isModified = true;
		chapters = Chapter.listChaptersOrderedNaturally(mangaFolder.toFile(), chapters, chapterOrdering);
		prepareChapters();
		resetCounts();
	}

	public void setUnmodifed() {
		isModified = false;
	}

	public void setBatchEditingMode(boolean batchEditingMode) {
		this.batchEditingMode = batchEditingMode;
	}

	/**
	 * if manga is not in batchEditingMode, that finalizeMangaChanges() call is ignored  
	 */
	public void finalizeMangaChanges() {
		if(!batchEditingMode)
			throw new IllegalStateException("batchEditingMode is false");
		
		isModified = true;
		
		if(mangaFolder.toFile().list().length == 0){
			readCount = 0;
			unreadCount = 0;
			chapCountPc = 0;
			chapters = new Chapter[0];
			return;
		}
		
		if(Stream.of(chapters).anyMatch(Chapter::isDeleted)){
			chapters = Stream.of(chapters).filter(c -> !c.isDeleted()).toArray(Chapter[]::new);

			if(chapters.length == 0)
				chapters = Chapter.listChaptersOrderedNaturally(mangaFolder.toFile(), null, isChaptersInIncreasingOrder());
		
			prepareChapters();
		}
		
		resetCounts();
	}
}
