package samrock.manga.maneger;

import static samrock.manga.maneger.api.MangaManegerStatus.MOD_MODIFIED;

import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntPredicate;

import sam.nopkg.EnsureSingleton;
import sam.nopkg.Junk;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.api.DeleteQueue;
import samrock.manga.maneger.api.Listeners;
import samrock.manga.maneger.api.MangaIds;
import samrock.manga.maneger.api.MangaManegerStatus;
import samrock.manga.maneger.api.Mangas;
import samrock.manga.maneger.api.MangasDAO;
import samrock.manga.maneger.api.SortingMethod;
import samrock.manga.recents.ChapterSavePoint;

abstract class MangasImpl implements Mangas {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{
		singleton.init();
	}

	// mangaIndices for private use
	private SortingMethod sorting = null;
	private final int[] array;
	private final Sorter sorter;
	private int size;
	private IndexedManga currentManga;
	private final ListenersImpl<Manga, Void> currentMangaListener = new ListenersImpl<>();
	private final ListenersImpl<Mangas, MangaManegerStatus> idsChangeListeners = new ListenersImpl<>();

	MangasImpl(int size, Sorter sorter) throws IOException {
		this.sorter = sorter;
		this.array = new int[size];
		this.size = size; 
	}

	@Override
	public IndexedManga current() {
		return currentManga;
	}
	void set(int[] mangaIndices) {
		set(mangaIndices, MOD_MODIFIED);
	}
	void set(int[] mangaIndices, MangaManegerStatus status) {
		size = 0;
		for (int i : mangaIndices) 
			array[size++] = i;
		idsChangeListeners.notifyWatchers(this, status);
	}
	@Override
	public SortingMethod getSorting() {
		return sorting;
	}
	@Override
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
	@Override
	public MinimalManga last() throws SQLException, IOException {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return get(array[size - 1]);
	}
	@Override
	public MinimalManga first() throws SQLException, IOException {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return get(array[0]);
	}
	@Override
	public MinimalManga get(int index) throws SQLException, IOException {
		return dao().getMinimalMangaByIndex(index);
	}

	/**
	 * 
	 * @param sortingMethod by which mangas are sorted
	 * @param sortCurrentMangasOnDisplay if true and mangasOnDisplay is not null then current mangasOnDisplay is sorted, otherwise  mangasOnDisplay is set with new full mangas sorted array
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public void sort(SortingMethod sortingMethod, boolean notify) throws SQLException, IOException {
		Objects.requireNonNull(sortingMethod, "sortingMethod = null, changeCurrentSortingMethod()");

		this.sorting = sortingMethod;
		sorter.sortArray(array, sortingMethod, size);
		if(notify)
			idsChangeListeners.notifyWatchers(this, MOD_MODIFIED);
	}

	void reset() throws SQLException, IOException {
		sort(sorting, false);
		idsChangeListeners.notifyWatchers(this, MangaManegerStatus.MOD_MODIFIED_INTERNALLY);
	}

	@Override
	public void update(Manga m, ChapterSavePoint c) throws SQLException, IOException {
		sorter.updateReadTimeSorting(m);
		if(sorting == SortingMethod.READ_TIME_DECREASING || sorting == SortingMethod.READ_TIME_INCREASING)
			sort(sorting, true);
	}
	@Override
	public DeleteQueue getDeleteQueue() {
		return dao().getDeleteQueue();
	}

	protected abstract MangasDAO dao();

	// used as MARKER
	static final IntPredicate ONLY_DELETE_QUEUED = m -> true ;
	// used as MARKER
	static final IntPredicate ONLY_FAVORITES = m -> true ;

	void setFilter(IntPredicate filter) {

		// TODO Auto-generated method stub
		Junk.notYetImplemented();

	}


	@Override
	public Listeners<Mangas, MangaManegerStatus> getMangaIdsListener() {
		return idsChangeListeners;
	}


}
