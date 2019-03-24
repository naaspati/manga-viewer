package samrock.manga.maneger.api;

import java.io.File;

import samrock.manga.MinimalManga;

public interface ThumbManager {
	UnmodifiableArray<File> getThumbsPaths(MinimalManga manga);
	File getRandomThumbPath(MinimalManga manga);
}