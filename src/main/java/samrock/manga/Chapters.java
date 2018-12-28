package samrock.manga;

import java.awt.IllegalComponentStateException;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import samrock.manga.Manga.Chapter;

public class Chapters implements Closeable {
	private final ListIterator<Chapter> iter;
	int read_count, unread_count;
	private boolean closed;
	private final Runnable onclose;
	
	public Chapters(List<Chapter> chapters, Runnable onclose) {
		chapters.forEach(c -> {
			if(c.isRead())
				read_count++;
			else
				unread_count++;
		});
		
		this.iter = chapters.listIterator();
		this.onclose = onclose;
	}
	
	@Override
	public void close() throws IOException {
		if(closed)
			throw new IllegalComponentStateException("closed");
		
		closed = true;
		onclose.run();
	}
	public boolean hasNext() {
		return iter.hasNext();
	}
	public Chapter next() {
		return iter.next();
	}
	public boolean hasPrevious() {
		return iter.hasPrevious();
	}
	public void forEachRemaining(Consumer<? super Chapter> action) {
		iter.forEachRemaining(action);
	}
	public Chapter previous() {
		return iter.previous();
	}
	public int nextIndex() {
		return iter.nextIndex();
	}
	public int previousIndex() {
		return iter.previousIndex();
	}
}
