package samrock.manga;

public interface Chapters {

	void reload();

	Chapter get(int i);

	int size();

	Order getOrder();

	void flip();

}
