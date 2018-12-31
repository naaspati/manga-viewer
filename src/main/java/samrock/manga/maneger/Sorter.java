package samrock.manga.maneger;

import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static samrock.manga.maneger.SortingMethod.READ_TIME_DECREASING;
import static samrock.manga.maneger.SortingMethod.READ_TIME_INCREASING;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import sam.io.serilizers.IntSerializer;
import sam.logging.MyLoggerFactory;
import sam.reference.ReferenceUtils;
import sam.sql.JDBCHelper;
import samrock.manga.Manga;
import samrock.manga.maneger.MangasDAO.MangaIds;
import samrock.utils.Utils;

class Sorter {
	private final Logger LOGGER = MyLoggerFactory.logger(Sorter.class);

	private final EnumMap<SortingMethod, SoftReference<SortedArray>> map = new EnumMap<>(SortingMethod.class);
	private final MangasDAO dao;
	private final Path mydir;
	
	private class SortedArray {
		final int[] array;
		final SortingMethod method;
		final Path path;
		boolean modified = false;
		
		public SortedArray(int[] array, SortingMethod method) {
			this.array = array;
			this.method = method;
			this.path = path(method);
			this.modified = true;
		}
		
		public SortedArray(SortingMethod method) throws IOException {
			this.method = method;
			this.path = path(method);
			this.array = Files.notExists(path) ? null : IntSerializer.readArray(path);
			this.modified = false;
			
			if(this.array != null)
				LOGGER.fine(() -> "LOAD SORTER(FILE): "+method+", path: "+Utils.subpath(path));
		}
		@Override
		protected void finalize() throws Throwable {
			if(modified) {
				try {
					IntSerializer.write(array, path);
					LOGGER.fine(() -> "finalize: Sorter#SortedArray#"+method+"  write: "+Utils.subpath(path));
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "failed saving "+path, e);
				}	
			}
			super.finalize();
		}
		public SortedArray opposite() {
			int[] array = Arrays.copyOf(this.array, this.array.length);
			
			int len = array.length;
			for (int i = 0; i < len/2; i++) {
				int temp = array[i];
				array[i] = array[len - i - 1];
				array[len - i - 1] = temp;
			}
			return new SortedArray(array, method.opposite());
		}
	}
	
	public Path path(SortingMethod method) {
		return mydir.resolve(String.valueOf(method.ordinal()));
	}
	
	public Sorter(MangasDAO dao) throws IOException {
		this.dao = dao;
		this.mydir = Utils.APP_DATA.resolve(getClass().getName());
		if(DB.isModified())
			Utils.delete(mydir);
		
		Files.createDirectories(mydir);
	}

	/**
	 * arrayToBeSorted is sorted with currentSortingMethod 
	 * @param length 
	 * 
	 * @param  
	 * @return a new sorted array if arrayToBeSorted = null, otherwise arrayToBeSorted is sorted and returned 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public void sortArray(final int[] array, final SortingMethod sm, final int length) throws SQLException, IOException{
		if(array.length < 2)
			return;

		int[] array2 = getSortedFullArray(sm);
		if(length == array2.length)
			System.arraycopy(array2, 0, array, 0, array.length);
		else {
			BitSet set = new BitSet();
			for (int i = 0; i < length; i++) 
				set.set(array[i]);
			
			int i = 0, j = 0;
			
			while(j < length) {
				int n = array2[i++];
				if(set.get(n))
					array[j++] = n;
				i++;
			}
			LOGGER.fine(() -> "sorted subset of array ("+length+"/"+array2.length+")");
		}
	}

	private StringBuilder SELECT = JDBCHelper.selectSQL(MANGAS_TABLE_NAME, MANGA_ID).append(" ORDER BY ");
	private int SELECT_LEN = SELECT.length();

	private int[] getSortedFullArray(final SortingMethod sm) throws SQLException, IOException {
		SortedArray array = ReferenceUtils.get(map.get(sm));
		SortingMethod opposite = sm.opposite();

		if(array == null) {
			array = ReferenceUtils.get(map.get(opposite));
			if(array != null) 
				return put(sm, array.opposite());
		}

		if(array != null)
			return array.array;

		array = new SortedArray(sm);

		if(array.array != null) 
			return put(sm, array);

		Path p_decrease = path(opposite);

		if(Files.exists(p_decrease)) {
			LOGGER.fine(() -> "LOAD SORTER(FILE_REVERSE): "+sm+", path: "+Utils.subpath(p_decrease));
			array = new SortedArray(opposite);
			put(opposite, array);
			return put(sm, array.opposite());
		}

		String sql = SELECT.append(sm.columnName).append(';').toString();
		SELECT.setLength(SELECT_LEN);

		int n[] = {0}; 
		int[] array2 = new int[dao.getMangaIds().length()];

		MangaIds ids = dao.getMangaIds();
		DB.iterate(sql, rs -> array2[n[0]++] = ids.indexOfMangaId(rs.getInt(1)));
		
		if(n[0] != array2.length)
			throw new IOException("new array("+n[0]+") is smaller than expected ("+array2.length+")");

		LOGGER.fine(() -> "LOAD SORTER(DB): "+sm);
		
		if(sm.isIncreasingOrder) {
			SortedArray s = new SortedArray(array2, sm); 
			put(opposite, s.opposite());
			return put(sm, array);
		} else {
			SortedArray s = new SortedArray(array2, opposite); 
			put(opposite, s);
			return put(sm, s.opposite());
		}
	}
	private int[] put(SortingMethod sm, SortedArray s) {
		map.put(sm, new SoftReference<>(s));
		return s.array;
	}
	
	private long maxReadTime = System.currentTimeMillis();
	
	public void updateReadTimeSorting(Manga m) throws IOException {
		
		if(m.getLastReadTime() > maxReadTime) {
			maxReadTime = m.getLastReadTime();
			int index = MangaManeger.indexOf(m);
			
			relocate(READ_TIME_INCREASING, index);
			relocate(READ_TIME_DECREASING, index);
		}
	}

	private void relocate(final SortingMethod sm, final int index) throws IOException {
		SortedArray s = ReferenceUtils.get(map.get(sm));
		
		if(s == null)
			s = new SortedArray(sm);
		
		int[] array = s.array; 
		if(array != null) {
			boolean increase = sm == READ_TIME_INCREASING; 
			if(increase && array[array.length - 1] == index)
				return;
			else if(array[0] == index)
				return;
			
			int n = index(s.array, index);
			if(increase) {
				System.arraycopy(array, n+1, array, n, array.length - n);
				array[array.length - 1] = index;	
			} else {
				System.arraycopy(array, 0, array, 1, array.length - n);
				array[0] = index;
			}
		}
	}

	private int index(int[] array, int value) {
		for (int i = 0; i < array.length; i++) {
			if(array[i] == value)
				return i;
		}
		return -1;
	}

}
