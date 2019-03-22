package samrock.manga;

import java.sql.ResultSet;
import java.sql.SQLException;

import static sam.manga.samrock.mangas.MangasMeta.*;
import samrock.manga.recents.MinimalChapterSavePoint;

public abstract class MinimalManga {
	public final int manga_id;
	public final String mangaName;
	protected MinimalChapterSavePoint savePoint;
	
	protected int unreadCount;
	
	protected MinimalManga(int manga_id, String mangaName, int unreadCount) {
		this.manga_id = manga_id;
		this.mangaName = mangaName;
		this.unreadCount = unreadCount;
	}
	protected MinimalManga(ResultSet rs) throws SQLException {
		this.manga_id = rs.getInt(MANGA_ID);
		this.mangaName = rs.getString(MANGA_NAME);
		this.unreadCount = rs.getInt(UNREAD_COUNT);
	}
	
	protected abstract void onModified();
	
	protected static final MinimalChapterSavePoint NULL_SAVE = new MinimalChapterSavePoint() { };
	
	public MinimalChapterSavePoint getSavePoint() {
		if(NULL_SAVE == savePoint)
			return null;
		if(savePoint != null)
			return savePoint;
		
		savePoint = loadSavePoint();
		if(savePoint == null)
			savePoint = NULL_SAVE;
		
		return savePoint;
	}
	
	protected abstract MinimalChapterSavePoint loadSavePoint();
	
	public int getUnreadCount() {
		return unreadCount;
	}
	public String getMangaName() {
		return mangaName;
	}
	public int getMangaId() {
		return manga_id;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null || getClass() != o.getClass())
			return false;

		return ((MinimalManga)o).manga_id == this.manga_id;
	}
}
