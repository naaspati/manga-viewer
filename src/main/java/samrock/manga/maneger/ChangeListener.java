package samrock.manga.maneger;

@FunctionalInterface
public interface ChangeListener<E, F> {
	public void changed(E e, F f);
}
