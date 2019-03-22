package samrock.manga;

import static sam.manga.samrock.chapters.ChaptersMeta.CHAPTER_ID;
import static sam.manga.samrock.chapters.ChaptersMeta.NAME;
import static sam.manga.samrock.chapters.ChaptersMeta.NUMBER;
import static sam.manga.samrock.chapters.ChaptersMeta.READ;
import static sam.manga.samrock.mangas.MangasMeta.CHAPTER_ORDERING;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.OptionalDouble;
import java.util.logging.Level;
import org.slf4j.Logger;
import java.util.regex.Pattern;

import sam.collection.Iterators;
import sam.manga.samrock.chapters.ChapterWithId;
import sam.manga.samrock.chapters.MinimalChapter;
import sam.myutils.Checker;
import sam.myutils.ThrowException;
import samrock.Utils;
import samrock.manga.Chapters.Chapter;
import samrock.manga.maneger.MangaManeger;
import samrock.manga.recents.ChapterSavePoint;

public class Chapters implements Iterable<Chapter> {
	private static final Logger LOGGER = Utils.getLogger(Chapters.class);

	private List<Chapter> _chapters;
	private final Manga manga;
	/**
	 * chapter_ordering = true -> increasing order <br>
	 * chapter_ordering = false -> decreasing order<br> 
	 */
	private boolean chapterOrdering; // = isInIncreasingOrder

	int read_count, unread_count;

	public Chapters(Manga manga, ResultSet rs) throws SQLException {
		this.manga = manga;
	}
	public int size() {
		return isEmpty() ? 0 : chapters().size();
	}
	public boolean isEmpty() {
		return Checker.isEmpty(chapters());
	}
	public Order getOrder() {
		return chapterOrdering ? Order.INCREASING : Order.DECREASING;
	}
	public void flip() {
		Collections.reverse(chapters());
		chapterOrdering = !chapterOrdering; 
	}

	public List<Chapter> getChapters() {
		return isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(_chapters);
	}

	private boolean chapters_loaded;

	private List<Chapter> chapters() {
		if(!chapters_loaded) {
			chapters_loaded = true;
			try {
				this._chapters = manga.loadChapters();
			} catch (Exception e) {
				new RuntimeException(e);
			}
		}
		
		_chapters.forEach(c -> {
			if(c.isRead())
				read_count++;
			else
				unread_count++;
		});
		return _chapters;
	}

	@Override
	public Iterator<Chapter> iterator() {
		return isEmpty() ? Iterators.empty() : chapters().iterator();
	}
	public boolean reload() {
		try {
			this._chapters = manga.reloadChapters(chapters());
			resetCounts();
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "failed to reaload chapters", e);
		}
		return false;
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

