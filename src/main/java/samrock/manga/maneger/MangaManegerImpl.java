package samrock.manga.maneger;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import sam.manga.samrock.urls.nnew.UrlsMeta;
import sam.manga.samrock.urls.nnew.UrlsPrefixImpl;
import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import sam.reference.ReferenceUtils;
import samrock.Utils;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.api.MangaManeger;
import samrock.manga.maneger.api.Mangas;
import samrock.manga.maneger.api.Recents;
import samrock.manga.maneger.api.Tags;
import samrock.manga.recents.ChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint;
final class MangaManegerImpl implements MangaManeger {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{
		singleton.init();
	}
	
	private static final Logger logger = Utils.getLogger(MangaManegerImpl.class);

	/**
	 * Array Indices of mangas currently showing on display
	 */
	private final Mangas mangas;
	private final ThumbManager thumbManager;
	private MangasDAOImpl mangasDao;
	private RecentsImpl recents;
	private TagsImpl tags;

	private IndexedManga currentManga;
	private final AtomicBoolean stopping = new AtomicBoolean(false);

	public MangaManegerImpl() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		mangasDao = new MangasDAOImpl(new DLT());
		mangas = new MangasImpl(mangasDao);
		thumbManager = new ThumbManager(mangas.length());

		Utils.addExitTasks(() -> {
			if(stopping.get())
				return;

			stopping.set(true);

			//TODO close everything
			try {
				//TODO mangasDao.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	private DB db() {
		return Junk.notYetImplemented();//FIXME
	}
	
	private DB db0() {
		return db();
	}
	private MinimalChapterSavePoint loadSavePoint0(IndexedMinimalManga m) {
		return null; // FIXME
	}
	private ChapterSavePoint loadSavePoint0(IndexedManga m) {
		return null; // FIXME
	}
	private int indexOf(MinimalManga m) {
		return ((Indexed)m).getIndex();
	}
	private int indexOf0(MinimalManga m) {
		return indexOf(m);
	}
	
	private class IndexedMinimalMangaImpl extends IndexedMinimalManga {
		public IndexedMinimalMangaImpl(int index, ResultSet rs, int version) throws SQLException {
			super(index, rs, version);
		}

		@Override
		protected MinimalChapterSavePoint loadSavePoint() {
			return loadSavePoint0(this);
		}
		
	}
	
	private class IndexedMangaImpl extends IndexedManga {
		public IndexedMangaImpl(IndexedMinimalManga manga, ResultSet rs) throws SQLException {
			super(manga, rs);
		}
		@Override protected DB db() { return db0(); }
		@Override protected String[] parseTags(String tags) { return tagsDao().parseTags(tags); }
		@Override protected ChapterSavePoint loadSavePoint() { return loadSavePoint0(this); }
	}
	
	private class DLT extends DeleteQueueImpl {

		@Override
		protected int indexOf(MinimalManga m) {
			return indexOf0(m);
		}

		@Override
		protected MinimalManga getMangaByIndex(int index) {
			return Junk.notYetImplemented();//FIXME mangas.get(index);
		}
	}
	
	@Override
	public Manga getCurrentManga() {
		return currentManga;
	}
	@Override
	public Tags tagsDao() {
		// FIXME if(tags == null)
			// tags = MyUtilsException.noError(Tags::new);
		return tags;
	}
	@Override
	public void addMangaToDeleteQueue(Manga manga) {
		mangas.getDeleteQueue().add(manga);
	}
	@Override
	public ThumbManager getThumbManager() {
		return thumbManager;
	}
	@Override
	public MinimalManga getMinimalManga(int manga_id) throws SQLException, IOException {
		return mangasDao.getMinimalManga(manga_id);
	}

	// manga_id -> urls 
	private final HashMap<Integer, Object> urlsMap = new HashMap<>();
	private SoftReference<UrlsPrefixImpl[]> prefixesList;

	private static final String URL_SELECT = DB.selectAllQuery(UrlsMeta.URLSUFFIX_TABLE_NAME) + " WHERE "+UrlsMeta.MANGA_ID+" = ";

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public List<String> getUrls(MinimalManga manga) throws SQLException {
		Object obj = urlsMap.get(manga.manga_id);
		if(obj != null) {
			if(obj instanceof List)
				return (List<String>) obj;

			List<String> list = (List<String>) ReferenceUtils.get((WeakReference)obj);
			if(list != null)
				return list;
		}

		UrlsPrefixImpl[] m = ReferenceUtils.get(prefixesList);

		if(m == null) {
			prefixesList = new SoftReference<>(m = db().mangaUrlsPrefixes().values().toArray(new UrlsPrefixImpl[0]));
			logger.debug("LOADED: {}", UrlsPrefixImpl.class);
		}

		UrlsPrefixImpl[] ma = m;
		List<String> urls = db().executeQuery(URL_SELECT.concat(Integer.toString(manga.manga_id)),
				rs -> {
					ArrayList<String> list = new ArrayList<>();

					for (UrlsPrefixImpl u : ma) {
						String s = u.resolve(u.getColumnName()); 
						if(s != null)
							list.add(s);
					}

					if(list.isEmpty())
						return Collections.emptyList();
					else {
						list.trimToSize();
						return Collections.unmodifiableList(list);
					}
				});

		if(urls.isEmpty())
			urlsMap.put(manga.manga_id, urls);
		else 
			urlsMap.put(manga.manga_id, new WeakReference<>(urls));

		logger.debug("Urls loaded: ({}), manga_id: {}", urls.size(), manga.manga_id); 
		return urls;
	}
	
	@Override
	public int getMangasCount() {
		return mangas.length();
	}
	@Override
	public void loadMostRecentManga() {
		mangasDao.loadMostRecentManga((IndexedManga) mangas.current());
	}
	@Override
	public Mangas mangas() {
		return mangas;
	}
	@Override
	public Recents recentsDao() {
		// FIXME if(recents == null)
			// recents = new RecentsImpl(mangas.length());
		return recents;
	}
}
