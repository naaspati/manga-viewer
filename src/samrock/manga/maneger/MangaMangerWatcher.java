package samrock.manga.maneger;

@FunctionalInterface
public interface MangaMangerWatcher  {
    public void changed(MangaManegerStatus change);
}
