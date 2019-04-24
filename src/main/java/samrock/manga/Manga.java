package samrock.manga;

import static sam.manga.samrock.mangas.MangasMeta.BU_ID;
import static sam.manga.samrock.mangas.MangasMeta.CATEGORIES;
import static sam.manga.samrock.mangas.MangasMeta.CHAPTER_ORDERING;
import static sam.manga.samrock.mangas.MangasMeta.DESCRIPTION;
import static sam.manga.samrock.mangas.MangasMeta.DIR_NAME;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.LAST_UPDATE_TIME;
import static sam.manga.samrock.mangas.MangasMeta.STARTUP_VIEW;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;

import sam.collection.ArrayIterator;
import sam.config.MyConfig;
import sam.functions.IOExceptionConsumer;
import sam.manga.samrock.chapters.ChaptersMeta;
import sam.myutils.Checker;
import sam.nopkg.Junk;
import samrock.Utils;
import samrock.api.Views;
import samrock.manga.recents.ChapterSavePoint;

public abstract class Manga extends MinimalListManga implements Iterable<Chapter> {
	private static final Logger LOGGER = Utils.getLogger(Manga.class);
	protected static final ChapImpl[] EMPTY = new ChapImpl[0];
	
	//constants
	private final int buId;
	private final String dirName;
	private final String description;
	private final long last_update_time;
	private final String tags;

	private long lastReadTime;
	private Views startupView;
	
	private boolean chapterOrdering;
	private ChapImpl[] chapters;
	private final Chapters unmod_chapters = new Chapters();
	private final Path dir;
	private boolean savePoint_modified;

	public Manga(ResultSet rs) throws SQLException {
		super(rs);

		buId = rs.getInt(BU_ID);
		dirName = rs.getString(DIR_NAME);
		description = rs.getString(DESCRIPTION);
		lastReadTime = rs.getLong(LAST_READ_TIME);
		last_update_time = rs.getLong(LAST_UPDATE_TIME); 
		startupView = Views.parse(rs.getString(STARTUP_VIEW));
		tags = rs.getString(CATEGORIES);
		dir = Paths.get(MyConfig.MANGA_DIR, dirName);
		this.chapterOrdering = rs.getBoolean(CHAPTER_ORDERING);
	}
	public String getDirName() { return dirName; }
	public Path getDir() {return dir;}
	public String[] getTags() { return parseTags(tags); }
	public int getBuId() { return buId; }

	public Views getStartupView() {return startupView;}
	public long getLastReadTime() {return lastReadTime;}
	public void setLastReadTime(long lastReadTime) {onModified(); this.lastReadTime = lastReadTime;}
	public long getLastUpdateTime() {return last_update_time;}
	
	public Order getChapterOrder() {
		return chapterOrdering ? Order.INCREASING : Order.DECREASING;
	}
	public void setChapterOrder(Order order) {
		this.chapterOrdering = Objects.requireNonNull(order) == Order.INCREASING;
	}

	void setStartupView(Views startupView) {
		onModified();
		if(startupView.equals(Views.CHAPTERS_LIST_VIEW) || startupView.equals(Views.DATA_VIEW))
			this.startupView = startupView;
	}
	
	@Override
	public ChapterSavePoint getSavePoint() {
		return (ChapterSavePoint) super.getSavePoint();
	}
	
	protected abstract String[] parseTags(String tags);
	@Override
	protected abstract ChapterSavePoint loadSavePoint() ;
	protected abstract ChapImpl[] loadChapters() ;
	protected abstract boolean renameChapter(Chapter chapter, String newName, IOExceptionConsumer<String> filenameSetter) throws IOException;
	protected abstract boolean deleteChapterFile(Chapter c) throws IOException;

	public String getDescription(){
		return description;
	}
	private void load() {
		if(chapters != null)
			return;
		this.chapters = loadChapters();
		
		this.readCount = 0;
		this.unreadCount = 0;
		
		for (ChapImpl c : chapters) {
			if(c.isRead())
				readCount++;
			else
				unreadCount++;
		}
	}
	@Override
	public int getReadCount() {
		load();
		return readCount;
	}
	@Override
	public int getUnreadCount() {
		return (getReadCount() - chapters.length) * -1;
	}
	public Chapters getChapters() {
		load();
		return unmod_chapters;
	}
	public void setSavePoint(Chapter chapter, double x, double y, double scale, long time) {
		savePoint_modified = true;
		
		if(chapter == null)
			this.savePoint = null;
		else
			this.savePoint = new ChapterSavePoint(this, chapter, x, y, scale, time);
		
		setLastReadTime(time);
	}
	
	protected class ChapImpl extends Chapter {
		private boolean read;
		
		public ChapImpl(ResultSet rs) throws SQLException {
			super(rs);
			this.read = rs.getBoolean(ChaptersMeta.READ);
		}
		public ChapImpl(int id, double number, String filename) {
			super(id, number, filename);
		}
		@Override
		public boolean isRead() {
			return read;
		}
		@Override
		public void setRead(boolean read) {
			this.read = read;
			if(read) {
				readCount++;
				unreadCount--;
			} else {
				readCount--;
				unreadCount++;
			}
			onModified();
		}
		@Override
		public Path getFilePath() {
			Junk.notYetImplemented();
			// TODO Auto-generated method stub
			return null;
		}
	}
	@Override
	public Iterator<Chapter> iterator() {
		return Checker.isEmpty(chapters) ? Collections.emptyIterator() : new ArrayIterator<>(chapters);
	}
}

