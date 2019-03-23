package samrock.manga.maneger.api;

@FunctionalInterface
public interface ChangeListener<E, F> {
	public void changed(E e, F f);
}
