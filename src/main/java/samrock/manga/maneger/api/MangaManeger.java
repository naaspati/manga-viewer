package samrock.manga.maneger.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import samrock.manga.Manga;
import samrock.manga.MinimalManga;
public interface MangaManeger {
	Manga getCurrentManga();
	int getMangasCount() ;
	void  loadMostRecentManga() throws IOException, SQLException;
	void addMangaToDeleteQueue(Manga manga);
	MinimalManga getMinimalManga(int manga_id) throws SQLException, IOException;
	List<String> getUrls(MinimalManga manga) throws SQLException, IOException;
}
