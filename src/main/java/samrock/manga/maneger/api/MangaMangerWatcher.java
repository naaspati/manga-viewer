package samrock.manga.maneger.api;

@FunctionalInterface
public interface MangaMangerWatcher  {
    public void changed(MangaManegerStatus change);
}
