package samrock.manga.maneger.api;

public interface Tags {
	String getTag(int tagId);
	String[] parseTags(String tags);
}
