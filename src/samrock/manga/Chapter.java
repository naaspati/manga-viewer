package samrock.manga;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import samrock.utils.Utils;

/**
 * original implementation of chapter
 * @author Sameer
 *
 */
public class Chapter implements Comparable<Chapter> {
	public static final int SET_READ = 0x600;
	public static final int SET_UNREAD = 0x601;
	public static final int RENAMED = 0x602;
	public static final int DELETED = 0x603;

	private String name; //chapter_name
	private double number; // chapter_number
	private boolean isRead; // isRead ?

	private IntConsumer watcher;
	private Path mangaFolder;
	private boolean isDeleted = false;

	/**
	 * isRead will be set to false 
	 * @param chapterName
	 */
	public Chapter(String chapterName) {
		setName(chapterName);
		isRead = false;
	}

	public Chapter(String chapterName, boolean isRead) {
		this(chapterName);
		this.isRead = isRead;
	}

	private Chapter(DataInputStream in) throws IOException {
		number = in.readDouble();
		isRead = in.readBoolean();
		name = in.readUTF();
	}

	private void writeChapter(DataOutputStream out) throws IOException{
		out.writeDouble(number);
		out.writeBoolean(isRead);
		out.writeUTF(name);
	}

	public void setWatcher(IntConsumer chapterWatcher) {this.watcher = chapterWatcher;}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setMangaFolder(Path mangaFolder) {this.mangaFolder = mangaFolder;}
	public boolean isRead() {return isRead;}

	/**
	 * return fileName of the strip
	 * @return
	 */
	public String getFileName() {return name;}

	private String extensionLessName;

	/**
	 * return strip filename without extension
	 * @return
	 */
	public String getName() {
		if(extensionLessName == null)
			extensionLessName = name.replaceFirst("\\.jpe?g$", "").trim();

		return extensionLessName;
	}

	/**
	 * 1 = exists, 0 = doesn't exists, -1 = needs to be checked
	 * why not boolean?  this values must be undetemined until it is set, that is not possible with two values
	 * wee need three values undetermined(-1), exists(0), not exists() 
	 */
	private byte chapterFileExits = -1;
	public boolean chapterFileExists() {
		if(chapterFileExits == -1)		
			chapterFileExits = (byte) (Files.exists(getGetChapterFilePath()) ? 1 : 0);

		return chapterFileExits == 1;
	}

	private Path chapterFilePath;
	public Path getGetChapterFilePath() {
		if(chapterFilePath == null)
			chapterFilePath = mangaFolder.resolve(name);

		return chapterFilePath; 
	}

	private void setName(String chapterName) {
		if(chapterName == null || chapterName.trim().isEmpty())
			throw new NullPointerException("chapterName: '"+chapterName+"'");

		this.name = chapterName;
		String numberString = extractChapterNumber(chapterName);
		number = numberString == null ? -1d : Double.parseDouble(numberString);
		chapterFileExits = -1;
		extensionLessName =  null;
		chapterFilePath = null;
	}

	public double getNumber() {return number;}

	/**
	 * 
	 * @param setRead true sets chapter read, false sets chapter unread, if previous value is save as current no changes made, no notifying parent manga 
	 */
	public void setRead(boolean setRead) {
		if(isRead == setRead)
			return;

		isRead = setRead;

		if(watcher != null)
			watcher.accept(isRead ? SET_READ : SET_UNREAD);
	}

	@Override
	public int compareTo(Chapter c) {
		if(this.number == c.number)
			return this.name.compareToIgnoreCase(c.name);
		else
			return Double.compare(this.number, c.number);
	}

