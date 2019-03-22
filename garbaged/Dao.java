package samrock.manga.maneger;

import java.sql.SQLException;
import org.slf4j.Logger;

import sam.logging.MyLoggerFactory;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.recents.ChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint;

@Deprecated
public class Dao implements AutoCloseable {
	private static final Logger LOGGER = Utils.getLogger(Dao.class);
	
	private final RecentChapterDao recent;
	
	public Dao() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		mangas = MangasDAO.getInstance();
		TagsDAO.init();;
		recent = new RecentChapterDao(this, mapdb);
	}
	
	public String getTag(int tagId) {
		return tags.getTag(tagId);
	}
	public int mangasCount() {
		return mangas.mangasCount();
	}
	
	public MinimalChapterSavePoint getSavePoint(MinimalManga manga) {
		return recent.getSavePoint(manga);
	}
	public ChapterSavePoint getFullSavePoint(Manga manga) {
		return recent.getFullSavePoint(manga);
	}
	public void updateMangaCache(Manga manga) {
		mangas.updateMangaCache(manga);
	}
	public void saveSavePoint(ChapterSavePoint cs) throws SQLException {
		recent.saveSavePoint(cs);
	}
	public TagsDAO tagsDAO() {
		return tags;
	}
	public IndexedMinimalManga getMinimalManga(int manga_id) {
		return mangas.getMinimalManga(manga_id);
	}
	public IndexedManga getFullManga(IndexedMinimalManga manga) {
		return mangas.getFullManga(manga);
	}
	public void saveManga(IndexedManga m) {
		mangas.saveManga(m);
	}
}
