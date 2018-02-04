package samrock.manga;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MinimalManga {

	public static final String SELECT_SQL = "SELECT manga_id, manga_name, unread_count FROM MangaData ORDER BY manga_id"; //'ORDER BY manga_id' is to use binary search

	public final int ARRAY_INDEX;
	public final String MANGA_NAME;
	
	protected int unreadCount;

	MinimalManga(ResultSet rs, int arrayIndex) throws SQLException {
		this.ARRAY_INDEX = arrayIndex;
		this.unreadCount = rs.getInt("unread_count");
		this.MANGA_NAME = rs.getString("manga_name");
	}


	public MinimalManga(Manga manga) {
		this.ARRAY_INDEX = manga.ARRAY_INDEX;
		this.unreadCount = manga.getUnreadCount();
		this.MANGA_NAME = manga.MANGA_NAME;
	}
	
	public int getUnreadCount() {
		return unreadCount;
	}

	public void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}

	public String getName() {
		return MANGA_NAME;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || getClass() != o.getClass())
			return false;

		return ((MinimalManga)o).MANGA_NAME.equals(this.MANGA_NAME);
	}


	@Override
	public String toString() {
		return new StringBuilder().append("MinimalManga [ARRAY_INDEX=").append(ARRAY_INDEX)
				.append(", MANGA_NAME=").append(MANGA_NAME).append("]").toString();
	}

	public void update(Manga m) {
		unreadCount = m.getUnreadCount();
		
	}
}
