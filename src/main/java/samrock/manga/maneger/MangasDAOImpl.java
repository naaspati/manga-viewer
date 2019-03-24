package samrock.manga.maneger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.meta.VersioningMeta.VERSIONING_TABLE_NAME;
import static sam.sql.ResultSetHelper.getInt;
import static samrock.Utils.APP_DATA;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;

import sam.collection.IntList;
import sam.io.IOUtils;
import sam.manga.samrock.meta.RecentsMeta;
import sam.manga.samrock.meta.VersioningMeta;
import sam.myutils.ThrowException;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import sam.nopkg.Resources;
import samrock.Utils;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.api.DeleteQueue;
import samrock.manga.maneger.api.MangaIds;
import samrock.manga.maneger.api.MangasDAO;

abstract class MangasDAOImpl implements AutoCloseable, MangasDAO {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{
		singleton.init();
	}
	private static final Logger LOGGER = Utils.getLogger(MangasDAOImpl.class);

	private final IndexedReferenceList<IndexedMinimalManga> mangas;
	private final HashMap<Integer, MangaState> state = new HashMap<>();
	private final DeleteQueue deleteQueue;

	private boolean modified;
	private final MangaIdsImpl mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id

	private final Path cache_dir = APP_DATA.resolve("manga-cache");
	private final Path MY_DIR = cache_dir.resolve(MangasDAO.class.getName());

	private Path mangasIdsCachePath() {
		return MY_DIR.resolve("manga-ids-cache.dat");
	}
	private Path minimalMangaCachePath() {
		return MY_DIR.resolve("minimal-manga-cache.dat");
	}
	public class MangaIdsImpl implements MangaIds {
		private final int[] mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id
		private final int[] versions;

		public MangaIdsImpl(int[] manga_ids, int[] versions) {
			if(manga_ids.length != versions.length)
				throw new IllegalArgumentException();

			this.mangaIds = manga_ids;
			this.versions = versions;
		}
		public int indexOfMangaId(int manga_id) {
			int n = Arrays.binarySearch(mangaIds, manga_id);
			if(n < 0)
				throw new IllegalArgumentException("invalid manga_id: "+manga_id);

			return n;
		}
		int getMangaId(int index) {
			return mangaIds[index];
		}
		public int size() {
			return mangaIds.length;
		}
		public int[] toArray() {
			return Arrays.copyOf(mangaIds, mangaIds.length);
		}
	}

	@SuppressWarnings("unchecked")
	public MangasDAOImpl(DeleteQueue deleteQueue) throws SQLException, IOException {
		this.deleteQueue = deleteQueue;
		this.mangaIds = prepare();
		Files.createDirectories(MY_DIR);
		this.mangas = new IndexedReferenceList<>(mangaIds.size(), getClass());
	}

	protected abstract DB db();
	protected abstract IndexedMinimalManga indexedMinimalManga(int index, ResultSet rs, int version) throws SQLException;
	protected abstract IndexedManga currentManga();

	private MangaIdsImpl prepare() throws SQLException, IOException {
		MangaIdsImpl old = loadCache();

		if(db().isModified() || old == null) {
			MangaIdsImpl neww = loadDB();
			if(old != null) {
				int size = Math.min(old.mangaIds.length, neww.mangaIds.length);
				int[] version  = neww.versions;
				int changed = 0;

				for (int i = 0; i < size; i++) {
					if(old.mangaIds[i] != neww.mangaIds[i]) {
						version[i] = -1;
						changed++;
					}
				}
				int c = changed;
				LOGGER.debug("partial cache change: old: {}, new: {}, changed: {}", old.mangaIds.length, neww.mangaIds.length, c);
			} else {
				LOGGER.debug("DB FULL load");
				Utils.delete(cache_dir);
			}
			Files.createDirectories(cache_dir);
			return neww;
		}
		LOGGER.debug("CACHE LOADED");
		return old;
	}

