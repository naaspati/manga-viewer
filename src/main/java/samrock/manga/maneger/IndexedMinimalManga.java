package samrock.manga.maneger;

import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_NAME;
import static sam.manga.samrock.mangas.MangasMeta.UNREAD_COUNT;

import java.sql.ResultSet;
import java.sql.SQLException;

import samrock.manga.MinimalManga;

abstract class IndexedMinimalManga extends MinimalManga {
	public final int index;
	private int mod;
	protected final int version;
	
	public static final String[] columnNames() {
		return new String[] {MANGA_ID, MANGA_NAME, UNREAD_COUNT};
	};

	public IndexedMinimalManga(int index, ResultSet rs, int version) throws SQLException {
		super(rs);
		this.version = version;
		this.index = index;
	}
	
	public boolean isModified() {
		return mod != 0;
	}
	@Override
	protected void onModified() {
		mod++;
	}
	public int getVersion() {
		return version;
	}
	@Override
	public int getMangaId() {
		return super.getMangaId();
	}
}
