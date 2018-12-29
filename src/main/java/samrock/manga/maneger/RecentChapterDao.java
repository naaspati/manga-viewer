package samrock.manga.maneger;

import static sam.manga.samrock.meta.RecentsMeta.*;
import static sam.myutils.Checker.isOfType;
import static sam.myutils.MyUtilsException.noError;
import static sam.sql.querymaker.QueryMaker.qm;

import java.sql.SQLException;
import java.util.logging.Logger;

import org.mapdb.HTreeMap;
import org.mapdb.serializer.SerializerInteger;

import sam.manga.samrock.meta.RecentsMeta;
import sam.nopkg.Junk;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.recents.ChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint.MinimalChapterSavePointSerilizer;
import samrock.utils.SoftListMapDBUsingMangaId;

/**
 * FIXME if possible, move it to {@link MangasDAO}
 * @author Sameer
 *
 */
class RecentChapterDao {
	private IndexedList<MinimalChapterSavePoint> list;

	public RecentChapterDao(int mangasCount) {
		this.list = new IndexedList<>(mangasCount, Logger.getLogger("RecentChapterDao#IndexedList"));
	}
	
	private final SelectSql minimal_select = new SelectSql(RECENTS_TABLE_NAME, MANGA_ID, MinimalChapterSavePoint.columnNames());
	private final SelectSql full_select = new SelectSql(RECENTS_TABLE_NAME, MANGA_ID, null);
	
	@SuppressWarnings("deprecation")
	public MinimalChapterSavePoint getSavePoint(MinimalManga manga) {
		int index = MangaManeger.indexOf(manga);
		MinimalChapterSavePoint m = list.get(index);
		
		Junk.notYetImplemented();
		// TODO FIXME load in batch
		m =  DB.executeQuery(minimal_select.create(MangaManeger.mangaIdOf(manga)), rs -> rs.next() ? new MinimalChapterSavePoint(rs) : null);
		
		if(m == null)
			return m;
		
		list.set(index, m);
		return m;
	}
	
	@SuppressWarnings("deprecation")
	public ChapterSavePoint getFullSavePoint(Manga manga) {
		MinimalChapterSavePoint m = mapdb.get(manga);
		
		if(isOfType(m, ChapterSavePoint.class))
			return (ChapterSavePoint) m;

		ChapterSavePoint c = noError(() -> samrock.executeQuery(qm().selectAllFrom(TABLE_NAME).where(w -> w.eq(MANGA_ID, manga.getMangaId())).build(), rs -> rs.next() ? new ChapterSavePoint(rs, manga.getMangaIndex()) : null));
		if(c == null) return c;
		mapdb.set(manga, c);
		return c;
	}
	
	private static final String EXITS_CHECK_SQL  = qm().select(MANGA_ID).from(TABLE_NAME).where(w -> w.eq(MANGA_ID, "", false)).build(); 
	
	public void saveSavePoint(ChapterSavePoint cs) throws SQLException {
		if(cs == null || !cs.isModified())
			return;
		
		boolean exists = samrock.executeQuery(EXITS_CHECK_SQL+cs.getMangaId(), rs -> rs.next());

		samrock.prepareStatementBlock(!exists ? ChapterSavePoint.UPDATE_SQL_NEW : ChapterSavePoint.UPDATE_SQL_OLD, ps -> {
			cs.unload(ps);
			return ps.executeUpdate();
		});
		mapdb.set(cs, cs);
	}
}
