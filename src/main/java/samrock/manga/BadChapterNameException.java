package samrock.manga;

public class BadChapterNameException extends Exception {
    public BadChapterNameException(String string) {
        super(string);
    }
    public BadChapterNameException(String string, Exception e) {
        super(string, e);
    }
    private static final long serialVersionUID = 9127290971422990782L;
}
