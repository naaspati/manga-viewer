package samrock.manga;

import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.newsamrock.mangas.MangasMeta.UNREAD_COUNT;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.manga.newsamrock.mangas.MangasMeta;

public class MinimalManga {
    protected static Logger logger = LoggerFactory.getLogger("Manga");

	public static final String[] COLUMN_NAMES = {MANGA_ID, MangasMeta.MANGA_NAME, UNREAD_COUNT};

	protected final int index;
	protected final String mangaName;
	
	protected int unreadCount;

	public MinimalManga(ResultSet rs, int index) throws SQLException {
		this.index = index;
		this.unreadCount = rs.getInt(UNREAD_COUNT);
		this.mangaName = rs.getString(MangasMeta.MANGA_NAME);
	}
	public MinimalManga(Manga manga) {
		this.index = manga.index;
		this.unreadCount = manga.getUnreadCount();
		this.mangaName = manga.mangaName;
	}
	public int getIndex() {
        return index;
    }
	public int getUnreadCount() {
		return unreadCount;
	}
	public void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}
	public String getMangaName() {
		return mangaName;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || getClass() != o.getClass())
			return false;

		return ((MinimalManga)o).mangaName.equals(this.mangaName);
	}
	@Override
    public String toString() {
        return "MinimalManga [arrayIndex=" + index + ", mangaName=" + mangaName + ", unreadCount=" + unreadCount
                + "]";
    }
    public void update(Manga m) {
		unreadCount = m.getUnreadCount();
		
	}
}
