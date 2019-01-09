package samrock.manga.maneger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import sam.collection.IntList;
import sam.io.fileutils.FileNameSanitizer;
import sam.io.serilizers.IOExceptionConsumer;
import sam.logging.MyLoggerFactory;
import sam.nopkg.Junk;
import samrock.manga.Chapters.Chapter;
import samrock.manga.Manga;
import samrock.manga.recents.ChapterSavePoint;
import samrock.utils.Utils;

class IndexedManga extends Manga implements IIndexedManga {
	private IntList deletedChaps;
	private Map<Integer, String> renamed;
	private final IndexedMinimalManga manga;
	private static final Logger LOGGER = MyLoggerFactory.logger(IndexedManga.class);

	public IndexedManga(IndexedMinimalManga manga, ResultSet rs, int version) throws SQLException {
		super(rs, version);
		this.manga = manga;
	}
	@Override
	public int getIndex() {
		return manga.getIndex();
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
	Chapter newChapter(ResultSet rs) throws SQLException {
		return super._newChapter(rs);
	}
	public void setReadCount(int readCount) {
		this.readCount = readCount;
	}
	/** 
	 * void setUnmodifed() {
		version = init_version;
	}
	 * @return
	 */
	public int getVersion() {
		return version;
	}
	void setVersion(int version) {
		this.version = version;
	}
	@Override
	protected List<Chapter> loadChapters() throws IOException, SQLException {
		return MangaManeger.loadChapters(this);
	}
	@Override
	protected List<Chapter> reloadChapters(List<Chapter> loadedChapters) throws IOException, SQLException {
		return MangaManeger.reloadChapters(this, loadedChapters);
	}

	@Override
	protected String[] parseTags(String tags) {
		return MangaManeger.tagsDao().parseTags(tags);
	}
	@Override
	protected ChapterSavePoint loadSavePoint() {
		return MangaManeger.recentsDao().getFullSavePoint(this, this::getChapter);
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
				LOGGER.log(Level.SEVERE, String.format("Failed: Files.setLastModifiedTime(mangaFolder = %s, fileTime);", dir), e);
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
}
