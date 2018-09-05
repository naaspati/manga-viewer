package samrock.manga;

import static sam.manga.newsamrock.mangas.MangasMeta.AUTHOR;
import static sam.manga.newsamrock.mangas.MangasMeta.CHAP_COUNT_MANGAROCK;
import static sam.manga.newsamrock.mangas.MangasMeta.CHAP_COUNT_PC;
import static sam.manga.newsamrock.mangas.MangasMeta.IS_FAVORITE;
import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.newsamrock.mangas.MangasMeta.MANGA_NAME;
import static sam.manga.newsamrock.mangas.MangasMeta.RANK;
import static sam.manga.newsamrock.mangas.MangasMeta.READ_COUNT;
import static sam.manga.newsamrock.mangas.MangasMeta.STATUS;
import static sam.manga.newsamrock.mangas.MangasMeta.UNREAD_COUNT;

import java.sql.ResultSet;
import java.sql.SQLException;
public class MinimalListManga extends MinimalManga {
    
	public static final String[] COLUMN_NAMES = {MANGA_ID, MANGA_NAME, AUTHOR, RANK, CHAP_COUNT_MANGAROCK, CHAP_COUNT_PC, UNREAD_COUNT, READ_COUNT, STATUS, IS_FAVORITE};
	
	protected final String authorName;
	protected final int rank;
	protected final int chapCountMangarock;
	protected int chapCountPc;
	protected int readCount;
	/**
	 * as STATUS = true if manga is 'Completed'
	 * <br>
	 * otherwise STATUS = false means manga is 'On Going'    
	 */
	protected final boolean status;// = isCompleted
	
	protected boolean isFavorited;
	
	public MinimalListManga(ResultSet rs, int arrayIndex) throws SQLException {
		super(rs, arrayIndex);
		authorName = rs.getString(AUTHOR);
		rank = rs.getInt(RANK);
		chapCountMangarock = rs.getShort(CHAP_COUNT_MANGAROCK);
		chapCountPc = rs.getShort(CHAP_COUNT_PC);
		readCount = rs.getShort(READ_COUNT);
		status = rs.getBoolean(STATUS);
		isFavorited = rs.getBoolean(IS_FAVORITE);
	}

	public MinimalListManga(Manga m) {
		super(m);
		authorName = m.authorName;
		rank = m.rank;
		chapCountMangarock = m.chapCountMangarock;
		chapCountPc = m.chapCountPc;
		readCount = m.readCount;
		status = m.status;
	}
	public String getStatusString() {
        return status ? "Completed" : "On Going";
    }
    /**
	 * 
	 * @return number of chapter counted in storage
	 */
	public int getChapCountPc() {
		return chapCountPc;
	}
	public String getAuthorName() {
		return authorName;
	}
	public int getRank() {
		return rank;
	}
	/**
	 * 
	 * @return number of chapter listed in mangarock.db
	 */
	public int getChapCountMangarock() {
		return chapCountMangarock;
	}
	public int getReadCount() {
		return readCount;
	}
	public void setReadCount(int readCount) {
		this.readCount = readCount;
	}
	
	/**
	 * true if added to favorited else false
	 * @return
	 */
	public boolean isFavorite() {
		return isFavorited;
	}
	public String isFavoriteString() {
        return isFavorited ? "Yes" : "No";
    }
	public void setFavorite(boolean isFavorited) {
		this.isFavorited = isFavorited;
	}
	@Override
	public void update(Manga m) {
		chapCountPc = m.getChapCountPc();
		readCount = m.getReadCount();
		isFavorited = m.isFavorite();
		super.update(m);
	}
}
