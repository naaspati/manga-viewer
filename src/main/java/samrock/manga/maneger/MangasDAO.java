package samrock.manga.maneger;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.meta.VersioningMeta.VERSIONING_TABLE_NAME;
import static sam.sql.ResultSetHelper.getInt;
import static samrock.utils.Utils.APP_DATA;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import sam.collection.IntList;
import sam.io.serilizers.IntSerializer;
import sam.logging.MyLoggerFactory;
import sam.manga.samrock.meta.RecentsMeta;
import sam.manga.samrock.meta.VersioningMeta;
import sam.myutils.Checker;
import sam.myutils.MyUtilsPath;
import sam.myutils.ThrowException;
import sam.nopkg.Junk;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.utils.Utils;

class MangasDAO implements Closeable {
	private static final Logger LOGGER = MyLoggerFactory.logger(MangasDAO.class);

	private final IndexedReferenceList<IndexedMinimalManga> mangas;
	private final HashMap<Integer, MangaState> state = new HashMap<>();
	private final DeleteQueue deleteQueue = new DeleteQueue();

	private boolean modified;
	private final MangaIds mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id

	private final Path cache_dir = APP_DATA.resolve("manga-cache");
	private final Path MY_DIR = cache_dir.resolve(MangasDAO.class.getName());

	private Path mangasIdsCachePath() {
		return MY_DIR.resolve("manga-ids-cache.dat");
	}
	private Path minimalMangaCachePath() {
		return MY_DIR.resolve("minimal-manga-cache.dat");
	}
	public class MangaIds {
		private final int[] mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id
		private final int[] versions;

		public MangaIds(int[] manga_ids, int[] versions) {
			if(manga_ids.length != versions.length)
				throw new IllegalArgumentException();
			
			this.mangaIds = manga_ids;
			this.versions = versions;
		}
		public int length() {
			return mangaIds.length;
		}
		int indexOfMangaId(int manga_id) {
			int n = Arrays.binarySearch(mangaIds, manga_id);
			if(n < 0)
				throw new IllegalArgumentException("invalid manga_id: "+manga_id);
			
			return n;
		}
		public int getMangaId(int index) {
			return mangaIds[index];
		}
		public int size() {
			return mangaIds.length;
		}
		public int[] toArray() {
			return Arrays.copyOf(mangaIds, mangaIds.length);
		}
		MinimalManga getMinimalManga(int index) throws SQLException, IOException {
			return MangasDAO.this.getMinimalManga(index, mangaIds[index]);
		}
	}

	@SuppressWarnings("unchecked")
	public MangasDAO() throws SQLException, IOException {
		this.mangaIds = prepare();
		Files.createDirectories(MY_DIR);
		this.mangas = new IndexedReferenceList<>(mangaIds.length(), getClass());
	}

