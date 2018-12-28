package samrock.manga.maneger;

import java.util.Collection;
import java.util.List;

import sam.myutils.ThrowException;
import samrock.manga.Manga;
import samrock.manga.Manga.Chapter;
import samrock.manga.MinimalManga;
import samrock.manga.recents.MinimalChapterSavePoint;

public class MangaManeger {
	private static volatile boolean init = false;
	private static IMangaManeger instance;

	public static void init() throws Exception  {
		synchronized (MangaManeger.class) {
			if(init)
				throw new IllegalStateException("already initialized");
			init0(new MangaManegerImpl());
		}
	}
	public static void init(IMangaManeger manager) {
		synchronized (MangaManeger.class) {
			if(init)
				throw new IllegalStateException("already initialized");
			init0(manager);
		}
	}
	private static void init0(IMangaManeger manager) {
		instance = manager;
		init = true;
	}
	public static int getMangasCount() {
		return instance.getMangasCount();
	}
	public static void loadMostRecentManga() {
		instance.loadMostRecentManga();
	}
	public static MangasOnDisplay getMangasOnDisplay() {
		return instance.getMangasOnDisplay();
	}
	public static MinimalChapterSavePoint getChapterSavePoint(MinimalManga manga) {
		return instance.getChapterSavePoint(manga);
	}
	public static DB samrock() {
		return instance.samrock();
	}
	public static TagsDAO getTagDao() {
		return instance.getTagDao();
	}
	public static void addMangaToDeleteQueue(Manga manga) {
		instance.addMangaToDeleteQueue(manga);
	}
	public static ThumbManager getThumbManager() {
		return instance.getThumbManager();
	}
	public static Manga getCurrentManga() {
		return instance.getCurrentManga();
	}
	public String parseTags(Manga manga, Collection<Integer> colortags) {
		return instance.parseTags(manga, colortags);
	}
	public static MinimalManga getMinimalManga(int mangaId) {
		return instance.getMinimalManga(mangaId);
	}
	public static List<Chapter> getChapters(Manga manga) throws Exception {
		return instance.getChapters(manga);
	}
	/**
	 * planned to remove
	 */
	@Deprecated
	public static void loadManga(int arrayIndexOfSelectedManga) {
		// TODO Auto-generated method stub
		
		ThrowException.illegalAccessError("getmanga will be used");
	}
}
