package samrock.manga.maneger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;

import sam.collection.IntList;
import sam.functions.IOExceptionConsumer;
import sam.io.fileutils.FileNameSanitizer;
import sam.manga.samrock.chapters.ChaptersMeta;
import sam.nopkg.Junk;
import sam.reference.WeakAndLazy;
import samrock.Utils;
import samrock.manga.Chapter;
import samrock.manga.Manga;

abstract class IndexedManga extends Manga implements Indexed {
	private IntList deletedChaps;
	private Map<Integer, String> renamed;
	private final IndexedMinimalManga manga;
	private static final Logger LOGGER = Utils.getLogger(IndexedManga.class);

	public IndexedManga(IndexedMinimalManga manga, ResultSet rs) throws SQLException {
		super(rs);
		this.manga = manga;
	}
	
	@Override
	public int getIndex() {
		return manga.index;
	}
	
	void setUnreadCount(int unreadCount) {
		this.unreadCount = unreadCount;
	}
	@Override
	public int getMangaId() {
		return super.getMangaId();
	}
	IntList getDeletedChaptersIds() {
		return deletedChaps;
	}
	public void setReadCount(int readCount) {
		this.readCount = readCount;
	}
	
	@Override
	protected boolean renameChapter(Chapter chapter, String newName, IOExceptionConsumer<String> filenameSetter) throws IOException {
		Junk.notYetImplemented();
		
		newName = FileNameSanitizer.sanitize(newName);

		if(newName == null || newName.isEmpty())
			throw new IOException("Failed: newName Cannot be null/empty");

		Path src = chapter.getFilePath();

		if(Files.notExists(src))
			throw new IOException("Failed: File does not exists");

		if(chapter.getTitle().equals(newName))
			return true;

		newName = newName.concat(".jpeg");
		Path target = src.resolveSibling(newName);

		if(Files.exists(target))
			throw new IOException("Failed, Duplicate Name Error");

		String nameBackup  = chapter.getFileName();
		Path dir = getDir();
		
		try {
			FileTime fileTime = Files.getLastModifiedTime(dir);
			Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
			filenameSetter.accept(newName);
			nameBackup = null;
			Files.setLastModifiedTime(dir, fileTime);
			if(renamed == null)
				renamed = new HashMap<>();
			
			renamed.put(chapter.getChapterId(), newName);
			return true;
		} catch (IOException|NullPointerException e) {
			if(nameBackup != null)
				filenameSetter.accept(nameBackup);
			else {
				LOGGER.error("Failed: Files.setLastModifiedTime(mangaFolder = '{}', fileTime);", dir, e);
				return true;
			}
			throw new IOException(e.toString(), e);
		}
	}
	@Override
	protected boolean deleteChapterFile(Chapter c) throws IOException {
		Objects.requireNonNull(c);
		Path file = c.getFilePath();

		Utils.delete(file);
		
		if(deletedChaps == null)
			deletedChaps = new IntList();
		deletedChaps.add(c.getChapterId());
		
		return true;
	}
	public boolean isModified() {
		return manga.isModified();
	}

	@Override
	protected void onModified() {
		manga.onModified();
	}
	
	private static final WeakAndLazy<ArrayList<ChapImpl>> wbuffer = new WeakAndLazy<>(ArrayList::new);
	private static final String SELECT_CHAPTERS = "SELECT * FROM "+ChaptersMeta.CHAPTERS_TABLE_NAME+ " WHERE "+ChaptersMeta.MANGA_ID + " = ";
	private static final ChapImpl[] EMPTY = new ChapImpl[0];
	private static final Comparator<ChapImpl> comparator = Comparator.comparingDouble(ChapImpl::getNumber).thenComparing(ChapImpl::getFileName);

	@Override
	protected ChapImpl[] loadChapters() {
		synchronized (wbuffer) {
			List<ChapImpl> list = wbuffer.get();
			list.clear();
			try {
				db().iterate(SELECT_CHAPTERS.concat(Integer.toString(manga_id)), rs -> list.add(new ChapImpl(rs)));
				if(list.isEmpty())
					return EMPTY;
				
				ChapImpl[] cs = list.toArray(new ChapImpl[list.size()]);
				Arrays.sort(cs, comparator);
				return cs;
			} catch (SQLException e) {
				LOGGER.error("failed to load chapters for: {}", this, e);
				//FIXME display error to User
				return EMPTY;
			}
		}
	}
	
	protected abstract DB db();
}