	private MangaIds prepare() throws SQLException, IOException {
		MangaIds old = loadCache();
		
		if(DB.isModified() || old == null) {
			MangaIds neww = loadDB();
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
				LOGGER.fine(() -> String.format("partial cache change: old: %s, new: %s, changed: %s", old.mangaIds.length, neww.mangaIds.length, c));
			} else {
				LOGGER.fine(() -> "DB FULL load");
				Utils.delete(cache_dir);
			}
			Files.createDirectories(cache_dir);
			return neww;
		}
		LOGGER.fine(() -> "CACHE LOADED");
		return old;
	}
	
	private MangaIds loadDB() throws SQLException {
		IntList list = new IntList(3000);
		DB.iterate(DB.selectAll(VERSIONING_TABLE_NAME), rs -> {
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
		LOGGER.fine(() -> "LOADED FROM DB");
		return new MangaIds(sorted, vdb2);
	}
	public MangaIds getMangaIds() {
		return mangaIds;
	}
	private MangaIds loadCache() throws IOException {
		Path cache = mangasIdsCachePath();
		
		if(Files.notExists(cache))
			return null;
		
		final ByteBuffer buffer = ByteBuffer.allocate(1024 * Integer.BYTES);

		try(FileChannel fc = FileChannel.open(cache, READ)) {
			MangaIds m = new MangaIds(new IntSerializer().readArray(fc, buffer), new IntSerializer().readArray(fc, buffer));
			LOGGER.fine(() -> "LOADED FROM CACHE");
			return m;
		}
		
	}

	private void writeCache() throws IOException {
		if(!modified)
			return;

		final ByteBuffer buffer = ByteBuffer.allocate((mangaIds.length() * 2 + 2) * Integer.BYTES);
		Path p = mangasIdsCachePath();

		try(FileChannel fc = FileChannel.open(p, READ, WRITE, TRUNCATE_EXISTING)) {
			new IntSerializer().write(mangaIds.mangaIds, fc, buffer);
			new IntSerializer().write(mangaIds.versions, fc, buffer);
		}
	}
	private void checkClosed() {
		if(closed)
			throw new IllegalStateException("closed");
	}

	private boolean closed;
	@Override
	public void close() throws IOException {
		checkClosed();
		
		unloadManga(currentManga);

		if(getDeleteQueue().isEmpty())
			return;

		ProcessDeleteQueue.process(getDeleteQueue());
		//TODO 
		closed = true;
		writeCache();
	}

	public DeleteQueue getDeleteQueue() {
		return deleteQueue;
	}

	private final SelectSql minimal_select = new SelectSql(MANGAS_TABLE_NAME, MANGA_ID, MinimalManga.COLUMN_NAMES());
	private final SelectSql full_select = new SelectSql(MANGAS_TABLE_NAME, MANGA_ID, null);

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

		return DB.executeQuery(minimal_select.create(manga_id), rs -> new IndexedMinimalManga(index, rs, mangaIds.versions[index]));
	}
	private void restore(MinimalManga m) {
		MangaState ms = state.get(MangaManeger.mangaIdOf(m));

		if(ms != null)
			ms.restore(m);
	}
	public void saveManga(IndexedManga m) {
		checkClosed();

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

	public IndexedManga getFullManga(IndexedMinimalManga manga) {
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
	 */
	private Manga loadManga(Object loadType, IndexedManga currentManga, MinimalManga manga) {
		if(loadType == LOAD_MOST_RECENT_MANGA && currentManga != null && currentManga.getLastReadTime() > Utils.START_UP_TIME)
			return currentManga;

		if(currentManga == manga)
			return (Manga) manga;
		if(manga != null && manga instanceof Manga)
			return (Manga) manga;

		try {
			unloadManga(currentManga);

			if(loadType == LOAD_MOST_RECENT_MANGA)
				manga = dao.getMinimalManga(db.executeQuery("SELECT "+RecentsMeta.MANGA_ID+" FROM "+RecentsMeta.TABLE_NAME+" WHERE "+RecentsMeta.TIME+" = (SELECT MAX("+LAST_READ_TIME+") FROM "+MANGAS_TABLE_NAME+")", getInt(RecentsMeta.MANGA_ID)));
			else if(loadType != LOAD_MANGA)
				throw new IllegalStateException("unknonwn loadType: "+loadType);

			currentManga = dao.getFullManga((IndexedMinimalManga) manga);
			currentSavePoint = dao.getFullSavePoint(currentManga);
			return currentManga;
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "error while loading full manga: "+manga, e);
			return null;
		}
	}

	public void  loadMostRecentManga(IndexedManga currentManga){
		loadManga(LOAD_MOST_RECENT_MANGA, currentManga, null);
	}

	private Set<Integer> deleteChapters;

	private void unloadManga(Manga mm) throws SQLException {
		if(mm == null)
			return;

		IndexedManga m = (IndexedManga) mm;
		List<Integer> deletedChapIds = m.getDeletedChaptersIds();
		if(Checker.isNotEmpty(deletedChapIds)) {
			if(deleteChapters == null)
				deleteChapters = new HashSet<>();
			deleteChapters.addAll(deletedChapIds);
			deletedChapIds.clear();
		}

		dao.saveSavePoint(currentSavePoint);
		dao.saveManga(m);

		if(!stopping.get())
			mangasOnDisplay.update(m, currentSavePoint);
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
