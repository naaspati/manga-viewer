package samrock.utils;

import org.mapdb.HTreeMap;

public class SoftListMapDBUsingMangaId<E> extends SoftList<E> {
	// manga_id -> manga
	private final HTreeMap<Integer, E> mapdb;

	public SoftListMapDBUsingMangaId(int size, HTreeMap<Integer, E> mapdb) {
		super(size);
		this.mapdb = mapdb;
	}
	public E get(MangaIdIndexContainer m) {
		return get(m.getMangaIndex(), m.getMangaId());
	}
	public E get(int index, int manga_id) {
		E m = super.get(index);
		if(m == null) {
			m = mapdb.get(manga_id);
			super.set(index, m);
		}
		return m;
	}
	@Override
	public E get(int index) {
		throw new IllegalAccessError();
	}
	@Override
	public void set(int index, E value) {
		throw new IllegalAccessError();
	}
	public void set(MangaIdIndexContainer key, E e) {
		set(key.getMangaIndex(), key.getMangaId(), e);
	}
	private void set(int index, int mangaId, E e) {
		if(e == null)
			return;
		super.set(index, e);
		this.mapdb.put(mangaId, e);
	}
}