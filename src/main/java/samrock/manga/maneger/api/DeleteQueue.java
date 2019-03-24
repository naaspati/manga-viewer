package samrock.manga.maneger.api;

import samrock.manga.MinimalManga;

public interface DeleteQueue extends Listeners<MinimalManga, Operation> {
	void add(MinimalManga m);
	void remove(MinimalManga m);
	boolean contains(MinimalManga m);
	boolean isEmpty();
	MinimalManga[] values();
	int[] toMangaIdsArray();
}