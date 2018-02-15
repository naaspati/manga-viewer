package samrock.manga.chapter;

import static samrock.manga.chapter.ChapterStatus.DELETED;
import static samrock.manga.chapter.ChapterStatus.RENAMED;
import static samrock.manga.chapter.ChapterStatus.SET_READ;
import static samrock.manga.chapter.ChapterStatus.SET_UNREAD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.sql.ResultSet;
import java.sql.SQLException;

import samrock.utils.Utils;

/**
 * original implementation of chapter
 * @author Sameer
 *
 */
public class Chapter extends sam.manga.newsamrock.chapters.Chapter {
    private ChapterWatcher watcher;
    private Path mangaFolder;

    public Chapter(double chapterNumber, String chapterName, boolean isRead) {
        super(chapterNumber, chapterName, isRead);
    }
    public Chapter(double chapterNumber, String chapterName) {
        super(chapterNumber, chapterName);
    }
    public Chapter(ResultSet rs) throws SQLException {
        super(rs);
    }
    public void setWatcher(ChapterWatcher chapterWatcher) {
        this.watcher = chapterWatcher;
    }
    public void setMangaFolder(Path mangaFolder) {this.mangaFolder = mangaFolder;}

    private String extensionLessName;

    /**
     * return strip filename without extension
     * @return
     */
    public String getName() {
        if(extensionLessName == null)
            extensionLessName = getFileName().replaceFirst("\\.jpe?g$", "").trim();

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
            chapterFilePath = mangaFolder.resolve(getFileName());

        return chapterFilePath; 
    }

    private void setName(String chapterName) throws BadChapterNameException {
        if(chapterName == null || chapterName.trim().isEmpty())
            throw new BadChapterNameException("chapterName: '"+chapterName+"'");

        Double number =  sam.manga.newsamrock.chapters.Chapter.parseChapterNumber(chapterName);
        if(number == null)
            throw new BadChapterNameException("number not found in chapterName: '"+chapterName+"'");
        
        super.setFileName(chapterName);
        super.setNumber(number);
        chapterFileExits = -1;
        extensionLessName =  null;
        chapterFilePath = null;
    }
    @Override
    public void setRead(boolean setRead) {
        if(isRead() == setRead)
            return;

        setRead(setRead);
        
        if(watcher != null)
            watcher.changed(isRead() ? SET_READ : SET_UNREAD);
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
                setDeleted(true);
                watcher.changed(DELETED);
            }
        } catch (IOException e) {
            Utils.openErrorDialoag(null, "failed to delete: "+src,Chapter.class,280/*{LINE_NUMBER}*/, e);
        }
        return isDeleted();
    }

    /**
     * 
     * @param newName
     * @return null if renaming successes else fail reason
     * @throws BadChapterNameException 
     */
    public synchronized boolean rename(String newName) throws BadChapterNameException {
        newName = Utils.removeInvalidCharsFromFileName(newName);

        if(newName == null || newName.isEmpty())
            throw new BadChapterNameException("Failed: newName Cannot be null/empty");

        Path src = getGetChapterFilePath();

        if(Files.notExists(src))
            throw new BadChapterNameException("Failed: File does not exists");

        if(getName().equals(newName))
            return true;

        if(!newName.endsWith(".jpeg"))
            newName = newName.concat(".jpeg");

        Path target = mangaFolder.resolve(newName);

        if(Files.exists(target))
            throw new BadChapterNameException("Failed, Duplicate Name Error");

        String nameBackup  = this.getFileName();

        try {
            FileTime fileTime = Files.getLastModifiedTime(mangaFolder);
            Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
            setName(newName);
            nameBackup = null;
            Files.setLastModifiedTime(mangaFolder, fileTime);
            watcher.changed(RENAMED);
            return true;
        } catch (IOException|NullPointerException e) {
            if(nameBackup != null)
                setName(nameBackup);
            else {
                Utils.openErrorDialoag(null, String.format("Failed: Files.setLastModifiedTime(mangaFolder = %s, fileTime);", mangaFolder),Chapter.class,325/*{LINE_NUMBER}*/, e);
                return true;
            }
            throw new BadChapterNameException(e.toString(), e);
        }
    }
    private boolean inDeleteQueue = false;
    public void setInDeleteQueue(boolean inDeleteQueue) { this.inDeleteQueue = inDeleteQueue; }
    public boolean isInDeleteQueue() { return inDeleteQueue; }
}

