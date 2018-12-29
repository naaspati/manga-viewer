package samrock.manga.maneger;

import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.logging.Logger;

import sam.io.serilizers.IntSerializer;
import sam.logging.MyLoggerFactory;
import sam.reference.ReferenceUtils;
import sam.sql.JDBCHelper;
import samrock.manga.maneger.MangasDAO.MangaIds;
import samrock.utils.SortingMethod;
import samrock.utils.Utils;

class Sorter {
	private final Logger LOGGER = MyLoggerFactory.logger(Sorter.class);
	
	private final EnumMap<SortingMethod, SoftReference<int[]>> map = new EnumMap<>(SortingMethod.class);
	private final MangasDAO dao;
	private final Path mydir;

	public Sorter(MangasDAO dao) throws IOException {
		this.dao = dao;
		this.mydir = dao.cacheDir().resolve(getClass().getName());
		Files.createDirectories(mydir);
	}

	/**
	 * arrayToBeSorted is sorted with currentSortingMethod 
	 * 
	 * @param  
	 * @return a new sorted array if arrayToBeSorted = null, otherwise arrayToBeSorted is sorted and returned 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public void sortArray(int[] array, SortingMethod sm) throws SQLException, IOException{
		if(array.length < 2)
			return;

		int[] array2 = getSortedFullArray(sm);
		System.arraycopy(array2, 0, array, 0, array.length);
	}

	private int[] reverseCopy(int[] array) {
		int[] copy = copy(array);
		reverse(copy);
		return copy;
	}
	private void reverse(int[] array) {
		if(array.length < 2)
			return;

		int len = array.length;
		for (int i = 0; i < len/2; i++) {
			int temp = array[i];
			array[i] = array[len - i - 1];
			array[len - i - 1] = temp;
		}
	}

	private StringBuilder SELECT = JDBCHelper.selectSQL(MANGAS_TABLE_NAME, MANGA_ID).append(" ORDER BY ");
	private int SELECT_LEN = SELECT.length();

	private int[] getSortedFullArray(SortingMethod sm) throws IOException, SQLException {
		int[] array = ReferenceUtils.get(map.get(sm));

		if(array == null) {
			array = ReferenceUtils.get(map.get(sm.opposite()));

			if(array != null) 
				return put(sm, reverseCopy(array));
		}

		if(array != null)
			return array;

		final Path p_increase = mydir.resolve(sm.columnName);

		if(Files.exists(p_increase)) {
			LOGGER.fine(() -> "LOAD SORTER(FILE): "+sm+", path: "+Utils.subpath(p_increase));
			return put(sm, IntSerializer.readArray(p_increase));
		}

		Path p_decrease = mydir.resolve(sm.opposite().columnName);

		if(Files.exists(p_decrease)) {
			LOGGER.fine(() -> "LOAD SORTER(FILE_REVERSE): "+sm+", path: "+Utils.subpath(p_decrease));
			return put(sm, reverseCopy(IntSerializer.readArray(p_decrease)));
		}

		String sql = SELECT.append(sm.columnName).append(';').toString();
		SELECT.setLength(SELECT_LEN);

		int n[] = {0}; 
		int[] array2 = new int[dao.getMangaIds().length()];
		array = array2;

		MangaIds ids = dao.getMangaIds();
		DB.iterate(sql, rs -> array2[n[0]++] = ids.indexOfMangaId(rs.getInt(1)));
		
		LOGGER.fine(() -> "LOAD SORTER(DB): "+sm);

		if(sm.isIncreasingOrder) {
			IntSerializer.write(array2, p_increase);
			LOGGER.fine(() -> "WRITE SORTER: "+sm+", path: "+Utils.subpath(p_increase));
			return put(sm, array2);
		} else {
			array = reverseCopy(array2);
			IntSerializer.write(array, p_decrease);
			LOGGER.fine(() -> "LOAD SORTER: "+sm+", path: "+Utils.subpath(p_decrease));
			return put(sm, array);
		}
	}
	
	private int[] put(SortingMethod sm, int[] array) {
		map.put(sm, new SoftReference<int[]>(array));
		return array;
	}
	private int[] copy(int[] array) {
		return Arrays.copyOf(array, array.length);
	}

}
