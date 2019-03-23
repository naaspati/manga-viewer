package samrock.manga.maneger.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.ThumbManager;
public interface MangaManeger {
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
}
