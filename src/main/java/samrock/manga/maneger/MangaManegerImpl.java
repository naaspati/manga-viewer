package samrock.manga.maneger;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.sql.ResultSetHelper.getInt;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import sam.logging.MyLoggerFactory;
import sam.manga.samrock.chapters.ChaptersMeta;
import sam.manga.samrock.mangas.MangaUtils;
import sam.manga.samrock.meta.RecentsMeta;
import sam.myutils.Checker;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.recents.ChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint;
import samrock.utils.Utils;
final class MangaManegerImpl implements IMangaManeger {
	private static final Logger logger = MyLoggerFactory.logger(MangaManeger.class);

	/**
	 * Array Indices of mangas currently showing on display
	 */
	private final MangasOnDisplay mangasOnDisplay;
	private final Dao dao;
	private final ThumbManager thumbManager;

	private IndexedManga currentManga;
	private ChapterSavePoint currentSavePoint;
	private final AtomicBoolean stopping = new AtomicBoolean(false);

	public MangaManegerImpl() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		dao = new Dao();
		mangasOnDisplay = new MangasOnDisplay(this, dao);
		thumbManager = new ThumbManager(dao.mangasCount());

		Utils.addExitTasks(() -> {
			if(stopping.get())
				return;

			stopping.set(true);
			mangasOnDisplay.clearListeners();
			unloadManga(currentManga);
			processDeleteQueue();
			try {
				dao.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	@Override
	public Manga getCurrentManga() {
		return currentManga;
	}
	public ChapterSavePoint getCurrentSavePoint() {
		return currentSavePoint;
	}

	@Override
	public String parseTags(Manga manga, Collection<Integer> colortags) {
		int[] array = MangaUtils.tagsToIntArray(manga.getTags());
		if(array.length == 0)
			return "";

		StringBuilder sb = new StringBuilder();

		for (int index : array) {
			String tag = dao.getTag(index);
			if(colortags.contains(index))
				sb.append("<span bgcolor=red>").append(tag).append("</span>").append(", ");
			else
				sb.append(tag).append(", ");
		}

		while(sb.length() != 0 && (sb.charAt(sb.length() - 1) == ' ' || sb.charAt(sb.length() - 1) == ','))
			sb.setLength(sb.length() - 1);

		return sb.toString();
	}

	/**
	 * @return mangas.length
	 */
	@Override
	public int getMangasCount() {
		return dao.mangasCount();
	}

	private static final Object LOAD_MOST_RECENT_MANGA = new Object();
	private static final Object LOAD_MANGA = new Object();

	/**
	 * load corresponding manga, ChapterSavePoint and set to currentManga and currentSavePoint  
	 * @param arrayIndex
	 */
	private Manga loadManga(Object loadType, MinimalManga manga) {
		if(loadType != LOAD_MOST_RECENT_MANGA && manga == null)
			throw new NullPointerException("manga == null");

		if(currentManga == manga)
			return (Manga) manga;
		if(manga != null && manga instanceof Manga)
			return (Manga) manga;

		if(loadType == LOAD_MOST_RECENT_MANGA && currentManga != null && currentManga.getLastReadTime() > Utils.START_UP_TIME)
			return currentManga;

		try {
			DB db = dao.samrock();
			unloadManga(currentManga);

			if(loadType == LOAD_MOST_RECENT_MANGA)
				manga = dao.getMinimalManga(db.executeQuery("SELECT "+RecentsMeta.MANGA_ID+" FROM "+RecentsMeta.TABLE_NAME+" WHERE "+RecentsMeta.TIME+" = (SELECT MAX("+LAST_READ_TIME+") FROM "+MANGAS_TABLE_NAME+")", getInt(RecentsMeta.MANGA_ID)));
			else if(loadType != LOAD_MANGA)
				throw new IllegalStateException("unknonwn loadType: "+loadType);

			currentManga = dao.getFullManga((IndexedMinimalManga) manga);
			currentSavePoint = dao.getFullSavePoint(currentManga);
			return currentManga;
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "error while loading full manga: "+manga, e);
			return null;
		}
	}

	@Override
	public void  loadMostRecentManga(){
		loadManga(LOAD_MOST_RECENT_MANGA, null);
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

	private void processDeleteQueue() {
		if(mangasOnDisplay.getDeleteQueue().isEmpty())
			return;
		new ProcessDeleteQueue().process(mangasOnDisplay.getDeleteQueue(), dao);
	}
	/**
	 * 
	 * @return a <b>copy</b> of mangasOnDisplay
	 */
	@Override
	public MangasOnDisplay getMangasOnDisplay() {
		return mangasOnDisplay;
	}
	@Override
	public MinimalChapterSavePoint getChapterSavePoint(MinimalManga manga) {
		return dao.getSavePoint(manga);
	}
	@Override
	public DB samrock() {
		return dao.samrock();
	}
	@Override
	public TagsDAO getTagDao() {
		return dao.tagsDAO();
	}
	@Override
	public void addMangaToDeleteQueue(Manga manga) {
		mangasOnDisplay.getDeleteQueue().add(manga);
	}
	@Override
	public ThumbManager getThumbManager() {
		return thumbManager;
	}
	@Override
	public MinimalManga getMinimalManga(int manga_id) {
		return dao.getMinimalManga(manga_id);
	}

	private final String CHAPTERS_SELECT = "SELECT * FROM "+ChaptersMeta.CHAPTERS_TABLE_NAME+ " WHERE "+ChaptersMeta.MANGA_ID + " = ";

	@Override
	public List<Chapter> getChapters(Manga m) throws SQLException {
		Objects.requireNonNull(m);
		IndexedManga manga = (IndexedManga)m;
		return dao.samrock().collectToList(CHAPTERS_SELECT.concat(String.valueOf(manga.getMangaId())), manga::newChapter);
	}
}
