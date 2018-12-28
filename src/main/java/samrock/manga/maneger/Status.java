package samrock.manga.maneger;

enum Status {
	ONGOING("On Going"),
	COMPLETED("Completed"),
	STATUS_ALL("All"),
	READ_0("Read = 0"), 
	READ_NOT_0("Read != 0"),
	READ_ALL("All"),
	UNREAD_0("Unread = 0"), 
	UNREAD_NOT_0("Unread != 0"),
	UNREAD_ALL("All");

	final String text;

	private Status(String text) {
		this.text = text;
	}
}