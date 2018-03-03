package samrock.manga.chapter;

@FunctionalInterface
public interface ChapterWatcher {
    public void changed(ChapterStatus cs);
}
