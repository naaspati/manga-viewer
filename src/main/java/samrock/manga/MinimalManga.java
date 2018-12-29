package samrock.manga;

import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.UNREAD_COUNT;

import java.sql.ResultSet;
import java.sql.SQLException;

import sam.manga.samrock.mangas.MangasMeta;
import samrock.manga.recents.MinimalChapterSavePoint;

public abstract class MinimalManga {
    // protected static Logger logger = MyLoggerFactory.logger("Manga");

	public static final String[] COLUMN_NAMES() {
		return new String[] {MANGA_ID, MangasMeta.MANGA_NAME, UNREAD_COUNT};
	};
	
	protected final int manga_id;
	protected int version;
	protected final int init_version; 
	protected final String mangaName;
	protected MinimalChapterSavePoint savePoint;
	
	protected int unreadCount;

	protected MinimalManga(ResultSet rs, int version) throws SQLException {
		this.init_version = version;
		this.version = version;
		this.manga_id = rs.getInt(MANGA_ID);
		this.unreadCount = rs.getInt(UNREAD_COUNT);
		this.mangaName = rs.getString(MangasMeta.MANGA_NAME);
	}
	
	protected MinimalManga(int manga_id, int version, String mangaName, int unreadCount) {
		this.manga_id = manga_id;
		this.version = version;
		this.init_version = version;
		this.mangaName = mangaName;
		this.unreadCount = unreadCount;
	}
	
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

	protected void modified() {
		version++;
	}
	public int getUnreadCount() {
		return unreadCount;
	}
	public String getMangaName() {
		return mangaName;
	}
	protected int getMangaId() {
		return manga_id;
	}
	public boolean isModified() {
		return version != init_version;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null || getClass() != o.getClass())
			return false;

		return ((MinimalManga)o).manga_id == this.manga_id;
	}
}
