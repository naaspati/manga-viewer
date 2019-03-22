package samrock.manga.maneger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import samrock.manga.Chapter;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
interface IMangaManeger {
	
	Manga getCurrentManga();
	int getMangasCount() ;
	void  loadMostRecentManga();
	Mangas mangas();
	TagsDAO tagsDao();
	RecentsDao recentsDao();
	void addMangaToDeleteQueue(Manga manga);
	ThumbManager getThumbManager();
	MinimalManga getMinimalManga(int manga_id) throws SQLException, IOException;
	List<Chapter> getChapters(Manga manga) throws SQLException, IOException;
	List<String> getUrls(MinimalManga manga) throws SQLException, IOException;
	
	default int mangaIdOf(MinimalManga manga) {
		Objects.requireNonNull(manga);
		
		if(manga.getClass() == IndexedMinimalManga.class)
			return ((IndexedMinimalManga)manga).getMangaId();
		else if(manga.getClass() == IndexedManga.class)
			return ((IndexedManga)manga).getMangaId();
		else
			throw new IllegalStateException("unknonwn class: "+manga.getClass());
	}
	default int indexOf(MinimalManga manga) {
		return ((IIndexedManga)manga).getIndex();
	}
	List<Chapter> loadChapters(IndexedManga manga) throws IOException, SQLException ;
	List<Chapter> reloadChapters(IndexedManga manga, List<Chapter> loadedChapters) throws IOException, SQLException ;
	int indexOfMangaId(int manga_id);
	SearchManeger searchManager(boolean create);
	
}
