package samrock.manga.maneger.api;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import sam.manga.samrock.chapters.ChapterWithId;
import samrock.manga.Chapter;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.Mangas;
import samrock.manga.maneger.ThumbManager;
interface MangaManeger {
	Manga getCurrentManga();
	int getMangasCount() ;
	void  loadMostRecentManga();
	Mangas mangas();
	Tags tagsDao();
	Recents recentsDao();
	void addMangaToDeleteQueue(Manga manga);
	ThumbManager getThumbManager();
	MinimalManga getMinimalManga(int manga_id) throws SQLException, IOException;
	List<String> getUrls(MinimalManga manga) throws SQLException, IOException;
	
	@Deprecated
	List<Chapter> getChapters(Manga manga) throws SQLException, IOException;
	ResultSet loadChapters(Manga manga) throws IOException, SQLException ;
	<E extends ChapterWithId> List<E> reloadChapters(Manga manga, List<E> loadedChapters) throws IOException, SQLException ;
}
