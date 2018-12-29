package samrock.manga.maneger;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.meta.VersioningMeta.VERSIONING_TABLE_NAME;
import static sam.sql.querymaker.QueryMaker.qm;
import static samrock.utils.Utils.APP_DATA;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import sam.collection.IntList;
import sam.logging.MyLoggerFactory;
import sam.manga.samrock.meta.VersioningMeta;
import sam.myutils.ThrowException;
import sam.nopkg.Junk;
import sam.reference.ReferenceUtils;
import sam.sql.JDBCHelper;
import samrock.manga.MinimalManga;
import samrock.utils.Utils;

class MangasDAO implements Closeable {
	private static final Logger LOGGER = MyLoggerFactory.logger(MangasDAO.class);

	private final SoftReference<IndexedMinimalManga>[] mangas;
	private final HashMap<Integer, MangaState> state = new HashMap<>();
	private final DeleteQueue deleteQueue = new DeleteQueue();

	private boolean modified;
	private final MangaIds mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id
	private final boolean db_modified = DB.isModified();

	private final Path MY_DIR = APP_DATA.resolve(MangasDAO.class.getName());

	private Path cachePath() {
		return MY_DIR.resolve("cache.dat");
	}
	private Path minimalMangaCachePath() {
		return MY_DIR.resolve("minimal-manga-cache.dat");
	}

	public static class MangaIds {
		private final int[] mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id
		private final int[] versions;

		public MangaIds(int[] manga_ids, int[] versions) {
			this.mangaIds = manga_ids;
			this.versions = versions;
		}
		public int length() {
			return mangaIds.length;
		}
		private int indexOfMangaId(int manga_id) {
			return Arrays.binarySearch(mangaIds, manga_id);
		}
		public int getMangaId(int index) {
			return mangaIds[index];
		}
		public int size() {
			return mangaIds.length;
		}
	}

	@SuppressWarnings("unchecked")
	public MangasDAO() throws SQLException, IOException {
		MangaIds cache = loadCache();

		if(db_modified || cache.mangaIds == null) {
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

			if(Arrays.equals(sorted, cache.mangaIds)) {
				StringBuilder sb = LOGGER.isLoggable(Level.FINE) ? new StringBuilder() : null;
				Formatter formatter = sb == null ? null : new Formatter(sb);
				String format = "%-10s%-10s -> %s\n";
				if(formatter != null)
					formatter.format(format, "manga_id", "old_ver", "new_ver");
				int sblen = sb == null ? -1 : sb.length();

				if(cache.versions != null) {
					int modified = 0;

					for (int j = 0; j < sorted.length; j++) {
						if(vdb2[j] != cache.versions[j]) {
							if(formatter != null) 
								formatter.format(format, sorted[j], cache.versions[j], vdb2[j]);
							modified++;
							cache.versions[j] = -1;
						}
					}

					if(modified == sorted.length) 
						LOGGER.fine("ALL("+modified+") MODIFIED");
					else if(sb != null && sb.length() != sblen)
						LOGGER.fine(sb.toString());
				}
			} else {
				Utils.delete(cachePath(), LOGGER);
				// FIXME -- recache rather than deleting
				Utils.delete(minimalMangaCachePath(), LOGGER);
				LOGGER.fine(() -> "FULL load from db: "+mids.length);

				cache = new MangaIds(sorted, vdb2);
			}
		}

		this.mangaIds = Objects.requireNonNull(cache);
		Objects.requireNonNull(cache.mangaIds);
		Objects.requireNonNull(cache.versions);
		this.mangas = new SoftReference[mangaIds.length()];
	}

	public MangaIds getMangaIds() {
		return mangaIds;
	}
	private MangaIds loadCache() throws IOException {
		Path cache = cachePath();

		if(Files.notExists(cache))
			return new MangaIds(null, null);

		final ByteBuffer buffer = ByteBuffer.allocate(1024 * Integer.BYTES);

		try(FileChannel fc = FileChannel.open(cache, READ)) {
			fc.read(buffer);
			buffer.flip();

			int size = buffer.getInt();
			int[] mids = new int[size];
			int[] version = new int[size];

			for (int i = 0; i < size; i++) {
				if(buffer.remaining() < Integer.BYTES * 2) {
					if(buffer.hasRemaining())
						buffer.compact();
					else
						buffer.clear();

					fc.read(buffer);
					buffer.flip();
				}
				mids[i] = buffer.getInt();
				version[i] = buffer.getInt();
			}
			return new MangaIds(mids, version);
		}
	}

	private void writeCache() throws IOException {
		if(!modified)
			return;

		final ByteBuffer buffer = ByteBuffer.allocate((mangaIds.length() * 2 + 2) * Integer.BYTES);
		int size = mangaIds.length();
		buffer.putInt(size);

		for (int i = 0; i < size; i++) {
			buffer.putInt(mangaIds.mangaIds[i]);
			buffer.putInt(mangaIds.versions[i]);
		}

		buffer.flip();
		int size2 = buffer.remaining();
		Path p = cachePath();

		try(FileChannel fc = FileChannel.open(p, READ, WRITE, TRUNCATE_EXISTING)) {
			while(buffer.hasRemaining())
				fc.write(buffer);
		}
		LOGGER.fine(() -> "write bytes:"+size2+": "+Utils.subpath(p));
	}
	private void checkClosed() {
		if(closed)
			throw new IllegalStateException("closed");
	}

	private boolean closed;
	@Override
	public void close() throws IOException {
		checkClosed();

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

	public IndexedMinimalManga getMinimalManga(int manga_id) throws SQLException {
		checkClosed();

		int index = mangaIds.indexOfMangaId(manga_id);
		IndexedMinimalManga m = ReferenceUtils.get(mangas[index]);

		if(m != null)
			return m;
		
		m = loadManga(manga_id, index);

		mangas[index] = new SoftReference<>(m);
		restore(m);
		return m;
	}
	private IndexedMinimalManga loadManga(int manga_id, int index) throws SQLException {
		Junk.notYetImplemented();
		// FIXME load atleast 10 mangas when access DB		
		// -- load mangas following this mangas (which are not loaded)
		// implement batch_size config to load number of mangas to load at once
		
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
