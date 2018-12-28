package samrock.manga.maneger;

import java.util.Collection;
import java.util.List;

import samrock.manga.Manga;
import samrock.manga.Manga.Chapter;
import samrock.manga.MinimalManga;
import samrock.manga.recents.ChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint;
interface IMangaManeger {
	
	Manga getCurrentManga();
	ChapterSavePoint getCurrentSavePoint(); 
	String parseTags(Manga manga, Collection<Integer> colortags); 

	int getMangasCount() ;
	void  loadMostRecentManga();
	MangasOnDisplay getMangasOnDisplay();
	MinimalChapterSavePoint getChapterSavePoint(MinimalManga manga);
	DB samrock();
	TagsDAO getTagDao();
	void addMangaToDeleteQueue(Manga manga);
	ThumbManager getThumbManager();
	MinimalManga getMinimalManga(int manga_id);
	List<Chapter> getChapters(Manga manga) throws  Exception; 
}
