import static sam.manga.samrock.mangas.MangasMeta.CHAPTER_ORDERING;
import static sam.manga.samrock.mangas.MangasMeta.CHAP_COUNT_PC;
import static sam.manga.samrock.mangas.MangasMeta.IS_FAVORITE;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.READ_COUNT;
import static sam.manga.samrock.mangas.MangasMeta.STARTUP_VIEW;
import static sam.manga.samrock.mangas.MangasMeta.UNREAD_COUNT;
import static sam.sql.querymaker.QueryMaker.qm;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import sam.collection.Iterables;
import sam.manga.samrock.SamrockDB;
import sam.manga.samrock.chapters.ChapterUtils;
import sam.manga.samrock.chapters.ChaptersMeta;
import sam.manga.samrock.meta.VersioningMeta;
import sam.myutils.Checker;
import sam.sql.querymaker.QueryMaker;
import samrock.manga.Chapters;
import samrock.manga.Manga;
import samrock.manga.Manga.Chapter;

public static class MangaSql {
		private static final String UPDATE_SQL = QueryMaker.getInstance().update(MANGAS_TABLE_NAME).placeholders(
				IS_FAVORITE,
				LAST_READ_TIME,
				STARTUP_VIEW,
				CHAPTER_ORDERING,
				CHAP_COUNT_PC,
				READ_COUNT,
				UNREAD_COUNT)
				.where(w -> w.eqPlaceholder(MANGA_ID)).build();

		private static final String GET_VERSION_SQL = qm().select(VersioningMeta.VERSION).from(VersioningMeta.TABLE_NAME).build() + " WHERE "+VersioningMeta.MANGA_ID+"=";

		private static final int IS_FAVORITE_N        = 1;
		private static final int LAST_READ_TIME_N     = 2;
		private static final int STARTUP_VIEW_N       = 3;
		private static final int CHAPTER_ORDERING_N   = 4;
		private static final int CHAP_COUNT_PC_N      = 5;
		private static final int READ_COUNT_N         = 6;
		private static final int UNREAD_COUNT_N       = 7;
		private static final int MANGA_ID_N           = 8;

		/**
		 * sets all corresponding values to the given PreparedStatement and adds it to batch
		 * <br><b>note: doesn't execute the PreparedStatement only PreparedStatement.addBatch() is called</b>
		 * <br> and sets chapters = null  
		 * @param mangaUpdate
		 * @return new_manga_version number
		 * @throws SQLException
		 * @throws IOException
		 */
		public int save(Manga m, SamrockDB db) throws SQLException {
			if(!m.isModified())
				return m.version;

			try(PreparedStatement ps = db.prepareStatement(UPDATE_SQL)) {
				ps.setBoolean(IS_FAVORITE_N, m.isFavorite());
				ps.setLong(LAST_READ_TIME_N, m.lastReadTime);
				ps.setInt(STARTUP_VIEW_N, m.startupView.index());
				ps.setBoolean(CHAPTER_ORDERING_N, m.chapters.isChaptersInIncreasingOrder());
				ps.setInt(CHAP_COUNT_PC_N, m.getChapCountPc());
				ps.setInt(READ_COUNT_N, m.getReadCount());
				ps.setInt(UNREAD_COUNT_N, m.unreadCount);
				ps.setInt(MANGA_ID_N, m.manga_id);
				ps.executeUpdate();
			}
			if(m.chapters.isLoaded())
				unload(db, m.chapters);

			db.commit();
			int version = db.executeQuery(GET_VERSION_SQL+m.manga_id, rs -> rs.getInt(1));
			m.setVersion(version);
			LOGGER.debug("manga saved manga_id: {}, version:{} ", m.manga_id, version);
			return version;
		}
		private void unload(SamrockDB db, Chapters ch) throws SQLException {
			ChapterUtils utils = new ChapterUtils(db);
			StringBuilder sb = commitChaptersChanges(utils, ch.getManga().manga_id, Iterables.map(ch, c -> (sam.manga.samrock.chapters.Chapter)c));
			LOGGER.debug("manga_id: {}, chapters changes: {}",ch.getManga().manga_id, sb);
			List<Chapter> deleted = ch.getDeletedChapters();

			if(!Checker.isEmpty(deleted )) {
				int n = db.executeUpdate(qm().deleteFrom(ChaptersMeta.CHAPTERS_TABLE_NAME).where(w -> w.in(ChaptersMeta.CHAPTER_ID, Iterables.map(deleted, Chapter::getChapterId))).build());
				LOGGER.debug("manga_id: {}, chapters deleted: {}",ch.getManga().manga_id, n);	
			}
			ch.unloaded();
		}
	}
	