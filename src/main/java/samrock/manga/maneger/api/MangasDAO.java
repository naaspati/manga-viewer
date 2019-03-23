package samrock.manga.maneger.api;

import java.io.IOException;
import java.sql.SQLException;

import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.MangasDAOImpl.MangaIds;

public interface MangasDAO {
	MangaIds getMangaIds();
	DeleteQueue getDeleteQueue();
	MinimalManga getMinimalManga(int manga_id) throws SQLException, IOException;
	void saveManga(Manga m);
	Manga getFullManga(MinimalManga manga);
	void loadMostRecentManga(Manga manga);

}