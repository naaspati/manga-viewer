package samrock.manga.maneger;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.logging.Logger;

import sam.reference.ReferenceUtils;

public class IndexedList<T> {
	private final Logger logger;
	
	private SoftReference<T>[] array;
	private final int maxSize;
	
	@SuppressWarnings("unchecked")
	public IndexedList(int maxSize, Logger logger) {
		this.maxSize = maxSize;
		array = new SoftReference[0];
		
		this.logger = logger;
	}
	public T get(int index) {
		
		if(index < 0 || index >= maxSize)
			throw new ArrayIndexOutOfBoundsException(index+"!= [0,"+maxSize+")");
		
		return index >= array.length ? null : ReferenceUtils.get(array[index]);
	}
	
	void set(int index, T t) {
		
		if(index >= array.length) {
			int size = array.length ;
			array = Arrays.copyOf(array, index+1);
			if(logger != null)
				logger.fine("array resized: "+size +" -> "+array.length);
		}
		
		array[index] = new SoftReference<>(t); 
	} 
}
