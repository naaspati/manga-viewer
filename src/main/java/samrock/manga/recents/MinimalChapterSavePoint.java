package samrock.manga.recents;

import static sam.manga.samrock.meta.RecentsMeta.CHAPTER_ID;
import static sam.manga.samrock.meta.RecentsMeta.CHAPTER_NAME;
import static sam.manga.samrock.meta.RecentsMeta.MANGA_ID;
import static sam.manga.samrock.meta.RecentsMeta.TIME;

import java.sql.ResultSet;
import java.sql.SQLException;

import samrock.manga.Chapter;
import samrock.manga.MinimalManga;

public class MinimalChapterSavePoint {
	public static final String[] columnNames() {
		return new String[] {MANGA_ID,CHAPTER_ID, CHAPTER_NAME,TIME};
	}

	protected final long saveTime;
	protected final int manga_id;  
	protected final int chapterId;
	protected final String chapterFileName;
	
	protected MinimalChapterSavePoint() {
		this.manga_id = -1;
		this.saveTime = -1;
		this.chapterFileName = null;
		this.chapterId = -1;
	}
	public MinimalChapterSavePoint(ResultSet rs) throws SQLException {
		this.manga_id = rs.getInt(MANGA_ID);
		this.saveTime = rs.getLong(TIME);
		this.chapterFileName = rs.getString(CHAPTER_NAME);
		this.chapterId = rs.getInt(CHAPTER_ID);
	}
	public MinimalChapterSavePoint(long saveTime, Chapter chapter, MinimalManga manga) {
		this.saveTime = saveTime;
		this.manga_id = manga.getMangaId();
		this.chapterId = chapter.getChapterId();
		this.chapterFileName = chapter.getFileName();
	}
	
	public int getMangaId() { return manga_id; }
	public String getChapterFileName() { return chapterFileName; }
	public int getChapterId() { return chapterId; }
	public long getSaveTime() { return saveTime; }
}