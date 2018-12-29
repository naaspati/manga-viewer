package samrock.manga.maneger;

import java.sql.ResultSet;
import java.sql.SQLException;

import samrock.manga.MinimalManga;

class IndexedMinimalManga extends MinimalManga implements IIndexedManga {
	private final int index;

	public IndexedMinimalManga(int index, ResultSet rs, int version) throws SQLException {
		super(rs, version);
		this.index = index;
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

}
