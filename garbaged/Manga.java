package samrock.manga;

import static sam.manga.samrock.mangas.MangasMeta.BU_ID;
import static sam.manga.samrock.mangas.MangasMeta.CATEGORIES;
import static sam.manga.samrock.mangas.MangasMeta.CHAPTER_ORDERING;
import static sam.manga.samrock.mangas.MangasMeta.CHAP_COUNT_PC;
import static sam.manga.samrock.mangas.MangasMeta.DESCRIPTION;
import static sam.manga.samrock.mangas.MangasMeta.DIR_NAME;
import static sam.manga.samrock.mangas.MangasMeta.IS_FAVORITE;
import static sam.manga.samrock.mangas.MangasMeta.LAST_READ_TIME;
import static sam.manga.samrock.mangas.MangasMeta.LAST_UPDATE_TIME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.manga.samrock.mangas.MangasMeta.READ_COUNT;
import static sam.manga.samrock.mangas.MangasMeta.STARTUP_VIEW;
import static sam.manga.samrock.mangas.MangasMeta.TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.UNREAD_COUNT;
import static samrock.manga.chapter.ChapterStatus.DELETED;
import static samrock.manga.chapter.ChapterStatus.SET_READ;
import static samrock.manga.chapter.ChapterStatus.SET_UNREAD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import sam.collection.CollectionUtils;
import sam.config.MyConfig;
import sam.manga.samrock.chapters.ChapterUtils;
import sam.sql.querymaker.QueryMaker;
import samrock.manga.chapter.Chapter;
import samrock.manga.chapter.ChapterWatcher;
import samrock.manga.maneger.MangaManeger;
import samrock.utils.Views;

public class Manga extends MinimalListManga {
    //constants

    private final int buId;
    private final String dirName;
    private final String description;
    private final long last_update_time;
    private final String tags;
    private final String[] urls;

    private long lastReadTime;
    private Views startupView;
    /**
     * chapter_ordering = true -> increasing order <br>
     * chapter_ordering = false -> decreasing order<br> 
     */
    private boolean chapterOrdering; // = isInIncreasingOrder

    private ArrayList<Chapter> chapters;
    private final Path mangaFolder;
    private ChapterWatcher chapterWatcher;
    private boolean isModified = false;
    /**
     * Used by chapterEditorPanel, this way manga can handle batch chapter modification 
     */
    private boolean batchEditingMode = false;

    public Manga(ResultSet rs, int arrayIndex, int version, String[] urls, Chapter[] chapters) throws SQLException {
        super(rs, arrayIndex, version);

        buId = rs.getInt(BU_ID);
        dirName = rs.getString(DIR_NAME);
        description = rs.getString(DESCRIPTION);
        lastReadTime = rs.getLong(LAST_READ_TIME);
        last_update_time = rs.getLong(LAST_UPDATE_TIME); 
        startupView = Views.parse(rs.getString(STARTUP_VIEW));
        chapterOrdering = rs.getBoolean(CHAPTER_ORDERING);
        tags = rs.getString(CATEGORIES);
        Path p = Paths.get(MyConfig.MANGA_DIR, dirName);
        mangaFolder = Files.exists(p) ? p : null;
        this.urls = urls;
        this.chapters = CollectionUtils.list(chapters);

        chapterWatcher = (chapter, code) -> {
            if(batchEditingMode)
                return;

            isModified = true;
            if(code == SET_READ || code == SET_UNREAD){
                boolean b = code == SET_READ;
                readCount = readCount + (b ? +1 : -1);
                unreadCount = unreadCount + (!b ? +1 : -1);
            }
            else if(code == DELETED){
            	if(chapter.isRead())
            		readCount--;
            	else
            		unreadCount--;
            	chapCountPc--;
            }
        };
        prepareChapters();
    }
    public String getDirName() {
        return dirName;
    }
    public String getTags() {
        return tags;
    }
    public int getBuId() {
        return buId;
    }
    public boolean isModified() {
        return isModified;
    }
    public String[] getUrls() {
        return urls;
    }
    /**
     * To all Chapters 
     * c.setWatcher(chapterWatcher);
     * c.setMangaFolder(mangaFolder);
     */
    private void prepareChapters() {
    	MangaManeger manger = MangaManeger.getInstance();
    	
    	chapters.removeIf(c -> {
    		if(c.isDeleted()) {
                manger.deleteChapter(c.getId());
                return true;
    		}
    		return false;
    	});  
    	chapters.sort(Comparator.naturalOrder());
    	
        if(!isChaptersInIncreasingOrder())
        	Collections.reverse(chapters);

        for (Chapter c : chapters) {
            c.setWatcher(chapterWatcher);
            c.setMangaFolder(mangaFolder);
        }
    }

    public boolean isChaptersInIncreasingOrder(){
        return chapterOrdering;
    }

    public Stream<Chapter> chapterStream(){
        return chapters.stream();
    }
    public void reverseChaptersOrder() {
        isModified = true;
        chapterOrdering = !chapterOrdering;
        Collections.reverse(chapters);
    }

