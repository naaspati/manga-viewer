package samrock.manga;

import static sam.manga.samrock.mangas.MangasMeta.CHAPTER_ORDERING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import sam.collection.Iterators;
import sam.logging.MyLoggerFactory;
import sam.manga.samrock.chapters.ChapterUtils;
import sam.myutils.Checker;
import sam.myutils.ThrowException;
import sam.nopkg.Junk;
import samrock.manga.Chapters.Chapter;

public class Chapters implements Iterable<Chapter> {
	private static final Logger LOGGER = MyLoggerFactory.logger(Chapters.class);

	private List<Chapter> chapters;
	private final Manga manga;
	/**
	 * chapter_ordering = true -> increasing order <br>
	 * chapter_ordering = false -> decreasing order<br> 
	 */
	private boolean chapterOrdering; // = isInIncreasingOrder

	int read_count, unread_count;

	public Chapters(Manga manga, ResultSet rs) throws SQLException {
		chapterOrdering = rs.getBoolean(CHAPTER_ORDERING);
		this.manga = manga;
	}
	public int size() {
		load();
		return isEmpty() ? 0 : chapters.size();
	}
	public boolean isEmpty() {
		load();
		return Checker.isEmpty(chapters);
	}
	public Order getOrder() {
		return chapterOrdering ? Order.INCREASING : Order.DECREASING;
	}
	public void flip() {
		Collections.reverse(chapters);
		chapterOrdering = !chapterOrdering; 
	}

	public List<Chapter> getChapters() {
		load();
		return Checker.isEmpty(chapters) ? Collections.emptyList() : Collections.unmodifiableList(chapters);
	}

	private boolean chapters_loaded;

	private void load() {
		try {
			if(!chapters_loaded)
				this.chapters = manga.loadChapters();
		} catch (Exception e) {
			new RuntimeException(e);
		}

		chapters.forEach(c -> {
			if(c.isRead())
				read_count++;
			else
				unread_count++;
		});
	}

	@Override
	public Iterator<Chapter> iterator() {
		load();
		return Checker.isEmpty(chapters) ? Iterators.empty() : chapters.iterator();
	}
	public boolean reload() {
		try {
			this.chapters = manga.reloadChapters(chapters);
			resetCounts();
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "failed to reaload chapters", e);
		}
		return false;
	}
	void chapterDeleted(Chapter c) {
		manga.modified();

		if(c.isRead())
			read_count--;
		else
			unread_count--;

		String s = removeMultiFileNumber(c.getFileName());

		if(s.equals(c.getFileName()))
			manga.chapCountPc--;
		else
			resetCounts();
	}

	private static final Pattern pattern = Pattern.compile(" - \\d+\\.jpe?g$");

	/**
	 * recounts again chapCountPc, read_count, unread_count
	 */
	public void resetCounts(){
		manga.modified();
		manga.chapCountPc = 0;
		read_count = 0;
		unread_count = 0;

		HashSet<String> set = new HashSet<>();

		for (Chapter c : chapters) {
			if(c.isRead()) read_count++;
			else unread_count++;

			String s = removeMultiFileNumber(c.getFileName());

			if(s.equals(c.getFileName()))
				manga.chapCountPc++;
			else
				set.add(s);
		}
		manga.chapCountPc = manga.chapCountPc + set.size();
	}
	private String removeMultiFileNumber(String fileName) {
		if(fileName.lastIndexOf('-') > 0)
			return pattern.matcher(fileName).replaceFirst("");

		return fileName; 
	}
	public static StringBuilder commitChaptersChanges(ChapterUtils utils, int manga_id, Iterable<Object> map) {
		// FIXME Auto-generated method stub
		// utils.commitChaptersChanges(ch.getManga().manga_id, Iterables.map(ch, c -> (sam.manga.samrock.chapters.Chapter)c));
		return Junk.notYetImplemented();
	}
	protected Chapter _newChapter(ResultSet rs) throws SQLException {
		return new Chapter(rs);
	}
	protected void onDeleteChapter(Chapter c) { }

	public class Chapter extends sam.manga.samrock.chapters.Chapter {
		public Chapter(double number, String fileName, boolean isRead) {
			super(number, fileName, isRead);
		}
		public Chapter(double number, String fileName) {
			super(number, fileName);
		}
		public Chapter(ResultSet rs) throws SQLException {
			super(rs);
		}
		@Override
		public void setRead(boolean read) {
			if(isRead() == read)
				return;

			super.setRead(read);
			if(isRead()) {
				read_count++;
				unread_count--;
			} else {
				read_count--;
				unread_count++;
			}
		}
		@Override
		public void setDeleted(boolean deleted) {
			if(deleted == isDeleted())
				return;
			super.setDeleted(deleted);

			if(deleted) {
				if(isRead())
					read_count--;
				else
					unread_count--;
			} else {
				if(isRead())
					read_count++;
				else
					unread_count++;
			}

			onDeleteChapter(this);
		}
		@Override
		public void setFileName(String name) {
			ThrowException.illegalAccessError();
		}
		@Override
		public void setNumber(double number) {
			ThrowException.illegalAccessError();
		}
		public boolean rename(String oldName) throws BadChapterNameException{
			// TODO Auto-generated method stub
			return Junk.notYetImplemented();
		}
		public void delete() {
			// TODO Auto-generated method stub
			Junk.notYetImplemented();
		}
		public String getName() {
			// TODO Auto-generated method stub
			return Junk.notYetImplemented();
		}
		public boolean chapterFileExists() {
			// TODO Auto-generated method stub
			return Junk.notYetImplemented();
		}
		public Path getGetChapterFilePath() {
			// TODO Auto-generated method stub
			return Junk.notYetImplemented();
		}
	}

	public Manga getManga() {
		return manga;
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

	
	/**
	 * was public, hiding it later will use in final operation (when MangaViewer is cloed or editor is closed this will confirm deleted chapters)
	 * and delete/rename then in batch, 
	 * @param c
	 * @return
	 * @throws IOException
	 */
	private boolean delete(Chapter c) throws IOException {
		Path src = c.getGetChapterFilePath();
		FileTime time = Files.getLastModifiedTime(manga.getMangaFolderPath());

		if(Files.notExists(src) || Files.deleteIfExists(src)){
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
	public Chapter first() {
		return isEmpty() ? null : chapters.get(0);
	}
	public Chapter last() {
		return isEmpty() ? null : chapters.get(chapters.size() - 1);
	}
	public int findIndex(int chapter_id) {
		for (int i = 0; i < chapters.size(); i++) {
			if(chapters.get(i).getChapterId() == chapter_id)
				return i;
		}
		return -1;
	}
	public Chapter findChapter(int chapter_id) {
		for (int i = 0; i < chapters.size(); i++) {
			if(chapters.get(i).getChapterId() == chapter_id)
				return chapters.get(i);
		}
		return null;
	}
	public Chapter get(int index) {
		load();
		return chapters.get(index);
	}
}
