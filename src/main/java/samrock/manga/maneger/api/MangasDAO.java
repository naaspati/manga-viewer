package samrock.manga.maneger.api;

import java.io.IOException;
import java.sql.SQLException;

import samrock.manga.Manga;
import samrock.manga.MinimalManga;

public interface MangasDAO {
	MangaIds getMangaIds();
	DeleteQueue getDeleteQueue();
	MinimalManga getMinimalManga(int manga_id) throws SQLException, IOException;
	void saveManga(Manga m);
	Manga getFullManga(MinimalManga manga);
	void loadMostRecentManga() throws SQLException, IOException;
	MinimalManga getMinimalMangaByIndex(int index) throws IOException, SQLException;
}