	@Override
	public String toString() {
		return new StringBuilder().append("Chapter [name=").append(name).append(", number=").append(number).append(", isRead=")
				.append(isRead).append(", isDeleted=").append(isDeleted).append(", extensionLessName=")
				.append(extensionLessName).append(", chapterFileExits=").append(chapterFileExits == 1)
				.append(", chapterFilePath=").append(chapterFilePath).append("]").toString();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Chapter))
			return false;

		return equals((Chapter)obj);
	}

	public boolean equals(Chapter chapter) {
		if(chapter == null)
			return false;

		return this.number == chapter.number && this.name.equals(chapter.name);
	}

	private static final Pattern PATTERN_TO_EXTRACT_DOUBLE_FROM_CHAPTER_NAME = Pattern.compile("(\\d+(?:\\.\\d+)?)");
	/**
	 * extract chapter_number from given chapter_name<br>
	 * returns double value (in String) extracted from chapterName else null if not found    
	 * @param chapterName
	 * @return
	 */
	public static String extractChapterNumber(String chapterName){
		Matcher m = PATTERN_TO_EXTRACT_DOUBLE_FROM_CHAPTER_NAME.matcher(chapterName);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * 
	 * @param mangaFolderPath directory of the manga
	 * @param chapters marked read in oldChapters will be marked read in newChapters (if newChapters contains that Chapter) 
	 * @param isInIncreasingOrder  if true method will return newChapters in increasing order or else decreasing order 
	 * @return newChapters, on case of mangaFolderPath doesn't exists or is empty, this method return a Chapter[0] 
	 */
	public static Chapter[] listChaptersOrderedNaturally(File mangaFolderPath, Chapter[] oldChapters, boolean sortIncreasingly) {
		if(!mangaFolderPath.exists())
			return new Chapter[0];

		String[] names = mangaFolderPath.list();

		if(names.length == 0)
			return new Chapter[0];

		//one shot filters
		if(names.length == 1)
			return new Chapter[]{new Chapter(names[0], oldChapters == null || oldChapters.length == 0 ? false : oldChapters[0].isRead())};

		Chapter[] chapters = Stream.of(names).map(Chapter::new).toArray(Chapter[]::new);

		Arrays.sort(chapters);

		if(oldChapters != null && oldChapters.length != 0){
			List<String> old = Stream.of(oldChapters).filter(Chapter::isRead).map(Chapter::getFileName).collect(Collectors.toList());

			for (Chapter c : chapters) {
				if(old.contains(c.getFileName()))
					c.setRead(true);
			}
		}

		if(!sortIncreasingly)
			reverse(chapters);

		return chapters;
	}

	public static byte[] chaptersToBytes(Chapter[] chapters) throws IOException{
		byte[] bytes = null;
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(baos)) {

			if(chapters == null)
				chapters = new Chapter[0];

			out.writeInt(chapters.length);

			for (Chapter c : chapters)
				c.writeChapter(out);

			bytes = baos.toByteArray();
		}

		return bytes;
	}

	public static Chapter[]  bytesToChapters(byte[] bytes) throws IOException{
		Chapter[] chapters = null;

		try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				DataInputStream in = new DataInputStream(bais)) {

			chapters = new Chapter[in.readInt()];

			for (int i = 0; i < chapters.length; i++) chapters[i] = new Chapter(in);
		}

		return chapters;
	}

	public static void reverse(Chapter[] chapters){
		if(chapters.length < 2)
			return;

		for (int i = 0; i < chapters.length/2; i++) {
			Chapter temp = chapters[i];
			chapters[i] = chapters[chapters.length - i - 1];
			chapters[chapters.length - i - 1] = temp;
		}
	}

	/**
	 * if successful, it signal Manga To rearrange The chapters(if manga is not in batch modify mode)
	 * @return
	 */
	public synchronized boolean delete() {
		Path src = getGetChapterFilePath();
		try {
			FileTime time = Files.getLastModifiedTime(mangaFolder);
			if(Files.notExists(src) || Files.deleteIfExists(src)){
				Files.setLastModifiedTime(mangaFolder, time);
				isDeleted = true;
				watcher.accept(DELETED);
			}
		} catch (IOException e) {
			Utils.openErrorDialoag(null, "failed to delete: "+src,Chapter.class,280/*{LINE_NUMBER}*/, e);
		}

		return isDeleted;
	}

	/**
	 * 
	 * @param newName
	 * @return null if renaming successes else fail reason
	 */
	public synchronized String rename(String newName) {
		newName = Utils.removeInvalidCharsFromFileName(newName);

		if(newName == null || newName.isEmpty())
			return "Failed: newName Cannot be null/empty";

		Path src = getGetChapterFilePath();

		if(Files.notExists(src))
			return "Failed: File does not exists";

		if(getName().equals(newName))
			return null;

		if(!newName.endsWith(".jpeg"))
			newName = newName.concat(".jpeg");

		Path target = mangaFolder.resolve(newName);

		if(Files.exists(target))
			return "Failed, Duplicate Name Error";

		String nameBackup  = this.name;

		try {
			FileTime fileTime = Files.getLastModifiedTime(mangaFolder);
			Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
			setName(newName);
			nameBackup = null;
			Files.setLastModifiedTime(mangaFolder, fileTime);
			watcher.accept(RENAMED);
			return null;
		} catch (IOException|NullPointerException e) {
			if(nameBackup != null)
				setName(nameBackup);
			else {
				Utils.openErrorDialoag(null, String.format("Failed: Files.setLastModifiedTime(mangaFolder = %s, fileTime);", mangaFolder),Chapter.class,325/*{LINE_NUMBER}*/, e);
				return null;
			}
			return String.valueOf(e);
		}
	}
	private boolean inDeleteQueue = false;
	public void setInDeleteQueue(boolean inDeleteQueue) { this.inDeleteQueue = inDeleteQueue; }
	public boolean isInDeleteQueue() { return inDeleteQueue; }
}
