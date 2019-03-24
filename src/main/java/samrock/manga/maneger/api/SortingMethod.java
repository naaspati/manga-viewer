package samrock.manga.maneger.api;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.LAST_UPDATE_TIME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_NAME;
import static sam.manga.samrock.mangas.MangasMeta.RANK;

/**
 * here is the logic of sorting time is, today < yesterday <  day before yesterday < .....
 * <br>mathmatically Math.abs(today - given_date)
 * <br>rest SortingMethod Follows natural Order
 * @author Sameer
 */
public enum SortingMethod {
	/**
	 * A -> Z
	 */
	ALPHABETICALLY_INCREASING(MANGA_NAME),
	/**
	 * Z -> A
	 */
	ALPHABETICALLY_DECREASING(MANGA_NAME),

	/*
	 * from 1->100....
	 */
	RANKS_INCREASING(RANK),
	/**
	 * ...100, 99, 98...-> 1
	 */
	RANKS_DECREASING(RANK),

	/**
	 * from Read long ago -> Read Recently
	 */
	READ_TIME_DECREASING(LAST_READ_TIME),
	/**
	 * from Recently Read -> Read long ago
	 */
	READ_TIME_INCREASING(LAST_READ_TIME),

	/**
	 * Recently Updated -> Updated long ago
	 */
	UPDATE_TIME_DECREASING(LAST_UPDATE_TIME),
	/**
	 * Updated long ago -> Recently Updated
	 */
	UPDATE_TIME_INCREASING(LAST_UPDATE_TIME);
	
	// DELETE_QUEUED,

	// FAVORITES;
	
	public final String columnName;
	public final boolean isIncreasingOrder;
	
	private SortingMethod() {
		this(null);
	}
	private SortingMethod(String columnName) {
		this.columnName = columnName;
		this.isIncreasingOrder = !name().endsWith("_DECREASING");
	}
	
	public SortingMethod opposite() {
		switch (this) {
		case ALPHABETICALLY_INCREASING:
			return ALPHABETICALLY_DECREASING;
		case ALPHABETICALLY_DECREASING:
			return ALPHABETICALLY_INCREASING;

		case READ_TIME_DECREASING:
			return READ_TIME_INCREASING;
		case READ_TIME_INCREASING:
			return READ_TIME_DECREASING;

		case UPDATE_TIME_DECREASING:
			return UPDATE_TIME_INCREASING;
		case UPDATE_TIME_INCREASING:
			return UPDATE_TIME_DECREASING;

		case RANKS_INCREASING:
			return RANKS_DECREASING;
		case RANKS_DECREASING:
			return RANKS_INCREASING;
		/*
		 * case DELETE_QUEUED:
			return DELETE_QUEUED;
		case FAVORITES:
			return FAVORITES;
		 */
		default:
			return null;
		}
	}
}
