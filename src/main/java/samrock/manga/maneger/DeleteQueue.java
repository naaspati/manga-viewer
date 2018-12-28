package samrock.manga.maneger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import sam.myutils.Checker;
import samrock.manga.MinimalManga;

public class DeleteQueue extends Listeners<MinimalManga, Type>{
	// manga_id -> manga
	private Map<Integer, MinimalManga> deleted;

	public void add(MinimalManga m) {
		if( deleted == null)
			deleted = new HashMap<>();
		
		if(m.getClass() != IndexedMinimalManga.class)
			m = MangaManeger.getMinimalManga(m.getMangaId());
			
		if(deleted.put(m.getMangaId(), m) == null)
			notifyWatchers(m, Type.ADDED);
	}
	public void remove(MinimalManga m) {
		if(deleted != null && deleted.remove(m.getMangaId()) != null)
			notifyWatchers(m, Type.REMOVED);
	}

	public boolean contains(MinimalManga m) {
		return deleted != null && deleted.containsKey(m.getMangaId());
	}
	public boolean isEmpty(){
		return Checker.isEmpty(deleted);
	}
	public Collection<MinimalManga> values() {
		return  isEmpty() ? Collections.emptyList() : Collections.unmodifiableCollection(deleted.values());
	}
}