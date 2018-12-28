package samrock.manga;

import static sam.manga.samrock.mangas.MangasMeta.BU_ID;
import static sam.manga.samrock.mangas.MangasMeta.CATEGORIES;
import static sam.manga.samrock.mangas.MangasMeta.CHAPTER_ORDERING;
import static sam.manga.samrock.mangas.MangasMeta.DESCRIPTION;
import static sam.manga.samrock.mangas.MangasMeta.DIR_NAME;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.LAST_UPDATE_TIME;
import static sam.manga.samrock.mangas.MangasMeta.STARTUP_VIEW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import sam.config.MyConfig;
import sam.logging.MyLoggerFactory;
import sam.manga.samrock.chapters.ChapterUtils;
import sam.myutils.ThrowException;
import sam.nopkg.Junk;
import samrock.utils.Views;
public abstract class Manga extends MinimalListManga {
	private static final Logger LOGGER = MyLoggerFactory.logger("Manga");

	//constants
	private final int buId;
	private final String dirName;
	private final String description;
	private final long last_update_time;
	private final String tags;
	private final List<String> urls;

	private long lastReadTime;
	private Views startupView;

	private boolean chapters_loaded;
	private List<Chapter> chapters;
	private final Path mangaFolder;

	/**
	 * chapter_ordering = true -> increasing order <br>
	 * chapter_ordering = false -> decreasing order<br> 
	 */
	private boolean chapterOrdering; // = isInIncreasingOrder

	public Manga(ResultSet rs, int version, String[] urls) throws SQLException {
		super(rs, version);

		buId = rs.getInt(BU_ID);
		dirName = rs.getString(DIR_NAME);
		description = rs.getString(DESCRIPTION);
		lastReadTime = rs.getLong(LAST_READ_TIME);
		last_update_time = rs.getLong(LAST_UPDATE_TIME); 
		startupView = Views.parse(rs.getString(STARTUP_VIEW));
		tags = rs.getString(CATEGORIES);
		Path p = Paths.get(MyConfig.MANGA_DIR, dirName);
		mangaFolder = Files.exists(p) ? p : null;
		chapterOrdering = rs.getBoolean(CHAPTER_ORDERING);
		this.urls = Collections.unmodifiableList(Arrays.asList(urls));
	}
	public String getDirName() { return dirName; }
	public String getTags() { return tags; }
	public int getBuId() { return buId; }
	public List<String> getUrls() { return urls; }

	public Views getStartupView() {return startupView;}
	public Path getMangaFolderPath() {return mangaFolder;}
	public long getLastReadTime() {return lastReadTime;}
	public void setLastReadTime(long lastReadTime) {modified(); this.lastReadTime = lastReadTime;}
	public long getLastUpdateTime() {return last_update_time;}

	void setStartupView(Views startupView) {
		modified();
		if(startupView.equals(Views.CHAPTERS_LIST_VIEW) || startupView.equals(Views.DATA_VIEW))
			this.startupView = startupView;
	}

	public String getDescription(){
		if(description == null || description.trim().isEmpty())
			return "No Description";
		return description;
	}
	public void resetChapters() {
		if(reloadChapters()) {
			modified();
			resetCounts();  
			LOGGER.debug("chapter reset manga_id: {1}", manga_id);
		}
	}
	private boolean reloadChapters() {
		//FIXME see Chapters.reloadChapters()
		// in garbaged
		return Junk.notYetImplemented();
	}
	void chapterDeleted(Chapter c) {
		modified();
		
		if(c.isRead())
			readCount--;
		else
			unreadCount--;

		String s = removeMultiFileNumber(c.getFileName());

		if(s.equals(c.getFileName()))
			chapCountPc--;
		else
			resetCounts();
	}
	
	private void load() {
		if(!chapters_loaded)
			this.chapters = loadChapters();	
	}
	
	private Chapters open;
	public Chapters asChapters() {
		if(open != null)
			throw new IllegalStateException("in use");
		
		load();
		return open = new Chapters(chapters, () -> open = null);
	}

	protected abstract List<Chapter> loadChapters();

	private static final Pattern pattern = Pattern.compile(" - \\d+\\.jpe?g$");

	/**
	 * recounts again chapCountPc, readCount, unreadCount
	 */
	public void resetCounts(){
		modified();
		chapCountPc = 0;
		readCount = 0;
		unreadCount = 0;

		HashSet<String> set = new HashSet<>();

		for (Chapter c : chapters) {
			if(c.isRead()) readCount++;
			else unreadCount++;

			String s = removeMultiFileNumber(c.getFileName());

			if(s.equals(c.getFileName()))
				chapCountPc++;
			else
				set.add(s);
		}
		chapCountPc = chapCountPc + set.size();
	}
	private String removeMultiFileNumber(String fileName) {
		if(fileName.lastIndexOf('-') > 0)
			return pattern.matcher(fileName).replaceFirst("");

		return fileName; 
	}
	public static StringBuilder commitChaptersChanges(ChapterUtils utils, int manga_id, Iterable<Object> map) {
		// FIXME Auto-generated method stub
		// utils.commitChaptersChanges(ch.getManga().manga_id, Iterables.map(ch, c -> (sam.manga.samrock.chapters.Chapter)c));
		return Junk.notYetImplemented();
	}
	protected Chapter _newChapter(ResultSet rs) throws SQLException {
		return new Chapter(rs);
	}
	protected void onDeleteChapter(Chapter c) { }

	public class Chapter extends sam.manga.samrock.chapters.Chapter {
		public Chapter(double number, String fileName, boolean isRead) {
			super(number, fileName, isRead);
		}
		public Chapter(double number, String fileName) {
			super(number, fileName);
		}
		public Chapter(ResultSet rs) throws SQLException {
			super(rs);
		}
		@Override
		public void setRead(boolean read) {
			if(isRead() == read)
				return;

			super.setRead(read);
			if(isRead()) {
				readCount++;
				unreadCount--;
			} else {
				readCount--;
				unreadCount++;
			}
		}
		@Override
		public void setDeleted(boolean deleted) {
			if(deleted == isDeleted())
				return;
			super.setDeleted(deleted);
			
			if(deleted) {
				if(isRead())
					readCount--;
				else
					unreadCount--;
			} else {
				if(isRead())
					readCount++;
				else
					unreadCount++;
			}
			
			onDeleteChapter(this);
		}
		@Override
		public void setFileName(String name) {
			ThrowException.illegalAccessError();
		}
		@Override
		public void setNumber(double number) {
			ThrowException.illegalAccessError();
		}
	}
}
