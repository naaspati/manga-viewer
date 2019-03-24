package samrock.manga.maneger;

import java.util.function.IntConsumer;

import sam.collection.IntSet;
import samrock.manga.MinimalManga;
import samrock.manga.maneger.api.DeleteQueue;
import samrock.manga.maneger.api.Operation;

abstract class DeleteQueueImpl extends ListenersImpl<MinimalManga, Operation> implements DeleteQueue {
	private IntSet deleted; 
	
	@Override
	public void add(MinimalManga m) {
		if(deleted == null)
			deleted = new IntSet();
		
		if(deleted.add(indexOf(m)))
			notifyWatchers(m, Operation.ADDED);
	}
	@Override
	public void remove(MinimalManga m) {
		if(!isEmpty() && deleted.remove(indexOf(m)))
			notifyWatchers(m, Operation.REMOVED);
	}

	@Override
	public boolean contains(MinimalManga m) {
		return isEmpty() ? false : deleted.contains(indexOf(m));
	}
	@Override
	public boolean isEmpty(){
		return deleted == null || deleted.isEmpty();
	}
	@Override
	public MinimalManga[] values() {
		if(isEmpty())
			return new MinimalManga[0];
		else {
			MinimalManga[] m = new MinimalManga[size()];
			deleted.forEach(new IntConsumer() {
				int n = 0;
				@Override
				public void accept(int value) {
					m[n++] = getMangaByIndex(value);
				}
			});
			return m;
		}
	}

	protected abstract int indexOf(MinimalManga m);
	protected abstract MinimalManga getMangaByIndex(int index);
	protected abstract int getMangaIdAtIndex(int index);
	
	private int size() {
		return isEmpty() ? 0 : deleted.size();
	}
	@Override
	public int[] toMangaIdsArray() {
		int[] array = new int[deleted.size()];
		deleted.forEach(new IntConsumer() {
			int n = 0;
			@Override
			public void accept(int value) {
				array[n++] = getMangaIdAtIndex(value);
			}
		});
		return array;
	}
}