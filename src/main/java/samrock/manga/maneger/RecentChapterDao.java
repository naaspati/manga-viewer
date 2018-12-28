package samrock.manga.maneger;

import static sam.manga.samrock.meta.RecentsMeta.MANGA_ID;
import static sam.manga.samrock.meta.RecentsMeta.TABLE_NAME;
import static sam.myutils.Checker.isOfType;
import static sam.myutils.MyUtilsException.noError;
import static sam.sql.querymaker.QueryMaker.qm;

import java.sql.SQLException;

import org.mapdb.HTreeMap;
import org.mapdb.serializer.SerializerInteger;

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
@Deprecated
class RecentChapterDao {
	SoftListMapDBUsingMangaId<MinimalChapterSavePoint> mapdb;

	public RecentChapterDao() {
		this.samrock = dao.samrock();
		HTreeMap<Integer, MinimalChapterSavePoint> temp = mapdb.hashMap(getClass().getSimpleName()+"recents", new SerializerInteger(), new MinimalChapterSavePointSerilizer(dao::indexOfMangaId)).createOrOpen();
		this.mapdb = new SoftListMapDBUsingMangaId<>(dao.mangasCount(), temp);
	}
	@SuppressWarnings("deprecation")
	public MinimalChapterSavePoint getSavePoint(MinimalManga manga) {
		MinimalChapterSavePoint m = mapdb.get(manga);
		
		m =  noError(() -> samrock.executeQuery(qm().select(MinimalChapterSavePoint.COLUMNS_NAMES).from(TABLE_NAME).where(w -> w.eq(MANGA_ID, manga.getMangaId())).build(), rs -> rs.next() ? new MinimalChapterSavePoint(rs, manga.getMangaIndex()) : null));
		
		if(m == null)
			return m;
		
		mapdb.set(manga, m);
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
