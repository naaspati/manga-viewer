package samrock.manga.maneger;

import static samrock.Utils.THUMB_FOLDER;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.inject.Singleton;

import sam.nopkg.EnsureSingleton;
import sam.reference.ReferenceType;
import sam.reference.WeakAndLazy;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.api.ThumbManager;
import samrock.manga.maneger.api.UnmodifiableArray;

@Singleton
abstract class ThumbManagerImpl implements ThumbManager {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{
		singleton.init();
	}
	
	IndexedReferenceList<UnmodifiableArray<File>> thumbs;
	
	ThumbManagerImpl(int size) {
		this.thumbs = new IndexedReferenceList<>(size, getClass(), ReferenceType.WEAK);
	}
	
	protected abstract int indexOf(MinimalManga manga);
	
	@Override
	public UnmodifiableArray<File> getThumbsPaths(MinimalManga manga) {
		int index = indexOf(manga);
		UnmodifiableArray<File> all = thumbs.get(index);
		if(all != null)
			return all;
		
		int manga_id = manga.manga_id;
		File thumb = new File(THUMB_FOLDER, manga_id+".jpg");
		File folder = new File(THUMB_FOLDER, Integer.toString(manga_id));
		
		if(thumb.exists())
			all = new UnmodifiableArrayImpl<>(new File[]{thumb});
		if(folder.exists()) {
			String[] str = folder.list();
			File[] files = new File[str.length];
			
			int n = 0;
			for (String s : str) {
				if(s.endsWith(".jpg") || s.endsWith(".jpeg"))
				   files[n++] = new File(folder, s);
			}
			
			all = n != files.length ? new UnmodifiableArrayImpl<>(Arrays.copyOf(files, n)) : new UnmodifiableArrayImpl<>(files);  
		}
		
		thumbs.set(index, all);
		return all;
	}

	private final WeakAndLazy<Random> random = new WeakAndLazy<>(Random::new);
	
	@Override
	public File getRandomThumbPath(MinimalManga manga) {
		UnmodifiableArray<File> thumbs = getThumbsPaths(manga);
		if(thumbs == null || thumbs.size() == 0)
			return null;
		
		return thumbs.size() == 1 ? thumbs.get(0) : thumbs.get(random.get().nextInt(thumbs.size()));
	}
}
