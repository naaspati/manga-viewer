package samrock.manga.chapter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static sam.manga.newsamrock.column.names.RecentsMeta.*;
import sam.sql.sqlite.querymaker.QueryMaker;
import samrock.manga.Manga;
import samrock.manga.recents.MinimalChapterSavePoint;
import samrock.utils.Utils;

public class ChapterSavePoint extends MinimalChapterSavePoint {
	public int x = 0;
	public int y = 0;
	public double scale = 1.0;
	public final int mangaId;
	private boolean isModified = false;

	/**
	 * @param resultSet
	 * @param arrayIndex
	 * @throws SQLException
	 */
	public ChapterSavePoint(ResultSet resultSet, int arrayIndex) throws SQLException {
		super(resultSet, arrayIndex);
		x = resultSet.getInt("x");
		y = resultSet.getInt("y");
		scale = resultSet.getDouble("scale");
		this.mangaId = resultSet.getInt("manga_id");
	}
	
	
	/**
	 * x = 0, y = 0 scale = 1, first chapter, time = 0 
	 * @param currentManga
	 */
	public ChapterSavePoint(Manga m) {
		super(m.getIndex(), m.getChapter(m.isChaptersInIncreasingOrder() ? 0 : m.getChaptersCount() - 1).getFileName(), 0);
		mangaId = m.getMangaId();
	}
	
	public ChapterSavePoint(Manga manga, String chapterName, double x, double y, double scale, long time) {
		super(manga.getIndex(), chapterName, time);
		this.mangaId = manga.getMangaId();
		this.x = (int) x;
		this.y = (int) y;
		this.scale = scale;
		
	}

	/**
	 * public static final String UPDATE_SQL = "UPDATE Recents SET chapter_name = 1, x = 2, y = 3, scale = 4, time = 5 WHERE manga_id = 6"
	 */
	public static final String UPDATE_SQL_OLD = QueryMaker.getInstance().update(TABLE_NAME).placeholders(CHAPTER_NAME, X, Y, SCALE, TIME).where(w -> w.eqPlaceholder(MANGA_ID)).build();
	public static final String UPDATE_SQL_NEW = QueryMaker.getInstance().insertInto(TABLE_NAME).placeholders(CHAPTER_NAME, X, Y, SCALE, TIME, MANGA_ID) ;
	
	/**
	 * sets all corresponding values to the given PreparedStatement and adds it to batch
	 * <br><b>note: doesn't execute the PreparedStatement only PreparedStatement.addBatch() is called</b>  
	 * @param p
	 * @throws SQLException
	 * @throws IOException
	 */
	public void unload(PreparedStatement p) throws SQLException{
		p.setString(1, getChapterFileName());
		p.setInt(2, x);
		p.setInt(3, y);
		p.setDouble(4, scale);
		p.setLong(5, saveTime);
		p.setInt(6, mangaId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChapterSavePoint [x=").append(x).append(", y=").append(y).append(", scale=").append(scale)
				.append(", MANGA_ID=").append(mangaId).append(", saveTime=").append(saveTime).append(" (").append(Utils.getFormattedDateTime(saveTime)).append(")").append(", chapterFileName=")
				.append(chapterFileName).append("]");
		return builder.toString();
	}

	public boolean isModified() {
		return isModified;
	}

	public void reset(String chapterFileName, double x, double y, double scale, long time) {
		
		
		this.chapterFileName = chapterFileName;
		this.x = (int) x;
		this.y = (int) y;
		this.scale = scale;
		this.saveTime = time;
		isModified = true;
		
	}
	
	public void setUnmodifed() {
		isModified = false;
	}
}
