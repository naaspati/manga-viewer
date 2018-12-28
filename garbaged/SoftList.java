package samrock.utils;

import java.lang.ref.SoftReference;
import java.util.Iterator;

import sam.collection.Iterators;
import sam.reference.ReferenceUtils;

public class SoftList<T> implements Iterable<T> {
	@SuppressWarnings("rawtypes")
	private SoftReference[] array;

	public SoftList(@SuppressWarnings("rawtypes") SoftReference[] array) {
		this.array = array;
	}
	public SoftList(int size) {
		array = new SoftReference[size];
	}
	
	@SuppressWarnings({ "unchecked"})
	public T get(int index) {
		return (T)ReferenceUtils.get(array[index]);
	}
	public void set(int index, T value) {
		array[index] = new SoftReference<T>(value);
	}
	@SuppressWarnings("unchecked")
	@Override
	public Iterator<T> iterator() {
		return Iterators.map(Iterators.of(array), e -> (T)e.get());
	}
	public int size() {
		return array == null ? 0 : array.length;
	}
}