		for (Chapter c : chapters()) {
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
	protected Chapter _newChapter(ResultSet rs) throws SQLException {
		return new Chapter(rs);
	}
	protected void onDeleteChapter(Chapter c) { }

	public class Chapter implements ChapterWithId {
		private final int id;
		private String filename; //file name
		private double number; // number
		private boolean read; // isRead ?

		private Chapter(double number, String fileName, boolean isRead) {
			this.id = -1;
			this.number = number;
			this.filename = fileName;
			this.read = isRead;
		}
		public Chapter(ResultSet rs) throws SQLException {
			this.id = rs.getInt(CHAPTER_ID);
			this.filename = rs.getString(NAME);
			this.number = rs.getDouble(NUMBER);
			this.read = rs.getBoolean(READ);
		}
		public boolean isRead() {
			return read;
		}
		public void setRead(boolean read) {
			if(this.read == read)
				return;

			this.read = read;
			if(read) {
				read_count++;
				unread_count--;
			} else {
				read_count--;
				unread_count++;
			}
		}
		public Path getFilePath() {
			return manga.getDir().resolve(filename);
		}
		@Override
		public double getNumber() {
			return number;
		}
		private String name;
		@Override
		public String getTitle() {
			return name != null ? name : (name = MinimalChapter.getTitleFromFileName(filename));
		}
		private void setFileName(String chapterName) throws IOException {
			if(Checker.isEmptyTrimmed(chapterName))
				throw new IOException("chapterName: '"+chapterName+"'");

			OptionalDouble number =  MinimalChapter.parseChapterNumber(chapterName);
			if(!number.isPresent())
				throw new IOException("number not found in chapterName: '"+chapterName+"'");

			double d = number.getAsDouble();

			if(d != this.number)
				throw new IOException("new number is not same as old");

			this.name = null; 
			this.filename = chapterName;
		}
		@Override
		public String getFileName() {
			return filename;
		}
		@Override
		public int getChapterId() {
			return id;
		}
		/**
		 * 
		 * @param newName
		 * @return null if renaming successes else fail reason
		 * @throws IOException 
		 */
		public boolean rename(String newName) throws IOException {
			return manga.renameChapter(this,newName, this::setFileName);
		}
		byte _exists = -1; 
		public boolean chapterFileExists() {
			if(_exists == -1)
				_exists =  (byte) (Files.exists(getFilePath()) ? 1 : 0);
			
			return _exists == 1;
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
	public class ChapterItr implements Closeable {
		private final ListIterator<Chapter> itr;
		private Chapter current;

		ChapterItr(int index) {
			itr =  chapters().listIterator(index);
		}

		public Chapter current() {
			return current;
		}
		public boolean delete() throws IOException {
			if(Chapters.this.delete0(current, itr)) {
				if(hasPrevious())
					current = previous();
				else if(hasNext())
					current = next();
				else
					current = null;

				return true;
			}
			return false;
		}
		public int previousIndex() { return itr.previousIndex(); }
		public int nextIndex() { return itr.nextIndex(); }

		public Chapter previous() { return current = itr.previous(); }
		public Chapter next() { return current = itr.next();  }

		public boolean hasPrevious() {return itr.hasPrevious() ;}
		public boolean hasNext() { return itr.hasNext() ; }

		public void close() {
			//TODO  
			if(manga.getChapCountPc() == 0)
				MangaManeger.addMangaToDeleteQueue(manga);
		}
	};

	public void set(Chapter e) {
		throw new IllegalAccessError("not allowed");
	}
	@SuppressWarnings("rawtypes")
	private boolean delete0(Chapter c, Object deleteFrom) throws IOException {
		if(!manga.deleteChapterFile(c))
			return false;

		if(deleteFrom instanceof Iterator)
			((Iterator) deleteFrom).remove();
		else 
			((List)deleteFrom).remove(c);

		if(c.isRead())
			read_count--;
		else
			unread_count--;

		String s = removeMultiFileNumber(c.getFileName());

		if(s.equals(c.getFileName()))
			manga.chapCountPc--;
		else
			resetCounts();

		manga.modified();
		return  true;
	}
	public boolean delete(Chapter c) throws IOException {
		return delete0(c, chapters());
	}
	public void add(Chapter e) {
		ThrowException.illegalAccessError("not allowed");
	}
	public Chapter first() {
		return isEmpty() ? null : chapters().get(0);
	}
	public Chapter last() {
		return isEmpty() ? null : chapters().get(chapters().size() - 1);
	}
	public int findChapter(ChapterSavePoint savePoint) {
		if(savePoint == null)
			return -1;

		int id = savePoint.getChapterId();

		for (int i = 0; i < chapters().size(); i++) {
			if(chapters().get(i).getChapterId() == id)
				return i;
		}

		String name = savePoint.getChapterFileName();

		if(!Checker.isEmptyTrimmed(name)) {
			for (int i = 0; i < chapters().size(); i++) {
				if(name.equals(chapters().get(i).getFileName()))
					return i;
			}	
		}

		return -1;
	}
	public Chapter findChapter(int chapter_id) {
		for (int i = 0; i < chapters().size(); i++) {
			if(chapters().get(i).getChapterId() == chapter_id)
				return chapters().get(i);
		}
		return null;
	}
	public Chapter get(int index) {
		return chapters().get(index);
	}
	public int indexOf(Chapter chapter) {
		return chapters().indexOf(chapter);
	}
	public Chapter getChapterByChapterId(int chapter_id) {
		return chapters().stream().filter(c -> c.id == chapter_id).findFirst().orElse(null);
	}
}
