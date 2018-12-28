package samrock.manga.maneger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sam.nopkg.Junk;
import samrock.manga.Manga;

class IndexedManga extends Manga implements IIndexedManga {
	private final int index;
	private List<Integer> deletedChaps; 

	public IndexedManga(int index, ResultSet rs, int version, String[] urls) throws SQLException {
		super(rs, version, urls);
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}
	
	void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}
	
	@Override
	protected void onDeleteChapter(Chapter c) {
		if(c.isDeleted()) {
			if(deletedChaps == null)
				deletedChaps = new ArrayList<>();
			deletedChaps.add(c.getChapterId());
		} else if(deletedChaps != null) {
			deletedChaps.remove(c.getChapterId());
		}

	} 
	List<Integer> getDeletedChaptersIds() {
		return deletedChaps;
	}
	Chapter newChapter(ResultSet rs) throws SQLException {
		return super._newChapter(rs);
	}
	public void setReadCount(int readCount) {
		this.readCount = readCount;
	}
	/** 
	 * void setUnmodifed() {
		version = init_version;
	}
	 * @return
	 */
	public int getVersion() {
		return version;
	}
	void setVersion(int version) {
		this.version = version;
	}

	@Override
	protected List<Chapter> loadChapters() {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
}
