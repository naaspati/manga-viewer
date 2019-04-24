package samrock.manga.maneger;

import java.util.Objects;

import samrock.manga.maneger.api.UnmodifiableArray;

class UnmodifiableArrayImpl<E> implements UnmodifiableArray<E> {
	private final E[] data;

	public UnmodifiableArrayImpl(E[] data) {
		this.data = Objects.requireNonNull(data);
	}
	@Override
	public int size() {
		return data.length;
	}
	@Override
	public E get(int index) {
		return data[index];
	}
}
