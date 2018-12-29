package samrock.manga.maneger;

import static sam.manga.samrock.mangas.MangasMeta.IS_FAVORITE;
import static sam.manga.samrock.mangas.MangasMeta.LAST_UPDATE_TIME;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static samrock.utils.SortingMethod.DELETE_QUEUED;
import static samrock.utils.SortingMethod.FAVORITES;
import static samrock.utils.SortingMethod.READ_TIME_DECREASING;
import static samrock.utils.SortingMethod.READ_TIME_INCREASING;

import java.lang.ref.SoftReference;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;

import sam.collection.IntList;
import sam.reference.ReferenceUtils;
import sam.sql.querymaker.QueryMaker;
import sam.sql.querymaker.Select;
import samrock.manga.Manga;
import samrock.utils.SortingMethod;

class Sorter {
	private final EnumMap<SortingMethod, SoftReference<int[]>> map = new EnumMap<>(SortingMethod.class);
	
	public Sorter() { }
	
	/**
	 * arrayToBeSorted is sorted with currentSortingMethod 
	 * 
	 * @param  
	 * @return a new sorted array if arrayToBeSorted = null, otherwise arrayToBeSorted is sorted and returned 
	 * @throws SQLException 
	 */
	public int[] sortArray(int[] arrayToBeSorted, SortingMethod sm) throws SQLException{
		if(arrayToBeSorted != null && arrayToBeSorted.length < 2)
			return arrayToBeSorted;

		if(arrayToBeSorted == null)
			return arrayCopy(getSortedFullArray(sm));
		else{ 
			sortArray(getSortedFullArray(sm), arrayToBeSorted);
			return arrayToBeSorted;
		}
	}

	public void sortArray(int[] sortedArray, int[] arrayToBeSorted){
		if(arrayToBeSorted.length < 2)
			return;

		if(arrayToBeSorted.length == sortedArray.length)
			for (int i = 0; i < sortedArray.length; i++) arrayToBeSorted[i] = sortedArray[i];
		else{
			Arrays.sort(arrayToBeSorted);
			int[] array2 = new int[arrayToBeSorted.length];

			for (int i = 0, j = 0; i < array2.length; j++) {
				if(Arrays.binarySearch(arrayToBeSorted, sortedArray[j]) >= 0)
					array2[i++] = sortedArray[j];
			}
			for (int i = 0; i < array2.length; i++) arrayToBeSorted[i] = array2[i];
		}
	}

	private int[] arrayReversedCopy(int[] array) {
		int[] array2 = arrayCopy(array);
		reverse(array2);
		return array2;
	}

	private int[] arrayCopy(int[] array) {
		return Arrays.copyOf(array, array.length);
	}

	private int[] getSortedFullArray(SortingMethod sm) throws SQLException {
		if(Objects.requireNonNull(sm) == DELETE_QUEUED)
			throw new IllegalArgumentException("not valid sortingMethod: "+sm);

		int[] array = ReferenceUtils.get(map.get(sm));

		if(array == null) {
			array = ReferenceUtils.get(map.get(sm.opposite()));
			if(array != null) {
				array = arrayReversedCopy(array);
				map.put(sm, new SoftReference<int[]>(array));
			}
		}

		if(array != null)
			return array;

		Select select = QueryMaker.qm().select(MANGA_ID).from(MANGAS_TABLE_NAME);
		if(sm == sm.opposite()) {
			if(sm == FAVORITES)
				select = select.where(w -> w.eq(IS_FAVORITE, 1)).orderBy(false, LAST_UPDATE_TIME);
		} else {
			select = select.orderBy(sm.isIncreasingOrder, sm.columnName);
		} 

		String sql = select.build();
		IntList list = new IntList(MangaManeger.getMangasCount());
		DB.iterate(sql, rs -> list.add(MangaManeger.indexOfMangaId(rs.getInt(1))));

		map.put(sm, new SoftReference<int[]>(array = list.toArray()));
		return array;
	}

	private void reverse(int[] array) {
		if(array.length < 2)
			return;
		for (int i = 0; i < array.length/2; i++) {
			int temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	public void updateFavorites(Manga currentManga) {
		int[] favorites = ReferenceUtils.get(map.get(FAVORITES));
		if(favorites == null) return;

		int index = 0;
		boolean found = false;
		int arrayIndex = MangaManeger.indexOf(currentManga);
		for (; index < favorites.length; index++)  if(found = favorites[index] == arrayIndex) break;

		if(currentManga.isFavorite()){
			if(!found){
				favorites = Arrays.copyOf(favorites, favorites.length + 1);
				index = favorites.length - 1;
			}

			for (; index > 0; index--) 
				favorites[index] = favorites[index - 1];

			favorites[0] = arrayIndex;
		}
		else if(found){
			for (; index < favorites.length - 1; index++) 
				favorites[index] = favorites[index + 1];

			favorites = Arrays.copyOf(favorites, favorites.length - 1);
		}

		map.put(FAVORITES, new SoftReference<int[]>(favorites));
	}
	public void updateReadTimeArray(Manga manga) {
		int index = MangaManeger.indexOf(manga);
		int[] array = ReferenceUtils.get(map.get(READ_TIME_INCREASING));
		
		if(array == null) {
			array = ReferenceUtils.get(map.get(READ_TIME_DECREASING));
			if(array == null)
				return;
			reverse(array);
		};
		
		if(array[array.length - 1] != index) {
			boolean shift = false;
			for (int i = 0; i < array.length - 1; i++) {
				if(array[i] == index)
					shift = true;
				if(shift)
					array[i] = array[i+1];
			}
			
			array[array.length - 1] = index;	
		}
		map.put(READ_TIME_INCREASING, new SoftReference<int[]>(array));
		map.put(READ_TIME_DECREASING, new SoftReference<int[]>(arrayReversedCopy(array)));
	}
}
