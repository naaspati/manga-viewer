package samrock.manga.recents;
import static sam.manga.samrock.meta.RecentsMeta.CHAPTER_NAME;
import static sam.manga.samrock.meta.RecentsMeta.MANGA_ID;
import static sam.manga.samrock.meta.RecentsMeta.SCALE;
import static sam.manga.samrock.meta.RecentsMeta.TABLE_NAME;
import static sam.manga.samrock.meta.RecentsMeta.TIME;
import static sam.manga.samrock.meta.RecentsMeta.X;
import static sam.manga.samrock.meta.RecentsMeta.Y;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import sam.sql.querymaker.QueryMaker;
import samrock.manga.MinimalManga;
import samrock.utils.Utils;

public class ChapterSavePoint extends MinimalChapterSavePoint {
	private int x = 0;
	private int y = 0;
	private double scale = 1.0;
	private boolean isModified = false;

	/**
	 * @param rs
	 * @param arrayIndex
	 * @throws SQLException
	 */
	public ChapterSavePoint(ResultSet rs, int index) throws SQLException {
		super(rs, index);
		x = rs.getInt(X);
		y = rs.getInt(Y);
		scale = rs.getDouble(SCALE);
	}
	public ChapterSavePoint(MinimalManga manga, Chapter chapter, long saveTime) {
		super(manga, chapter, saveTime);
	}
	public ChapterSavePoint(MinimalManga manga, Chapter chapter, double x, double y, double scale, long time) {
		super(manga, chapter, time);
		this.x = (int) x;
		this.y = (int) y;
		this.scale = scale;
	}

	/**
	 * public static final String UPDATE_SQL = "UPDATE Recents SET chapter_name = 1, x = 2, y = 3, scale = 4, time = 5 WHERE manga_id = 6"
	 */
	public static final String UPDATE_SQL_OLD = QueryMaker.getInstance().update(TABLE_NAME).placeholders(CHAPTER_NAME, X, Y, SCALE, TIME).where(w -> w.eqPlaceholder(MANGA_ID)).build();
	public static final String UPDATE_SQL_NEW = QueryMaker.getInstance().insertInto(TABLE_NAME).placeholders(CHAPTER_NAME, X, Y, SCALE, TIME, MANGA_ID);
	
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

	public void reset(Chapter chapter, double x, double y, double scale, long time) {
		setChapter(chapter);
		this.x = (int) x;
		this.y = (int) y;
		this.scale = scale;
		this.saveTime = time;
		isModified = true;
	}
	public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }

    public void setModified(boolean isModified) { this.isModified = isModified; }
    public void setUnmodifed() { isModified = false; }
}
