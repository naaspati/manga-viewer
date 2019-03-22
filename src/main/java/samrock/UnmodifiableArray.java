package samrock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import sam.collection.Iterators;

public class UnmodifiableArray<E> implements Iterable<E> {
	private final E[] array;
	
	public UnmodifiableArray(E[] array) {
		this.array = Objects.requireNonNull(array);
	}
	public E get(int index) {
		return array[index];
	}
	@Override
	public Iterator<E> iterator() {
		return Iterators.of(array);
	}
	@Override
	public void forEach(Consumer<? super E> action) {
		for (E e : array) 
			action.accept(e);
	}
	@Override
	public Spliterator<E> spliterator() {
		return Arrays.spliterator(array);
	}
	public int size() {
		return array.length;
	}
}
