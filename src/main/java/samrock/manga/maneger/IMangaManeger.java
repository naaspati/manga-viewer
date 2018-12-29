package samrock.manga.maneger;

import java.util.List;
import java.util.Objects;

import samrock.manga.Chapters.Chapter;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.recents.ChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint;
interface IMangaManeger {
	
	Manga getCurrentManga();
	int getMangasCount() ;
	void  loadMostRecentManga();
	Mangas getMangasOnDisplay();
	MinimalChapterSavePoint getChapterSavePoint(MinimalManga manga);
	DB samrock();
	TagsDAO getTagDao();
	void addMangaToDeleteQueue(Manga manga);
	ThumbManager getThumbManager();
	MinimalManga getMinimalManga(int manga_id);
	List<Chapter> getChapters(Manga manga) throws  Exception;
	List<String> getUrls(MinimalManga manga) throws Exception;
	
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
	List<Chapter> loadChapters(IndexedManga manga);
	List<Chapter> reloadChapters(IndexedManga manga, List<Chapter> loadedChapters);
	int indexOfMangaId(int manga_id);
	SearchManeger searchManager(boolean create);
}
