package samrock.manga.maneger;

import java.io.IOException;
import java.sql.SQLException;

import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.MangasDAOImpl.MangaIds;
import samrock.manga.maneger.api.DeleteQueue;

interface MangasDAO {
	MangaIds getMangaIds();
	DeleteQueue getDeleteQueue();
	IndexedMinimalManga getMinimalManga(int manga_id) throws SQLException, IOException;
	void saveManga(Manga m);
	IndexedManga getFullManga(MinimalManga manga);
	void loadMostRecentManga(Manga manga);

}