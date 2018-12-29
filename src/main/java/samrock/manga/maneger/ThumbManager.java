package samrock.manga.maneger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sam.myutils.Checker;
import sam.reference.WeakAndLazy;
import samrock.manga.MinimalManga;
import samrock.utils.SoftList;
import samrock.utils.Utils;

public class ThumbManager {
	public ThumbManager() {
		//TODO
	}
	public List<File> getThumbsPaths(MinimalManga manga) {
		List<File> all = thumbs.get(manga.getMangaIndex());
		if(all != null)
			return all;
		
		@SuppressWarnings("deprecation")
		int manga_id = manga.getMangaId();
		File thumb = new File(Utils.THUMB_FOLDER, manga_id+".jpg");
		File folder = new File(Utils.THUMB_FOLDER, Integer.toString(manga_id));
		
		ArrayList<File> files = new ArrayList<>();
		
		if(thumb.exists())
			files.add(thumb);
		if(folder.exists()) {
			for (String s : folder.list()) {
				if(s.endsWith(".jpg") || s.endsWith(".jpeg"))
				   files.add(new File(folder, s));
			}
		}
		files.trimToSize();
		all = Collections.unmodifiableList(files);
		thumbs.set(manga.getMangaIndex(), all);
		return all;
	}
	
	private final WeakAndLazy<Random> random = new WeakAndLazy<>(Random::new);
	
	public File getRandomThumbPath(MinimalManga manga) {
		List<File> thumbs = getThumbsPaths(manga);
		if(Checker.isEmpty(thumbs))
			return null;
		
		return thumbs.size() == 1 ? thumbs.get(0) : thumbs.get(random.get().nextInt(thumbs.size()));
	}
}
