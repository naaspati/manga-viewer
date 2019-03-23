package samrock.manga.maneger.api;

public interface Listeners<E, F> {
	void addChangeListener(ChangeListener<E, F> listener);
	void removeChangeListener(ChangeListener<E, F> listener);
}