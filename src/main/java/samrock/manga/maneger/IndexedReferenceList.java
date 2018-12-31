package samrock.manga.maneger;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.logging.Logger;

import sam.reference.ReferenceType;
import sam.reference.ReferenceUtils;

public class IndexedReferenceList<T> {
	private final Logger logger;
	
	private Reference<T>[] array;
	private final int maxSize;
	private final ReferenceType type;
	
	@SuppressWarnings({"rawtypes" })
	public IndexedReferenceList(int maxSize, Class owner) {
		this(maxSize, owner, ReferenceType.SOFT);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public IndexedReferenceList(int maxSize, Class owner, ReferenceType type) {
		this.maxSize = maxSize;
		array = new Reference[0];
		
		this.type = type;
		this.logger = Logger.getLogger(owner.getSimpleName()+"#"+IndexedReferenceList.class.getSimpleName());
	}
	public T get(int index) {
		checkIndex(index);
		return index >= array.length ? null : ReferenceUtils.get(array[index]);
	}
	
	private void checkIndex(int index) {
		if(index < 0 || index >= maxSize)
			throw new ArrayIndexOutOfBoundsException(index+"!= [0,"+maxSize+")");
	}
	void set(int index, T t) {
		checkIndex(index);
		
		if(index >= array.length) {
			int size = array.length ;
			array = Arrays.copyOf(array, index+1);
			if(logger != null)
				logger.fine("array resized: "+size +" -> "+array.length);
		}
		
		array[index] = type.get(t); 
	} 
}