	private MangaIdsImpl loadDB() throws SQLException {
		IntList list = new IntList(3000);
		db().iterate(DB.selectAllQuery(VERSIONING_TABLE_NAME), rs -> {
			list.add(rs.getInt(VersioningMeta.MANGA_ID));
			list.add(rs.getInt(VersioningMeta.VERSION));
		});

		int[] mids = new int[list.size()/2];
		int[] version = new int[list.size()/2];

		int n = 0;
		int i = 0;
		while(n < list.size()) {
			mids[i] = list.get(n++);
			version[i++] = list.get(n++);
		}

		int[] sorted = Arrays.copyOf(mids, mids.length);
		Arrays.sort(sorted);

		int[] vdb2 = new int[version.length];
		for (int j = 0; j < version.length; j++) {
			int index = Arrays.binarySearch(sorted, mids[j]);
			vdb2[index] = version[j];
		}
		LOGGER.debug("LOADED FROM DB");
		return new MangaIdsImpl(sorted, vdb2);
	}
	@Override
	public MangaIdsImpl getMangaIds() {
		return mangaIds;
	}
	private MangaIdsImpl loadCache() throws IOException {
		Path cache = mangasIdsCachePath();

		if(Files.notExists(cache))
			return null;

		try(FileChannel fc = FileChannel.open(cache, READ);
				Resources r = Resources.get();) {
			ByteBuffer buf = r.buffer();
			IOUtils.read(buf, false, fc);

			if(buf.remaining() < 4)
				return null;

			int size = buf.getInt();

			int[] ids = new int[size];
			int[] versions = new int[size];
			
			for (int i = 0; i < size; i++) {
				if(buf.remaining() < 8) {
					IOUtils.compactOrClear(buf);
					if(IOUtils.read(buf, false, fc) < 8)
						throw new IOException("underflow");
				}
				
				ids[i] = buf.getInt();
				versions[i] = buf.getInt();
			}

			MangaIdsImpl m = new MangaIdsImpl(ids, versions);
			LOGGER.debug("LOADED FROM CACHE: {}", size);
			return m;
		}
	}

	private void writeCache() throws IOException {
		if(!modified)
			return;

		Path p = mangasIdsCachePath();

		try(FileChannel fc = FileChannel.open(p, CREATE, WRITE, TRUNCATE_EXISTING);
				Resources r = Resources.get();) {
			ByteBuffer buf = r.buffer();

			int[] ids = mangaIds.mangaIds;
			int[] version = mangaIds.versions;

			buf.putInt(ids.length);

			for (int i = 0; i < version.length; i++) {
				if(buf.remaining() < 8) 
					IOUtils.write(buf, fc, true);
				
				buf.putInt(ids[i]);
				buf.putInt(version[i]);
			}

			IOUtils.write(buf, fc, true);
		}
	}
	private void checkClosed() {
		if(closed)
			throw new IllegalStateException("closed");
	}

	private boolean closed;
	@Override
	public void close() throws IOException, SQLException {
		checkClosed();

		unloadManga(currentManga());

		if(getDeleteQueue().isEmpty())
			return;

		ProcessDeleteQueue.process(getDeleteQueue(), db());
		//TODO 
		closed = true;
		writeCache();
	}
	
	@Override
	public DeleteQueue getDeleteQueue() {
		return deleteQueue;
	}

	private final SelectSql minimal_select = new SelectSql(MANGAS_TABLE_NAME, MANGA_ID, IndexedMinimalManga.columnNames());
	private final SelectSql full_select = new SelectSql(MANGAS_TABLE_NAME, MANGA_ID, null);

	@Override
	public IndexedMinimalManga getMinimalManga(int manga_id) throws SQLException, IOException {
		return getMinimalManga(mangaIds.indexOfMangaId(manga_id), manga_id);
	}
	private IndexedMinimalManga getMinimalManga(int index, int manga_id) throws SQLException, IOException  {
		checkClosed();
		IndexedMinimalManga m = mangas.get(index);

		if(m != null)
			return m;

		return loadManga(manga_id, index);
	}
	private IndexedMinimalManga loadManga(int manga_id, int index) throws SQLException {
		Junk.notYetImplemented();
		// FIXME load atleast 10 mangas when access DB		
		// -- load mangas following this mangas (which are not loaded)
		// implement batch_size config to load number of mangas to load at once
		// -- if version == -1, load manga directly from db, without looking in file_cache

		IndexedMinimalManga m = null;

		mangas.set(index, m);
		restore(m);

		return db().executeQuery(minimal_select.where_equals(manga_id), rs -> indexedMinimalManga(index, rs, mangaIds.versions[index]));
	}
	
	private void restore(MinimalManga m) {
		MangaState ms = state.get(m.manga_id);

		if(ms != null)
			ms.restore(m);
	}
	@Override
	public void saveManga(Manga manga) {
		checkClosed();
		
		IndexedManga m = (IndexedManga) manga;

		if(m == null || !m.isModified())
			return;

		modified = true;
		state.put(m.getMangaId(), new MangaState(m));
	}

	private static final class MangaState {
		private final boolean favorite;
		private final long lastReadTime;
		private final int readCount, unreadCount, version;

