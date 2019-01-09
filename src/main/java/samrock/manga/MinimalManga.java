package samrock.manga;

import samrock.manga.recents.MinimalChapterSavePoint;

public abstract class MinimalManga {
	protected final int manga_id;
	protected int version;
	protected final String mangaName;
	protected MinimalChapterSavePoint savePoint;
	
	protected int unreadCount;
	
	protected MinimalManga(int manga_id, int version, String mangaName, int unreadCount) {
		this.manga_id = manga_id;
		this.version = version;
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
	
	@Override
	public boolean equals(Object o) {
		if(o == null || getClass() != o.getClass())
			return false;

		return ((MinimalManga)o).manga_id == this.manga_id;
	}
}
