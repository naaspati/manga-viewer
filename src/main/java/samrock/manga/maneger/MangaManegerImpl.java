package samrock.manga.maneger;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

import sam.manga.samrock.chapters.ChaptersMeta;
import sam.manga.samrock.urls.nnew.UrlsMeta;
import sam.manga.samrock.urls.nnew.UrlsPrefixImpl;
import sam.myutils.MyUtilsException;
import sam.nopkg.Junk;
import sam.reference.ReferenceUtils;
import sam.reference.WeakAndLazy;
import samrock.Utils;
import samrock.manga.Chapter;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
final class MangaManegerImpl implements IMangaManeger {
	private static final Logger logger = Utils.getLogger(MangaManeger.class);

	/**
	 * Array Indices of mangas currently showing on display
	 */
	private final Mangas mangas;
	private final ThumbManager thumbManager;
	private MangasDAO mangasDao;
	private RecentsDao recents;
	private TagsDAO tags;

	private IndexedManga currentManga;
	private final AtomicBoolean stopping = new AtomicBoolean(false);

	public MangaManegerImpl() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		mangasDao = new MangasDAO();
		mangas = new Mangas(mangasDao);
		thumbManager = new ThumbManager(mangas.length());

		Utils.addExitTasks(() -> {
			if(stopping.get())
				return;

			stopping.set(true);

			//TODO close everything
			try {
				mangasDao.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	@Override
	public Manga getCurrentManga() {
		return currentManga;
	}
	@Override
	public TagsDAO tagsDao() {
		if(tags == null)
			tags = MyUtilsException.noError(TagsDAO::new);
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
	private final String CHAPTERS_SELECT = "SELECT * FROM "+ChaptersMeta.CHAPTERS_TABLE_NAME+ " WHERE "+ChaptersMeta.MANGA_ID + " = ";

	@Override
	public List<Chapter> getChapters(Manga m) throws SQLException {
		Objects.requireNonNull(m);
		IndexedManga manga = (IndexedManga)m;
		return DB.collectToList(CHAPTERS_SELECT.concat(String.valueOf(manga.getMangaId())), manga::newChapter);
	}

	// manga_id -> urls 
	private final HashMap<Integer, Object> urlsMap = new HashMap<>();
	private SoftReference<UrlsPrefixImpl[]> prefixesList;

	private static final String URL_SELECT = DB.selectAll(UrlsMeta.URLSUFFIX_TABLE_NAME) + " WHERE "+UrlsMeta.MANGA_ID+" = ";

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public List<String> getUrls(MinimalManga manga) throws SQLException {
		Object obj = urlsMap.get(mangaIdOf(manga));
		if(obj != null) {
			if(obj instanceof List)
				return (List<String>) obj;

			List<String> list = (List<String>) ReferenceUtils.get((WeakReference)obj);
			if(list != null)
				return list;
		}

		UrlsPrefixImpl[] m = ReferenceUtils.get(prefixesList);

		if(m == null) {
			prefixesList = new SoftReference<>(m = DB.mangaUrlsPrefixes().values().toArray(new UrlsPrefixImpl[0]));
			logger.fine(() -> "LOADED: "+UrlsPrefixImpl.class);
		}

		UrlsPrefixImpl[] ma = m;
		List<String> urls = DB.executeQuery(URL_SELECT.concat(Integer.toString(mangaIdOf(manga))),
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
			urlsMap.put(mangaIdOf(manga), urls);
		else 
			urlsMap.put(mangaIdOf(manga), new WeakReference<>(urls));

		logger.fine(() -> "Urls loaded: ("+urls.size()+"), manga_id: "+mangaIdOf(manga)); 
		return urls;
	}
	
	@Override
	public List<Chapter> loadChapters(IndexedManga manga) {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
	@Override
	public List<Chapter> reloadChapters(IndexedManga manga, List<Chapter> loadedChapters) {
		// TODO Auto-generated method stub
		return Junk.notYetImplemented();
	}
	@Override
	public int indexOfMangaId(int manga_id) {
		return mangasDao.indexOfMangaId(manga_id);
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
	public RecentsDao recentsDao() {
		if(recents == null)
			recents = new RecentsDao(mangas.length());
		return recents;
	}
	private WeakAndLazy<SearchManeger> search = new WeakAndLazy<>(SearchManeger::new);
	
	@Override
	public SearchManeger searchManager(boolean create) {
		return search.get();
	}
}
