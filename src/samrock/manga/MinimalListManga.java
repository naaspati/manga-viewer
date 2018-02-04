package samrock.manga;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MinimalListManga extends MinimalManga {
	
	public static final String SELECT_SQL = "SELECT manga_id, manga_name, author, rank, chap_count_mangarock, chap_count_pc, unread_count, read_count, status, isFavorite FROM MangaData ORDER BY manga_id";
	
	public final String AUTHOR_NAME;
	public final int RANK;
	public final int CHAP_COUNT_MANGAROCK;
	protected int chapCountPc;
	protected int readCount;
	/**
	 * as STATUS = true if manga is 'Completed'
	 * <br>
	 * otherwise STATUS = false means manga is 'On Going'    
	 */
	public final boolean STATUS;// = isCompleted
	
	private boolean isFavorited;
	
	MinimalListManga(ResultSet rs, int arrayIndex) throws SQLException {
		super(rs, arrayIndex);
		AUTHOR_NAME = rs.getString("author");
		RANK = rs.getInt("rank");
		CHAP_COUNT_MANGAROCK = rs.getShort("chap_count_mangarock");
		chapCountPc = rs.getShort("chap_count_pc");
		readCount = rs.getShort("read_count");
		STATUS = rs.getBoolean("status");
		isFavorited = rs.getBoolean("isFavorite");
	}

	public MinimalListManga(Manga m) {
		super(m);
		AUTHOR_NAME = m.AUTHOR_NAME;
		RANK = m.RANK;
		CHAP_COUNT_MANGAROCK = m.CHAP_COUNT_MANGAROCK;
		chapCountPc = m.chapCountPc;
		readCount = m.readCount;
		STATUS = m.STATUS;
	}

	/**
	 * 
	 * @return number of chapter counted in storage
	 */
	public int getChapCountPc() {
		return chapCountPc;
	}

	public String getAuthorName() {
		return AUTHOR_NAME;
	}

	public int getRank() {
		return RANK;
	}

	/**
	 * 
	 * @return number of chapter listed in mangarock.db
	 */
	public int getChapCountMangarock() {
		return CHAP_COUNT_MANGAROCK;
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