		public MangaState(IndexedManga m) {
			favorite =  m.isFavorite();
			lastReadTime = m.getLastReadTime();
			readCount = m.getReadCount();
			unreadCount = m.getUnreadCount();
			version = m.getVersion(); 
		}

		public void restore(MinimalManga mm) {
			Objects.requireNonNull(mm);

			if(mm instanceof IndexedManga) {
				IndexedManga m = (IndexedManga) mm;

				if(m.getVersion() < version) {
					m.setFavorite(favorite);
					m.setLastReadTime(lastReadTime);
					m.setReadCount(readCount);
					m.setUnreadCount(unreadCount);
					m.setVersion(version);
				}
			} else if(mm instanceof IndexedMinimalManga) {
				IndexedMinimalManga m = (IndexedMinimalManga) mm;

				if(m.getVersion() < version) {
					m.setUnreadCount(unreadCount);
					m.setVersion(version);
				}	
			} else {
				ThrowException.illegalArgumentException("bad value: "+mm.getClass());
			}
		}
	}

	@Override
	public IndexedManga getFullManga(MinimalManga manga) {
		/*
		 * MinimalManga ms = mangas.get(index);
		if(Checker.isOfType(ms, Manga.class))
			return (Manga) ms;

		int manga_id = mangaIdAt(index);
		Manga m = noError(() -> samrock.executeQuery(LOAD_ONE_Manga_SQL+manga_id, rs -> new Manga(samrock, rs, index, version(index))));
		mangas.set(m);
		return m;
		 */
		// TODO Auto-generated method stub

		IndexedManga m = null; 

		restore(m);
		return Junk.notYetImplemented();
	}
	public int indexOfMangaId(int manga_id) {
		return mangaIds.indexOfMangaId(manga_id);
	}

	private static final Object LOAD_MOST_RECENT_MANGA = new Object();
	private static final Object LOAD_MANGA = new Object();

	/**
	 * load corresponding manga, ChapterSavePoint and set to currentManga and currentSavePoint  
	 * @param arrayIndex
	 * @throws IOException 
	 */
	private Manga loadManga(Object loadType, MinimalManga manga) throws IOException, SQLException  {
		IndexedManga currentManga = currentManga();
		if(loadType == LOAD_MOST_RECENT_MANGA && currentManga != null && currentManga.getLastReadTime() > Utils.START_UP_TIME)
			return currentManga;

		if(currentManga == manga)
			return (Manga) manga;
		if(manga != null && manga instanceof Manga)
			return (Manga) manga;
		
		unloadManga(currentManga);

		if(loadType == LOAD_MOST_RECENT_MANGA)
			manga = getMinimalManga(db().executeQuery("SELECT "+RecentsMeta.MANGA_ID+" FROM "+RecentsMeta.RECENTS_TABLE_NAME+" WHERE "+RecentsMeta.TIME+" = (SELECT MAX("+LAST_READ_TIME+") FROM "+MANGAS_TABLE_NAME+")", getInt(RecentsMeta.MANGA_ID)));
		else if(loadType != LOAD_MANGA)
			throw new IllegalStateException("unknonwn loadType: "+loadType);

		return getFullManga(manga);
	}
	
	@Override
	public MinimalManga getMinimalMangaByIndex(int index) throws IOException, SQLException {
		return getMinimalManga(index, mangaIds.getMangaId(index));
	}
	
	@Override
	public void  loadMostRecentManga() throws IOException, SQLException {
		loadManga(LOAD_MOST_RECENT_MANGA, null);
	}

	private Set<Integer> deleteChapters;

	private void unloadManga(IndexedManga m) throws SQLException {
		if(m == null)
			return;
		
		/** FIXME
		 * 
		List<Integer> deletedChapIds = m.getDeletedChaptersIds();
		if(Checker.isNotEmpty(deletedChapIds)) {
			if(deleteChapters == null)
				deleteChapters = new HashSet<>();
			deleteChapters.addAll(deletedChapIds);
			deletedChapIds.clear();
		}

		dao().saveSavePoint(currentSavePoint);
		dao().saveManga(m);

		if(!stopping.get())
			mangasOnDisplay.update(m, currentSavePoint);
		 */

	}


	/*
	 	private MinimalManga get(final int index, final int manga_id) {
		MinimalManga m = mangas.get(index, manga_id);

		if(m != null)
			return versionChecked(index, m);

		return loadMinimalManga(index);
	}
	private MinimalManga versionChecked(int index, MinimalManga m) {
		if(version(index) != m.getVersion() )
			return loadMinimalManga(index);
		return m;
	}
	 */

}
