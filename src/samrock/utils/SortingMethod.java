package samrock.utils;

/**
 * here is the logic of sorting time is, today < yesterday <  day before yesterday < .....
 * <br>mathmatically Math.abs(today - given_date)
 * <br>rest SortingMethod Follows natural Order
 * @author Sameer
 */
public enum SortingMethod{
	/**
	 * A -> Z
	 */
	ALPHABETICALLY_INCREASING,
	/**
	 * Z -> A
	 */
	ALPHABETICALLY_DECREASING,

	/*
	 * from 1->100....
	 */
	RANKS_INCREASING,
	/**
	 * ...100, 99, 98...-> 1
	 */
	RANKS_DECREASING,

	/**
	 * from Read long ago -> Read Recently
	 */
	READ_TIME_DECREASING,
	/**
	 * from Recently Read -> Read long ago
	 */
	READ_TIME_INCREASING,

	/**
	 * Recently Updated -> Updated long ago
	 */
	UPDATE_TIME_DECREASING,
	/**
	 * Updated long ago -> Recently Updated
	 */
	UPDATE_TIME_INCREASING,

	DELETE_QUEUED,

	FAVORITES;

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
		case DELETE_QUEUED:
			return DELETE_QUEUED;
		case FAVORITES:
			return FAVORITES;
		default:
			return null;
		}
	}
}
