package samrock.manga.maneger;

import static samrock.manga.maneger.MangaManegerStatus.MOD_MODIFIED;

import java.io.IOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Objects;

import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.MangasDAO.MangaIds;
import samrock.manga.recents.ChapterSavePoint;
import samrock.utils.SortingMethod;

public class Mangas extends Listeners<Mangas, MangaManegerStatus> {

	// mangaIndices for private use
	private SortingMethod sorting = null;
	private final int[] array;
	private final Sorter sorter;
	private final MangasDAO dao;
	private final MangaIds mangaIds;
	private int size;

	private Mangas(MangasDAO dao) throws IOException {
		this.dao = dao;
		this.mangaIds = dao.getMangaIds();
		this.sorter = new Sorter(dao);
		this.array = new int[dao.getMangaIds().length()];
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
	private void sort(SortingMethod sortingMethod, boolean notify) throws SQLException, IOException {
		Objects.requireNonNull(sortingMethod, "sortingMethod = null, changeCurrentSortingMethod()");

		this.sorting = sortingMethod;
		sorter.sortArray(array, sortingMethod, size);
		if(notify)
			notifyWatchers(this, MOD_MODIFIED);
	}

	void reset() throws SQLException, IOException {
		sort(sorting, false);
		notifyWatchers(this, MangaManegerStatus.MOD_MODIFIED_INTERNALLY);
	}

	public void update(Manga m, ChapterSavePoint c) throws SQLException, IOException {
		sorter.update(m, c, sorting);
		sort(sorting, true);
	}
	public DeleteQueue getDeleteQueue() {
		return dao.getDeleteQueue();
	}

}
