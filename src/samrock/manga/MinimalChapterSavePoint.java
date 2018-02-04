package samrock.manga;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MinimalChapterSavePoint {
	public static final String SELECT_SQL = "SELECT chapter_name, manga_id, _time FROM Recents";
	public final int ARRAY_INDEX;
	protected long saveTime;
	protected String chapterFileName;

	public MinimalChapterSavePoint(ResultSet rs, int indexInArray) throws SQLException {
		ARRAY_INDEX = indexInArray;
		saveTime = rs.getLong("_time");
		chapterFileName = rs.getString("chapter_name");
	}

	public MinimalChapterSavePoint(int arrayIndex, String chapterName, long saveTime) {
		ARRAY_INDEX = arrayIndex;
		this.saveTime = saveTime;
		this.chapterFileName = chapterName;
	}

	public MinimalChapterSavePoint(ChapterSavePoint s) {
		ARRAY_INDEX = s.ARRAY_INDEX;
		saveTime = s.saveTime;
		chapterFileName = s.chapterFileName;
	}

	public String getChapterFileName() {
		return chapterFileName;
	}

	public int getArrayIndex() {
		return ARRAY_INDEX;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || getClass() != o.getClass())
			return false;

		return ((MinimalChapterSavePoint)o).ARRAY_INDEX == this.ARRAY_INDEX;
	}

	public long getSaveTime() {
		return saveTime;
	}

	public void setSaveTime(long saveTime) {
		this.saveTime = saveTime;
	}

	public void setChapterName(String chapterName) {
		this.chapterFileName = chapterName;
	}
}
