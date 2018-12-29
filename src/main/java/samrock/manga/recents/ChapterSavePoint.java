package samrock.manga.recents;
import static sam.manga.samrock.meta.RecentsMeta.SCALE;
import static sam.manga.samrock.meta.RecentsMeta.X;
import static sam.manga.samrock.meta.RecentsMeta.Y;

import java.sql.ResultSet;
import java.sql.SQLException;

import samrock.manga.Chapters.Chapter;
import samrock.manga.MinimalManga;
import samrock.utils.Utils;

public class ChapterSavePoint extends MinimalChapterSavePoint {
	private int x = 0;
	private int y = 0;
	private double scale = 1.0;
	private boolean modified = false;
	private long saveTime;

	private final MinimalManga manga;
	private final Chapter chapter;
	
	protected int chapterId;
	protected String chapterFileName;

	/**
	 * @param rs
	 * @param arrayIndex
	 * @throws SQLException
	 */
	public ChapterSavePoint(ResultSet rs, MinimalManga manga, Chapter chapter) throws SQLException {
		super(rs);
		x = rs.getInt(X);
		y = rs.getInt(Y);
		scale = rs.getDouble(SCALE);
		
		parentLoad();
		
		this.manga = manga;
		this.chapter = chapter;
	}
	private void parentLoad() {
		this.saveTime = super.saveTime;
		this.chapterId  = super.chapterId;
		this.chapterFileName = super.chapterFileName;
	}
	public ChapterSavePoint(MinimalManga manga, Chapter chapter, long saveTime) {
		super(saveTime, chapter, manga);
		
		this.manga = manga;
		this.chapter = chapter;
		
		parentLoad();
	}
	public ChapterSavePoint(MinimalManga manga, Chapter chapter, double x, double y, double scale, long time) {
		super(time, chapter, manga);
		this.x = (int) x;
		this.y = (int) y;
		this.scale = scale;
		this.saveTime = super.getSaveTime();

		this.manga = manga;
		this.chapter = chapter;
		
		parentLoad();
	}

	@Override
	public long getSaveTime() {
		return saveTime;
	}
	public Chapter getChapter() {
		return chapter;
	}
	public MinimalManga getManga() {
		return manga;
	}

	@Override
	public int getMangaId() {
		return super.getMangaId();
	}
	@Override
	public String getChapterFileName() {
		return chapterFileName;
	}
	@Override
	public int getChapterId() {
		return chapterId;
	}
	@Override  
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChapterSavePoint [x=").append(x).append(", y=").append(y).append(", scale=").append(scale)
		.append(", MANGA_ID=").append(manga_id).append(", saveTime=").append(saveTime).append(" (").append(Utils.getFormattedDateTime(saveTime)).append(")").append(", chapterFileName=")
		.append(chapterFileName).append("]");
		return builder.toString();
	}

	public boolean isModified() {
		return modified;
	}

	public void set(Chapter chapter, double x, double y, double scale, long time) {
		this.chapterFileName = chapter.getFileName();
		this.chapterId = chapter.getChapterId();
		this.x = (int) x;
		this.y = (int) y;
		this.scale = scale;
		this.saveTime = time;
		
		modified();
	}
	private void modified() {
		this.modified = true;
	}

	public int x() { return x; }
	public void x(int x) {
		if(this.x == x)
			return;

		this.x = x;
		modified();
	}
	public int y() { return y; }
	public void y(int y) {
		if(this.y == y)
			return;

		this.y = y; 
		modified();
	}

	public double scale() { return scale; }
	public void scale(double scale) {
		if(this.scale == scale)
			return;
		this.scale = scale;
		modified();
	}

}
