package samrock.utils;

import java.lang.ref.SoftReference;
import java.util.Objects;

import org.mapdb.IndexTreeList;

public class SoftListMapDB<T> extends SoftList<T> {
	private final IndexTreeList<T> mapdb;

	public SoftListMapDB(@SuppressWarnings("rawtypes") SoftReference[] array, IndexTreeList<T> mapdb) {
		super(Objects.requireNonNull(array));
		this.mapdb = mapdb;
	}
	public SoftListMapDB(int size, IndexTreeList<T> mapdb) {
		super(size);
		this.mapdb = Objects.requireNonNull(mapdb);
	}
	public T get(int index) {
		T t = super.get(index);
		if(t != null)
			return t;

		t = mapdb.get(index);
		super.set(index, t);
		return t;
	}
	public void set(int index, T value) {
		super.set(index, value);
		mapdb.set(index, value);
	}
}
