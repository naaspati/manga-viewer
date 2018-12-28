package samrock.manga.recents;

import static sam.manga.samrock.meta.RecentsMeta.CHAPTER_ID;
import static sam.manga.samrock.meta.RecentsMeta.CHAPTER_NAME;
import static sam.manga.samrock.meta.RecentsMeta.MANGA_ID;
import static sam.manga.samrock.meta.RecentsMeta.TIME;

import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.SQLException;

import sam.reference.ReferenceUtils;
import samrock.manga.Chapters.Chapter;
import samrock.manga.MinimalManga;

public class MinimalChapterSavePoint {
	public static final String[] COLUMNS_NAMES = {MANGA_ID,CHAPTER_ID, CHAPTER_NAME,TIME};

	public final int mangaId;
	protected long saveTime;
	/**
	 * expected chapterId
	 */
	protected int chapterId;
	protected String chapterFileName;
	protected WeakReference<MinimalManga> manga;
	protected WeakReference<Chapter> chapter;
	
	private MinimalChapterSavePoint(int mangaId, int mangaIndex) {
		this.mangaId = mangaId;
	}
	public MinimalChapterSavePoint(ResultSet rs, int mangaIndex) throws SQLException {
		this.mangaId = rs.getInt(MANGA_ID);
		this.saveTime = rs.getLong(TIME);
		this.chapterFileName = rs.getString(CHAPTER_NAME);
		this.chapterId = rs.getInt(CHAPTER_ID);
	}
	@SuppressWarnings("deprecation")
	public MinimalChapterSavePoint(MinimalManga manga, Chapter chapter, long saveTime) {
		this.mangaId = manga.getMangaId();
		this.saveTime = saveTime;
		setChapter(chapter);
		this.manga = new WeakReference<>(manga);
	}
	
	public final String getChapterFileName() { return chapterFileName; }
	public final int getMangaId() { return mangaId; }
	public final int getChapterId() { return chapterId; }

	public long getSaveTime() { return saveTime; }
	public void setSaveTime(long saveTime) { this.saveTime = saveTime; }

	public void setChapter(Chapter chapter) { 
		this.chapterFileName = chapter.getFileName();
		this.chapterId = chapter.getChapterId();
		this.chapter = new WeakReference<>(chapter);
	}
	/**
	 * possible may return null, since Chapter is stored weakly
	 * @return
	 */
	public Chapter getChapter() {
		return ReferenceUtils.get(chapter);
	}
	/**
	 * possible may return null, since Manga is stored weakly
	 * @return
	 */
	public MinimalManga getManga() {
		return ReferenceUtils.get(manga);
	}
}