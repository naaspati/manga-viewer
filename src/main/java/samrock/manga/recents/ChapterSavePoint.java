package samrock.manga.recents;
import static sam.manga.samrock.meta.RecentsMeta.SCALE;
import static sam.manga.samrock.meta.RecentsMeta.X;
import static sam.manga.samrock.meta.RecentsMeta.Y;

import java.sql.ResultSet;
import java.sql.SQLException;

import samrock.Utils;
import samrock.manga.Chapter;
import samrock.manga.MinimalManga;

public class ChapterSavePoint extends MinimalChapterSavePoint {
	public final int x;
	public final int y;
	public final double scale;
	public final long saveTime;

	public final MinimalManga manga;
	public final Chapter chapter;

	/**
	 * @param rs
	 * @param arrayIndex
	 * @throws SQLException
	 */
	public ChapterSavePoint(ResultSet rs, MinimalManga manga, Chapter chapter) throws SQLException {
		super(rs);
		this.x = rs.getInt(X);
		this.y = rs.getInt(Y);
		this.scale = rs.getDouble(SCALE);
		
		this.manga = manga;
		this.chapter = chapter;
		this.saveTime = super.saveTime;
	}
	
	public ChapterSavePoint(MinimalManga manga, Chapter chapter, long saveTime) {
		super(saveTime, chapter, manga);

		this.x = 0;
		this.y = 0;
		this.scale = 1.0;
		
		this.manga = manga;
		this.chapter = chapter;
		this.saveTime = super.saveTime;
		
	}
	public ChapterSavePoint(MinimalManga manga, Chapter chapter, double x, double y, double scale, long time) {
		super(time, chapter, manga);
		this.x = (int) x;
		this.y = (int) y;
		this.scale = scale;
		this.saveTime = super.getSaveTime();

		this.manga = manga;
		this.chapter = chapter;
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
}
