package samrock.manga.maneger.api;

import java.io.IOException;
import java.sql.SQLException;

import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.SortingMethod;
import samrock.manga.recents.ChapterSavePoint;

public interface Mangas {
	Manga current();
	SortingMethod getSorting();
	int length();
	MinimalManga last() throws SQLException, IOException;
	MinimalManga first() throws SQLException, IOException;
	MinimalManga get(int index) throws SQLException, IOException;
	void update(Manga m, ChapterSavePoint c) throws SQLException, IOException;
	DeleteQueue getDeleteQueue();
	Listeners<Mangas, MangaManegerStatus> getMangaIdsListener();

}