package samrock.manga;

import static sam.manga.samrock.chapters.ChaptersMeta.TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.CHAPTER_ORDERING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import sam.collection.Iterables;
import sam.manga.samrock.SamrockDB;
import sam.manga.samrock.chapters.ChapterUtils;
import sam.myutils.Checker;
import sam.sql.querymaker.QueryMaker;
import samrock.manga.chapter.Chapter;

public class Chapters implements Iterable<Chapter> {
	private final Manga manga;
	private  ArrayList<Chapter> chapters;
	private boolean loaded;

	/**
	 * chapter_ordering = true -> increasing order <br>
	 * chapter_ordering = false -> decreasing order<br> 
	 */
	private boolean chapterOrdering; // = isInIncreasingOrder

	Chapters(Manga manga, ResultSet rs) throws SQLException {
		this.manga = manga;
		chapterOrdering = rs.getBoolean(CHAPTER_ORDERING);
	}
	public Manga getManga() {
		return manga;
	}
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * To all Chapters 
	 * c.setWatcher(chapterWatcher);
	 * c.setMangaFolder(mangaFolder);
	 */
	void prepareChapters() {
		this.chapters.removeIf(c -> {
			if(c.isDeleted()) {
				addDeleted(c);
				return true;
			}
			return false;
		}); 

		orderChapters();

		for (Chapter c : chapters) {
			c.setWatcher(manga.getChapterWatcher());
			c.setMangaFolder(manga.getMangaFolderPath());
		}
	}
	private void orderChapters() {
		chapters.sort(Comparator.naturalOrder());

		if(!isChaptersInIncreasingOrder())
			Collections.reverse(chapters);
	}

	public ChapterItr chapterIterator() {
		return chapterIterator(0);
	}
	public ChapterItr chapterIterator(int index) {
		return new ChapterItr(index);
	}
	public class ChapterItr {
			private final ListIterator<Chapter> itr;
			private Chapter current;
			
			ChapterItr(int index) {
				itr =  chapters.listIterator(index);
			}
			
			public Chapter current() {
				return current;
			}
			public void delete() throws IOException {
				if(Chapters.this.delete(current)) {
					itr.remove();
					if(hasPrevious())
						current = previous();
					else if(hasNext())
						current = next();
					else
						current = null;
				}
			}
			public int previousIndex() { return itr.previousIndex(); }
			public int nextIndex() { return itr.nextIndex(); }
			
			public Chapter previous() { return current = itr.previous(); }
			public Chapter next() { return current = itr.next();  }

			public boolean hasPrevious() {return itr.hasPrevious() ;}
			public boolean hasNext() { return itr.hasNext() ; }
		};

	public boolean isChaptersInIncreasingOrder(){
		return chapterOrdering;
	}
	public void reverseChaptersOrder() {
		manga.isModified = true;
		chapterOrdering = !chapterOrdering;
		Collections.reverse(chapters);
	}
	public Stream<Chapter> stream(){
		return chapters.stream();
	}
	public int size() {
		return chapters.size();
	}
	public boolean isEmpty() {
		return chapters.isEmpty();
	}
	private ArrayList<Chapter> _deletedChapters;
	List<Chapter> getDeletedChapters(){
		return _deletedChapters == null ? null : Collections.unmodifiableList(_deletedChapters);
	}
	private void addDeleted(Chapter c) {
		if(_deletedChapters == null)
			_deletedChapters = new ArrayList<>();
		_deletedChapters.add(c);
	}

	public boolean delete(Chapter c) throws IOException {
		Path src = c.getGetChapterFilePath();
		FileTime time = Files.getLastModifiedTime(manga.getMangaFolderPath());

		if(Files.notExists(src) || Utils.delete(src)){
			Files.setLastModifiedTime(manga.getMangaFolderPath(), time);
			c.setDeleted(true);
		}
		return c.isDeleted();
	}
	public void set(Chapter e) {
		throw new IllegalAccessError("not allowed");
	}
	public void add(Chapter e) {
		throw new IllegalAccessError("not allowed");
	}
	public boolean reloadChapters() {
		try {
			this.chapters = (ArrayList<Chapter>) ChapterUtils.reloadChapters(manga.getMangaFolderPath(), chapters, Chapter::new);
			prepareChapters();
			return true;
		} catch (IOException e) {
			LoggerFactory.getLogger(getClass()).error("failed to reaload chapters", e);
		}
		return false;
	}
	@Override
	public Iterator<Chapter> iterator() {
		return chapters.iterator();
	}
	public Chapter first() {
		return isEmpty() ? null : chapters.get(0);
	}
	public Chapter last() {
		return isEmpty() ? null : chapters.get(chapters.size() - 1);
	}
	void unloaded() {
		if(!loaded)
			throw new IllegalStateException("no chapters loaded");

		chapters.removeIf(s -> s == null || s.isChapterNotInDb() || s.isDeleted());
		_deletedChapters = null;
		loaded = false;
	}

	void load(SamrockDB db) throws SQLException {
		if(loaded)
			throw new IllegalStateException("chapters already loaded");

		String sql = QueryMaker.qm().selectAllFrom(TABLE_NAME).where(w -> {
			w.eq(MANGA_ID, manga.manga_id);
			if(!Checker.isEmpty(chapters))
				w.and().not().in(CHAPTER_ID, Iterables.map(chapters, Chapter::getId));
			return w;
		}).build();

		if(chapters == null)
			chapters = new ArrayList<>(); 

		db.collect(sql, chapters, Chapter::new);
		loaded = true;
	}
	public int findIndex(int chapter_id) {
		for (int i = 0; i < chapters.size(); i++) {
			if(chapters.get(i).getId() == chapter_id)
				return i;
		}
		return -1;
	}
	public Chapter findChapter(int chapter_id) {
		for (int i = 0; i < chapters.size(); i++) {
			if(chapters.get(i).getId() == chapter_id)
				return chapters.get(i);
		}
		return null;
	}
	public Chapter get(int index) {
		return chapters.get(index);
	}
	public ArrayList<Chapter> chaptersCopy() {
		return new ArrayList<>(chapters);
	}
}