    public ArrayList<Chapter> __getChaptersRaw() {
        return chapters;
    }
    public int getChaptersCount() {
        return chapters.size();
    }
    public ListIterator<Chapter> getIterator(int index) {
        return chapters.listIterator(index);
    }
    public List<Chapter> getChapters() {
        return Collections.unmodifiableList(chapters);
    }
    public Views getStartupView() {return startupView;}

    public Path getMangaFolderPath() {return mangaFolder;}

    public long getLastReadTime() {return lastReadTime;}

    public void setLastReadTime(long lastReadTime) {isModified = true; this.lastReadTime = lastReadTime;}

    public long getLastUpdateTime() {return last_update_time;}

    void setStartupView(Views startupView) {
        isModified = true;
        if(startupView.equals(Views.CHAPTERS_LIST_VIEW) || startupView.equals(Views.DATA_VIEW))
            this.startupView = startupView;
    }

    /**
     * recounts again chapCountPc, readCount, unreadCount
     */
    public void resetCounts(){
        isModified = true;
        //{read, unread}
        int[] counts = {0,0};

        long l = chapterStream()
                .peek(c -> {
                    if(c.isRead())
                        counts[0]++;
                    else
                        counts[1]++;
                })
                .map(c -> c.getFileName().replaceFirst(" - \\d+\\.jpe?g$", ""))
                .distinct()
                .count();

        chapCountPc = (int) l;
        readCount = counts[0];
        unreadCount = counts[1];
    }

    public static final String UPDATE_SQL = QueryMaker.getInstance().update(TABLE_NAME).placeholders(
            IS_FAVORITE,
            LAST_READ_TIME,
            STARTUP_VIEW,
            CHAPTER_ORDERING,
            CHAP_COUNT_PC,
            READ_COUNT,
            UNREAD_COUNT)
            .where(w -> w.eqPlaceholder(MANGA_ID)).build();

    private static final int IS_FAVORITE_N        = 1;
    private static final int LAST_READ_TIME_N     = 2;
    private static final int STARTUP_VIEW_N       = 3;
    private static final int CHAPTER_ORDERING_N   = 4;
    private static final int CHAP_COUNT_PC_N      = 5;
    private static final int READ_COUNT_N         = 6;
    private static final int UNREAD_COUNT_N       = 7;
    private static final int MANGA_ID_N           = 8;

    /**
     * sets all corresponding values to the given PreparedStatement and adds it to batch
     * <br><b>note: doesn't execute the PreparedStatement only PreparedStatement.addBatch() is called</b>
     * <br> and sets chapters = null  
     * @param mangaUpdate
     * @return 
     * @throws SQLException
     * @throws IOException
     */
    public void unload(PreparedStatement mangaUpdate) throws SQLException {
        mangaUpdate.setBoolean(IS_FAVORITE_N, isFavorite());
        mangaUpdate.setLong(LAST_READ_TIME_N, lastReadTime);
        mangaUpdate.setInt(STARTUP_VIEW_N, startupView.index());
        mangaUpdate.setBoolean(CHAPTER_ORDERING_N, chapterOrdering);
        mangaUpdate.setInt(CHAP_COUNT_PC_N, getChapCountPc());
        mangaUpdate.setInt(READ_COUNT_N, getReadCount());
        mangaUpdate.setInt(UNREAD_COUNT_N, unreadCount);
        mangaUpdate.setInt(MANGA_ID_N, getMangaId());
    }
    public String getDescription(){
        if(description == null || description.trim().isEmpty())
            return "No Description";
        return description;
    }
    public void resetChapters() {
        if(reloadChapters()) {
            isModified = true;
            prepareChapters();
            resetCounts();    
        }
    }
    private boolean reloadChapters() {
        try {
        	Chapter[] cs = ChapterUtils.reloadChapters(mangaFolder, chapters.toArray(new Chapter[chapters.size()]), Chapter::new);
        	chapters.clear();
            chapters.addAll(Arrays.asList(cs));
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("failed to reaload chapters", e);
        }
        return false;
    }
    public void setUnmodifed() {
        isModified = false;
    }
    public void setBatchEditingMode(boolean batchEditingMode) {
        this.batchEditingMode = batchEditingMode;
    }

    /**
     * if manga is not in batchEditingMode, that finalizeMangaChanges() call is ignored  
     */
    public void finalizeMangaChanges() {
        if(!batchEditingMode)
            throw new IllegalStateException("batchEditingMode is false");

        isModified = true;

        if(mangaFolder.toFile().list().length == 0){
            readCount = 0;
            unreadCount = 0;
            chapCountPc = 0;
            if(chapters == null)
            	chapters = new ArrayList<>();
            chapters.clear();
            return;
        }

        int size = chapters.size();
        chapters.removeIf(Chapter::isDeleted);
        
        if(size != chapters.size()){
            if(chapters.isEmpty() && reloadChapters())
                prepareChapters();
        }
        resetCounts();
    }
}
