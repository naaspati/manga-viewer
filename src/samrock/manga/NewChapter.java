package samrock.manga;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.function.IntConsumer;

import samrock.utils.Utils;

// will replace Original Chapter class
public class NewChapter extends sam.manga.samrock.Chapter {
    public static final int SET_READ = 0x600;
    public static final int SET_UNREAD = 0x601;
    public static final int RENAMED = 0x602;
    public static final int DELETED = 0x603;

    private IntConsumer watcher;
    private Path mangaFolder;
    private boolean isDeleted = false;

    public void setWatcher(IntConsumer chapterWatcher) {this.watcher = chapterWatcher;}

    public NewChapter(String chapterName) {
        super(chapterName);
    }
    public boolean isDeleted() {
        return isDeleted;
    }
    public void setMangaFolder(Path mangaFolder) {this.mangaFolder = mangaFolder;}

    /**
     * return fileName of the strip
     * @return
     */
    public String getFileName() {return super.getName();}

    private String extensionLessName;

    /**
     * return strip filename without extension
     * @return
     */
    @Override
    public String getName() {
        if(extensionLessName == null)
            extensionLessName = super.getName().replaceFirst("\\.jpe?g$", "").trim();

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
            chapterFilePath = mangaFolder.resolve(super.getName());

        return chapterFilePath; 
    }

    private void _setName(String chapterName) {
        if(chapterName == null || chapterName.trim().isEmpty())
            throw new NullPointerException("chapterName: '"+chapterName+"'");

        super.setName(chapterName);
        String numberString = extractChapterNumber(chapterName);
        super.setNumber(numberString == null ? -1d : Double.parseDouble(numberString));
        chapterFileExits = -1;
        extensionLessName =  null;
        chapterFilePath = null;
    }

    /**
     * 
     * @param setRead true sets chapter read, false sets chapter unread, if previous value is save as current no changes made, no notifying parent manga 
     */
    public void setRead(boolean setRead) {
        if(isRead() == setRead)
            return;

        setRead(setRead);
        
        if(watcher != null)
            watcher.accept(isRead() ? SET_READ : SET_UNREAD);
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
    @Override
    public String toString() {
        return new StringBuilder().append("Chapter [name=").append(super.getName()).append(", number=").append(getNumber()).append(", isRead=")
                .append(isRead()).append(", isDeleted=").append(isDeleted).append(", extensionLessName=")
                .append(extensionLessName).append(", chapterFileExits=").append(chapterFileExits == 1)
                .append(", chapterFilePath=").append(chapterFilePath).append("]").toString();
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

        if(super.getName().equals(newName))
            return null;

        if(!newName.endsWith(".jpeg"))
            newName = newName.concat(".jpeg");

        Path target = mangaFolder.resolve(newName);

        if(Files.exists(target))
            return "Failed, Duplicate Name Error";

        String nameBackup  = super.getName();

        try {
            FileTime fileTime = Files.getLastModifiedTime(mangaFolder);
            Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
            _setName(newName);
            nameBackup = null;
            Files.setLastModifiedTime(mangaFolder, fileTime);
            watcher.accept(RENAMED);
            return null;
        } catch (IOException|NullPointerException e) {
            if(nameBackup != null)
                _setName(nameBackup);
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
