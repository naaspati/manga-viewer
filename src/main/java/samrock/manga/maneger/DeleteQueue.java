package samrock.manga.maneger;

import sam.collection.IntSet;
import samrock.manga.MinimalManga;

public class DeleteQueue extends Listeners<MinimalManga, Operation>{
	private IntSet deleted; 
	
	private static int id(MinimalManga m) {
		return MangaManeger.mangaIdOf(m);
	}
	public void add(MinimalManga m) {
		if(deleted == null)
			deleted = new IntSet();
		
		if(deleted.add(id(m)))
			notifyWatchers(m, Operation.ADDED);
	}
	public void remove(MinimalManga m) {
		if(!isEmpty() && deleted.remove(id(m)))
			notifyWatchers(m, Operation.REMOVED);
	}

	public boolean contains(MinimalManga m) {
		return isEmpty() ? false : deleted.contains(id(m));
	}
	public boolean isEmpty(){
		return deleted == null || deleted.isEmpty();
	}
	public MinimalManga[] values() {
		if(isEmpty())
			return new MinimalManga[0];
		else {
			MinimalManga[] m = new MinimalManga[size()];
			int n[] = {0};
			deleted.forEach(id -> m[n[0]++] = MangaManeger.getMinimalManga(id));
			return m;
		}
	}
	private int size() {
		return isEmpty() ? 0 : deleted.size();
	}
	public int[] toArray() {
		return isEmpty() ? new int[0] : deleted.toArray();
	}
}