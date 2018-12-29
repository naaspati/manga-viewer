package samrock.manga.maneger;

import static samrock.manga.maneger.MangaManegerStatus.MOD_MODIFIED;
import static samrock.utils.SortingMethod.DELETE_QUEUED;
import static samrock.utils.SortingMethod.FAVORITES;
import static samrock.utils.SortingMethod.READ_TIME_DECREASING;
import static samrock.utils.SortingMethod.READ_TIME_INCREASING;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import sam.collection.Iterators;
import sam.nopkg.Junk;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.MangasDAO.MangaIds;
import samrock.manga.recents.ChapterSavePoint;
import samrock.utils.SortingMethod;

public class Mangas extends Listeners<Mangas, MangaManegerStatus> implements Iterable<Integer> {
	private SortingMethod currentSortingMethod = null;

	// mangaIndices
	private final int[] array;
	private final Sorter sorter;
	private MangaIds mangaIds;
	private int size;

	private Mangas(MangaIds mangaIds) {
		this.sorter = new Sorter();
		this.mangaIds = mangaIds;
		this.array = new int[mangaIds.length()];
		this.size = array.length; 
	}
	void set(int[] mangaIndices) {
		set(mangaIndices, MOD_MODIFIED);
	}
	void set(int[] mangaIndices, MangaManegerStatus status) {
		size = 0;
		for (int i : mangaIndices) 
			array[size++] = i;
		notifyWatchers(this, status);
	}
	private SortingMethod getCurrentSortingMethod() {
		return currentSortingMethod;
	}
	public int length() {
		return  size;
	}
	public boolean equalsContent(int[] array) {
		if(array == null)
			return false;
		if(array.length != size)
			return false;
		for (int i = 0; i < array.length; i++) {
			if(this.array[i] != array[i])
				return false;
		}
		return true;
	}
	private boolean isEmpty() {
		return size == 0; 
	}
	private MinimalManga last() throws SQLException, IOException {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return get(array[size - 1]);
	}
	private MinimalManga first() throws SQLException, IOException {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return get(array[0]);
	}
	public MinimalManga get(int index) throws SQLException, IOException {
		return mangaIds.getMinimalManga(index);
	}
	
	private void sort(SortingMethod sortingMethod, boolean sortCurrentMangasOnDisplay) {
		sort(sortingMethod, sortCurrentMangasOnDisplay, true);
	}
	/**
	 * 
	 * @param sortingMethod by which mangas are sorted
	 * @param sortCurrentMangasOnDisplay if true and mangasOnDisplay is not null then current mangasOnDisplay is sorted, otherwise  mangasOnDisplay is set with new full mangas sorted array
	 */
	private void sort(SortingMethod sortingMethod, boolean sortCurrentMangasOnDisplay, boolean notify) {
		Objects.requireNonNull(sortingMethod, "sortingMethod = null, changeCurrentSortingMethod()");

		if(array == null)
			sortCurrentMangasOnDisplay = false;

		this.currentSortingMethod = sortingMethod;
		this.array = sorter.sortArray(sortCurrentMangasOnDisplay ? array : null, sortingMethod);
		if(notify)
			notifyWatchers(this, MOD_MODIFIED);
	}
	private void updateFavorites(Manga currentManga) {
		sorter.updateFavorites(currentManga);
		if(currentSortingMethod == FAVORITES)
			sort(FAVORITES, true);
	}
	//FIXME possibly combine into one methods
	private void updateReadTimeArray(Manga manga) {
		sorter.updateReadTimeArray(manga);
		if(currentSortingMethod == READ_TIME_INCREASING || currentSortingMethod == READ_TIME_DECREASING)
			sort(currentSortingMethod, true);
	}
	void reset() {
		sort(currentSortingMethod, false);
		notifyWatchers(this, MangaManegerStatus.MOD_MODIFIED_INTERNALLY);
	}
	private static final EnumSet<SortingMethod> resetIfContains = EnumSet.of(READ_TIME_DECREASING, READ_TIME_INCREASING, FAVORITES, DELETE_QUEUED);

	private void update(Manga m, ChapterSavePoint c) {

		if(m != null && m.isModified())
			updateFavorites(m);
		if(c != null && c.isModified()) {
			updateReadTimeArray(m);
			c.setUnmodifed();
		}
		if(resetIfContains.contains(getCurrentSortingMethod()))
			reset();

	}
	private Sorter getSorter() {
		return sorter;
	}
	/**
	 * arrayToBeSorted is sorted with currentSortingMethod 
	 * 
	 * @param  
	 * @return a new sorted array if arrayToBeSorted = null, otherwise arrayToBeSorted is sorted and returned 
	 */
	private int[] sortArray(int[] arrayToBeSorted){
		return sorter.sortArray(arrayToBeSorted, currentSortingMethod);		
	}
	private MinimalManga getManga(int index) {
		Junk.notYetImplemented();
		// TODO Auto-generated method stub
		return null;
	}
    
}
