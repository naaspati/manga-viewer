package samrock.manga.maneger;

import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_NAME;
import static sam.manga.samrock.mangas.MangasMeta.UNREAD_COUNT;

import java.sql.ResultSet;
import java.sql.SQLException;

import samrock.manga.MinimalManga;
import samrock.manga.recents.MinimalChapterSavePoint;

class IndexedMinimalManga extends MinimalManga implements IIndexedManga {
	private final int index;
	protected final int init_version;
	
	public static final String[] columnNames() {
		return new String[] {MANGA_ID, MANGA_NAME, UNREAD_COUNT};
	};

	public IndexedMinimalManga(int index, ResultSet rs, int version) throws SQLException {
		super(rs.getInt(MANGA_ID), version, rs.getString(MANGA_NAME), rs.getInt(UNREAD_COUNT));
		
		this.init_version = version;
		this.index = index;
	}
	
	public boolean isModified() {
		return version != init_version;
	}
	
	@Override
	public int getIndex() {
		return index;
	}
	public void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}
	public int getVersion() {
		return version;
	}
	void setVersion(int version) {
		this.version = version;
	}
	@Override
	public int getMangaId() {
		return super.getMangaId();
	}

	@Override
	protected MinimalChapterSavePoint loadSavePoint() {
		return MangaManeger.recentsDao().getSavePoint(this);
	}

}
