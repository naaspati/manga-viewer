package samrock.manga.maneger;

import static samrock.manga.maneger.MangaManegerStatus.MOD_MODIFIED;

import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntPredicate;

import sam.nopkg.Junk;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.MangasDAO.MangaIds;
import samrock.manga.recents.ChapterSavePoint;

public class Mangas {

	// mangaIndices for private use
	private SortingMethod sorting = null;
	private final int[] array;
	private final Sorter sorter;
	private final MangasDAO dao;
	private final MangaIds mangaIds;
	private int size;
	private Manga currentManga;
	private final Listeners<Manga, Void> currentMangaListener = new Listeners<>();
	private final Listeners<Mangas, MangaManegerStatus> idsChangeListeners = new Listeners<>();

	Mangas(MangasDAO dao) throws IOException {
		this.dao = dao;
		this.mangaIds = dao.getMangaIds();
		this.sorter = new Sorter(dao);
		this.array = new int[dao.getMangaIds().length()];
		this.size = array.length; 
	}

	public Manga current() {
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
	public SortingMethod getSorting() {
		return sorting;
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
	public MinimalManga last() throws SQLException, IOException {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return get(array[size - 1]);
	}
	public MinimalManga first() throws SQLException, IOException {
		if(isEmpty())
			throw new NoSuchElementException("empty");
		return get(array[0]);
	}
	public MinimalManga get(int index) throws SQLException, IOException {
		return mangaIds.getMinimalManga(index);
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

	public void update(Manga m, ChapterSavePoint c) throws SQLException, IOException {
		sorter.updateReadTimeSorting(m);
		if(sorting == SortingMethod.READ_TIME_DECREASING || sorting == SortingMethod.READ_TIME_INCREASING)
			sort(sorting, true);
	}
	public DeleteQueue getDeleteQueue() {
		return dao.getDeleteQueue();
	}

	// used as MARKER
	static final IntPredicate ONLY_DELETE_QUEUED = m -> true ;
	// used as MARKER
	static final IntPredicate ONLY_FAVORITES = m -> true ;

	void setFilter(IntPredicate filter) {

		// TODO Auto-generated method stub
		Junk.notYetImplemented();

	}


	public Listeners<Mangas, MangaManegerStatus> getMangaIdsListener() {
		return idsChangeListeners;
	}


}
