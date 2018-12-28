package samrock.manga.maneger;

import static samrock.manga.maneger.MangaManegerStatus.MOD_MODIFIED;
import static samrock.utils.SortingMethod.DELETE_QUEUED;
import static samrock.utils.SortingMethod.FAVORITES;
import static samrock.utils.SortingMethod.READ_TIME_DECREASING;
import static samrock.utils.SortingMethod.READ_TIME_INCREASING;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import sam.collection.Iterators;
import samrock.manga.Manga;
import samrock.manga.recents.ChapterSavePoint;
import samrock.utils.SortingMethod;

public class MangasOnDisplay extends Listeners<MangasOnDisplay, MangaManegerStatus> implements Iterable<Integer> {
	private final DeleteQueue deleteQueue = new DeleteQueue();
	private SortingMethod currentSortingMethod = null;

	// mangaIndices
	private int[] array;
	private final Sorter sorter;
	private final MangaManeger maneger;

	public MangasOnDisplay(MangaManeger maneger, Dao dao) {
		this.maneger = maneger;
		this.sorter = new Sorter(dao);
	}
	void set(int[] mangaIndices) {
		set(mangaIndices, MOD_MODIFIED);
	}
	void set(int[] mangaIndices, MangaManegerStatus status) {
		this.array = mangaIndices;
		notifyWatchers(this, status);
	}
	public SortingMethod getCurrentSortingMethod() {
		return currentSortingMethod;
	}
	public int length() {
		return  isEmpty() ? 0 : array.length;
	}
	public int get(int index) {
		return  array[index];
	}
	@Override
	public Iterator<Integer> iterator() {
		return Iterators.of(array);
	}
	public boolean equalsContent(int[] array) {
		return Arrays.equals(array, this.array);
	}
	public DeleteQueue getDeleteQueue() {
		return deleteQueue;
	}
	public boolean isEmpty() {
		return array == null || array.length == 0; 
	}
	public int last() {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return array[array.length];
	}
	public int first() {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return array[0];
	}
	public int at(int index) {
		return array[index];
	}
	
	public int[] getCopy() {
		return isEmpty() ? array : Arrays.copyOf(array, array.length);
	}
	public void sort(SortingMethod sortingMethod, boolean sortCurrentMangasOnDisplay) {
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
	public void updateFavorites(Manga currentManga) {
		sorter.updateFavorites(currentManga);
		if(currentSortingMethod == FAVORITES)
			sort(FAVORITES, true);
	}
	//FIXME possibly combine into one methods
	public void updateReadTimeArray(Manga manga) {
		sorter.updateReadTimeArray(manga);
		if(currentSortingMethod == READ_TIME_INCREASING || currentSortingMethod == READ_TIME_DECREASING)
			sort(currentSortingMethod, true);
	}
	void reset() {
		sort(currentSortingMethod, false);
		notifyWatchers(this, MangaManegerStatus.MOD_MODIFIED_INTERNALLY);
	}
	private static final EnumSet<SortingMethod> resetIfContains = EnumSet.of(READ_TIME_DECREASING, READ_TIME_INCREASING, FAVORITES, DELETE_QUEUED);

	public void update(Manga m, ChapterSavePoint c) {

		if(m != null && m.isModified())
			updateFavorites(m);
		if(c != null && c.isModified()) {
			updateReadTimeArray(m);
			c.setUnmodifed();
		}
		if(resetIfContains.contains(getCurrentSortingMethod()))
			reset();

	}
	public Sorter getSorter() {
		return sorter;
	}
	/**
	 * arrayToBeSorted is sorted with currentSortingMethod 
	 * 
	 * @param  
	 * @return a new sorted array if arrayToBeSorted = null, otherwise arrayToBeSorted is sorted and returned 
	 */
	public int[] sortArray(int[] arrayToBeSorted){
		return sorter.sortArray(arrayToBeSorted, currentSortingMethod);		
	}
    
}
