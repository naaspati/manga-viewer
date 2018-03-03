package samrock.manga.maneger;
import static samrock.manga.maneger.MangaManegerStatus.DQ_UPDATED;
import static samrock.manga.maneger.MangaManegerStatus.MOD_MODIFIED;
import static samrock.manga.maneger.MangaManegerStatus.MOD_MODIFIED_INTERNALLY;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sam.manga.newsamrock.SamrockDB;
import sam.manga.newsamrock.column.names.RecentsMeta;
import sam.manga.newsamrock.column.names.TagsMeta;
import sam.manga.newsamrock.mangas.MangasMeta;
import sam.manga.newsamrock.urls.MangaUrlsMeta;
import sam.manga.newsamrock.urls.MangaUrlsUtils.MangaUrl;
import sam.properties.myconfig.MyConfig;
import sam.sql.sqlite.querymaker.QueryMaker;
import samrock.manga.Manga;
import samrock.manga.MinimalListManga;
import samrock.manga.MinimalManga;
import samrock.manga.chapter.Chapter;
import samrock.manga.chapter.ChapterSavePoint;
import samrock.manga.recents.MinimalChapterSavePoint;
import samrock.utils.IntArray;
import samrock.utils.SortingMethod;
import samrock.utils.Utils;

public final class MangaManeger {

    private static MangaManeger instance;
    public synchronized static MangaManeger getInstance() { return instance; }
    /**
     * used by mainMethod
     * @throws SQLException
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public synchronized static void createInstance() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        instance = new MangaManeger();
    }


    private final MinimalManga[] mangas; //contains mangas sorted by manga_id
    private final IntArray mangaIds; //contains manga_id(s), this is used as mapping array_index -> manga_id  
    private MinimalChapterSavePoint[] recents;

    public final int TAG_MIN_ID;
    public final int TAG_MAX_ID;
    private final String[] tagsArray;
    /**
     * Array Indices of mangas currently showing on display
     */
    private int[] mangasOnDisplay;
    private Manga currentManga;
    private ChapterSavePoint currentSavePoint;
    private final String[] thumbFolder; //
    private final String[][] thumbFolderListed;
    private SortingMethod currentSortingMethod = null;

    private class TempTag {
        final int id;
        final String name;

        public TempTag(ResultSet rs) throws SQLException {
            id = rs.getInt(TagsMeta.ID);
            name = rs.getString(TagsMeta.NAME);
        }
    }

    private static QueryMaker qm() {
        return QueryMaker.getInstance();
    }

    private MangaManeger() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        try (SamrockDB samrock = new SamrockDB()) {

            List<TempTag> tags = new ArrayList<>();

            samrock.executeQueryIterate(qm().select(TagsMeta.ID, TagsMeta.NAME).from(TagsMeta.TABLE_NAME).where(w -> w.eq(TagsMeta.ACTIVE, 1)).build(), 
                    rs -> tags.add(new TempTag(rs)));

            int max = 0;
            int min = Integer.MAX_VALUE;
            for (TempTag t : tags) {
                max = Math.max(max, t.id);
                min = Math.min(min, t.id);
            }

            TAG_MIN_ID = min;
            TAG_MAX_ID = max;
            int tagsCount =  tags.size();

            if(tagsCount + 50 < TAG_MAX_ID - TAG_MIN_ID)
                Utils.openErrorDialoag(null, "tagsCount ("+tagsCount+") + 50 < tagMaxId ("+TAG_MAX_ID+") - tagMinId("+TAG_MIN_ID+") = true",MangaManeger.class,118/*{LINE_NUMBER}*/, null);

            tagsArray = new String[TAG_MAX_ID - TAG_MIN_ID + 1];

            for (TempTag t : tags)
                tagsArray[t.id - TAG_MIN_ID] = t.name;                

            IntStream.Builder builder = IntStream.builder();
            samrock.executeQueryIterate(qm().select(MangasMeta.MANGA_ID).from(MangasMeta.TABLE_NAME).build(), 
                    rs -> builder.accept(rs.getInt(MangasMeta.MANGA_ID)));

            mangaIds = new IntArray(builder.build().sorted().toArray(), true);

            mangas = new MinimalManga[mangaIds.length()];
            thumbFolder = new String[mangaIds.length()];
            thumbFolderListed = new String[mangaIds.length()][];

            samrock.executeQueryIterate(qm().select(MinimalManga.COLUMN_NAMES).from(MangasMeta.TABLE_NAME).build(),
                    rs -> {
                        int index = mangaIds.indexOf(rs.getInt(MangasMeta.MANGA_ID));
                        mangas[index] = new MinimalManga(rs, index); 
                    });

            String[] thumbs = new File(MyConfig.SAMROCK_THUMBS_FOLDER).list();

            for (int i = 0; i < thumbs.length; i++){
                String s = thumbs[i]; 

                int n = s.indexOf('_');
                if(n < 0)
                    n = s.indexOf('.');
                if(n < 0)
                    n = s.length();
                
                try {
                    int index = mangaIds.indexOf(Integer.parseInt(s.substring(0, n)));
                    if(index >= 0)
                        thumbFolder[index] = s;
                } catch (NumberFormatException e) {}
            }
        }
        Utils.addExitTasks(() -> {
            mangaManegerWatchers.clear();
            loadManga(SELF_INITIATED_MANGA_UNLOAD);
            processDeleteQueue();
        });
    }

    public void loadAllMinimalListMangas() {
        if(Stream.of(mangas).allMatch(m -> m instanceof MinimalListManga))
            return;

        try(SamrockDB db = new SamrockDB()) {
            db.manga().selectAll(rs -> {
                int index = mangaIds.indexOf(rs.getInt("manga_id"));
                mangas[index] = new MinimalListManga(rs, index);
            }, MinimalListManga.COLUMN_NAMES);

        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException | IOException e2) {
            Utils.openErrorDialoag(null, "Error while loadAllMinimalListMangas, App Will Close",MangaManeger.class,155/*{LINE_NUMBER}*/, e2);
            System.exit(0);
        }
    }

    public SortingMethod getCurrentSortingMethod() {
        return currentSortingMethod;
    }
    public String getTag(int tagId) {
        return tagId <= TAG_MAX_ID && tagId >= TAG_MIN_ID ? tagsArray[tagId - TAG_MIN_ID] : null;
    }

    /**
     * other possible name -> loadAllRecents
     */
    public void loadAllMinimalChapterSavePoints(){
        if (recents != null)
            return;

        try (SamrockDB db = new SamrockDB()) {
            recents = new MinimalChapterSavePoint[mangas.length];
            db.executeQueryIterate(qm().select(MinimalChapterSavePoint.COLUMNS_NAMES).from(RecentsMeta.TABLE_NAME).build(), 
                    rs -> {
                        int id = rs.getInt("manga_id");
                        int index = mangaIds.indexOf(id);

                        if(index >= 0)
                            recents[index] = new MinimalChapterSavePoint(rs, index);
                        else
                            db.executeUpdate(qm().deleteFrom(RecentsMeta.TABLE_NAME).where(w -> w.eq(RecentsMeta.MANGA_ID, id)).build());
                    }); 
            db.commit();
        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException | IOException e) {
            Utils.openErrorDialoag(null, "Error while loading all recents(), App Will Close",MangaManeger.class,216/*{LINE_NUMBER}*/, e);
            System.exit(0);
        }
    }

    public Manga getCurrentManga() {
        return currentManga;
    }
    public ChapterSavePoint getCurrentSavePoint() {
        return currentSavePoint;
    }
    public String parseTags(String tags) {
        Pattern p = Pattern.compile("\\.(\\d+)\\.");
        Matcher m = p.matcher(tags);

        String f = "<span bgcolor=red>$1</span>";

        StringBuffer b = new StringBuffer();

        while(m.find()){
            int index =Integer.parseInt(m.group(1));

            String tag = getTag(index);

            m.appendReplacement(b, tag == null ? f : Matcher.quoteReplacement(tag+", "));
        }

        return b.toString();

    }
    
    ArrayList<MangaMangerWatcher> mangaManegerWatchers = new ArrayList<>();
    /**
     * 
     * @param mangasOnDisplay
     */
    public void setMangasOnDisplay(int[] mangasOnDisplay) {
        if(Arrays.equals(this.mangasOnDisplay, mangasOnDisplay))
            return;

        this.mangasOnDisplay = mangasOnDisplay;
        notifyWatchers(MOD_MODIFIED);
    }

    /**
     * @param mangaManegerWatcher will notify the subscriber any changes in mangamaneger
     */
    public void addMangaManegerWatcher(MangaMangerWatcher mangaManegerWatcher) {
        mangaManegerWatchers.add(mangaManegerWatcher);
    }

    /**
     * @param mangaManegerWatcher will notify the subscriber any changes in mangamaneger
     */
    public void removeMangaManegerWatcher(MangaMangerWatcher mangaManegerWatcher) {
        mangaManegerWatchers.remove(mangaManegerWatcher);
    }

    int randomNumber = 0;
    public String getRandomThumbPath(int arrayIndex) {
        if(thumbFolder[arrayIndex] == null)
            return null;

        if(thumbFolderListed[arrayIndex] == null){
            File file = new File(MyConfig.SAMROCK_THUMBS_FOLDER, thumbFolder[arrayIndex]);

            if(!file.exists()){
                thumbFolder[arrayIndex] = null;
                return null;
            }

            if(file.isFile()){
                thumbFolderListed[arrayIndex] = new String[]{thumbFolder[arrayIndex]};
                return thumbFolder[arrayIndex];
            }
            else{
                thumbFolderListed[arrayIndex] = file.list();
                if(thumbFolderListed[arrayIndex].length == 0){
                    file.delete();
                    thumbFolder[arrayIndex] = null;
                    thumbFolderListed[arrayIndex] = null;
                    return null;
                }
                else{
                    String str = thumbFolder[arrayIndex].concat("/");
                    for (int i = 0; i < thumbFolderListed[arrayIndex].length; i++) 
                        thumbFolderListed[arrayIndex][i] = str.concat(thumbFolderListed[arrayIndex][i]);
                }
            }
        }

        else if(thumbFolderListed[arrayIndex].length == 0)
            return thumbFolder[arrayIndex];
        else if(thumbFolderListed[arrayIndex].length == 1)
            return thumbFolderListed[arrayIndex][0]; 

        return thumbFolderListed[arrayIndex][(randomNumber++)%thumbFolderListed[arrayIndex].length];
    }

    public String[] getThumbsPaths(int arrayIndex){
        getRandomThumbPath(arrayIndex);
        if(thumbFolderListed[arrayIndex] == null || thumbFolderListed[arrayIndex].length == 0)
            return null;

        return thumbFolderListed[arrayIndex];
    } 

    /**
     * this method will reCheck icons for current manga
     */
    public void reListIcons() {
        File file = new File(MyConfig.SAMROCK_THUMBS_FOLDER, thumbFolder[currentManga.getIndex()]);

        if(!file.exists()){
            file = new File(MyConfig.SAMROCK_THUMBS_FOLDER, String.valueOf(currentManga.getMangaId()));
            if(file.exists())
                thumbFolder[currentManga.getIndex()]  = file.getName();
            else if((file = new File(MyConfig.SAMROCK_THUMBS_FOLDER, String.valueOf(currentManga.getMangaId())+".jpg" )).exists())
                thumbFolder[currentManga.getIndex()]  = file.getName();
            else if((file = new File(MyConfig.SAMROCK_THUMBS_FOLDER, String.valueOf(currentManga.getMangaId())+"_0.jpg" )).exists())
                thumbFolder[currentManga.getIndex()]  = file.getName();
        }

        thumbFolderListed[currentManga.getIndex()] = null;
    }

    /**
     * 
     * @return mangas.length
     */
    public int getMangasCount() {
        return mangas.length;
    }

    /**
     * 
     * @param arrayIndex 
     * @return MinimalManga or MinimalListManga stored in this index in mangas array
     */
    public MinimalManga getManga(int arrayIndex) {
        return mangas[arrayIndex];
    }


    private static final int SELF_INITIATED_MANGA_UNLOAD = 0x705*-1;
    private static final int LOAD_MOST_RECENT_MANGA = 0x706*-1;

    /**
     * load corresponding manga, ChapterSavePoint and set to currentManga and currentSavePoint  
     * @param arrayIndex
     */
    public void loadManga(int arrayIndex) {

        if(arrayIndex >= 0 && currentManga != null && currentManga.getIndex() == arrayIndex){

            return;
        }

        if(arrayIndex == LOAD_MOST_RECENT_MANGA && currentManga != null && currentManga.getLastReadTime() > Utils.START_UP_TIME){

            return;
        }

        try (SamrockDB db = new SamrockDB()) {
            //TODO was here
            unloadCurrentManga(db, arrayIndex != SELF_INITIATED_MANGA_UNLOAD);

            if(arrayIndex  != SELF_INITIATED_MANGA_UNLOAD){
                int manga_id;
                if(arrayIndex == LOAD_MOST_RECENT_MANGA){
                    manga_id = db.executeQuery("SELECT "+RecentsMeta.MANGA_ID+" FROM "+RecentsMeta.TABLE_NAME+" WHERE "+RecentsMeta.TIME+" = (SELECT MAX("+MangasMeta.LAST_READ_TIME+") FROM "+MangasMeta.TABLE_NAME+")", rs -> rs.getInt(RecentsMeta.MANGA_ID));
                    arrayIndex = mangaIds.indexOf(manga_id); 
                }
                else
                    manga_id = mangaIds.at(arrayIndex);

                MangaUrl url = db.url().getMangaUrl(manga_id);
                Chapter[] chapters = db.chapter().getChapters(manga_id, Chapter::new, Chapter.class);
                int aryIndex = arrayIndex;
                currentManga = db.executeQuery(qm().selectAll().from(MangasMeta.TABLE_NAME).where(w -> w.eq(MangasMeta.MANGA_ID, manga_id)).build(), rs -> new Manga(rs, aryIndex, new String[] {url.getMangafoxUrl(), url.getMangahereUrl()}, chapters));

                mangas[arrayIndex] = currentManga;

                currentSavePoint = db.executeQuery(qm().selectAll().from(RecentsMeta.TABLE_NAME).where(w -> w.eq(RecentsMeta.MANGA_ID, manga_id)).build(), rs -> {
                    if(rs.next())
                        return new ChapterSavePoint(rs, aryIndex);
                    else
                        return new ChapterSavePoint(currentManga);
                });
                if(recents != null)
                    recents[arrayIndex] = currentSavePoint;
            }
        } catch (SQLException | IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Utils.openErrorDialoag(null, "error while loading "+
                    (arrayIndex == LOAD_MOST_RECENT_MANGA ? " LOAD_MOST_RECENT_MANGA " 
                            : arrayIndex == SELF_INITIATED_MANGA_UNLOAD ? " SELF_INITIATED_MANGA_UNLOAD " : mangas[arrayIndex].toString()),MangaManeger.class,434/*{LINE_NUMBER}*/, e);
        }
    }

    public void  loadMostRecentManga(){loadManga(LOAD_MOST_RECENT_MANGA);}

    private void unloadCurrentManga(SamrockDB samrock, boolean notSelfInitiated) throws SQLException, IOException {
        if(currentManga == null)
            return;
        
        if(deleteChapters != null && !deleteChapters.isEmpty())
            samrock.chapter().deleteChapters(deleteChapters);    
        deleteChapters = null;

        if(currentManga.isModified()){
            samrock.prepareStatementBlock(Manga.UPDATE_SQL, ps -> {
                currentManga.unload(ps); 
                return ps.executeUpdate();
                });
            samrock.chapter().commitChaptersChanges(currentManga.getMangaId(), currentManga.__getChaptersRaw());

            if(favorites != null && notSelfInitiated){
                int index = 0;
                boolean found = false;
                int arrayIndex = currentManga.getIndex();
                for (; index < favorites.length; index++)  if(found = favorites[index] == arrayIndex) break;

                if(currentManga.isFavorite()){
                    if(!found){
                        favorites = Arrays.copyOf(favorites, favorites.length + 1);
                        index = favorites.length - 1;
                    }

                    for (; index > 0; index--) 
                        favorites[index] = favorites[index - 1];

                    favorites[0] = arrayIndex;
                }
                else if(found){
                    for (; index < favorites.length - 1; index++) 
                        favorites[index] = favorites[index + 1];

                    favorites = Arrays.copyOf(favorites, favorites.length - 1);
                }
            }
        }

        if(currentSavePoint != null && currentSavePoint.isModified()){
            int count = samrock.executeQuery(qm().select("COUNT("+RecentsMeta.MANGA_ID+") AS counts").from(RecentsMeta.TABLE_NAME).where(w -> w.eq(RecentsMeta.MANGA_ID, currentSavePoint.mangaId)).build(), rs ->  rs.getInt("counts"));

            samrock.prepareStatementBlock(count == 0 ? ChapterSavePoint.UPDATE_SQL_NEW : ChapterSavePoint.UPDATE_SQL_OLD, ps -> {
                currentSavePoint.unload(ps);
                return ps.executeUpdate();
            });

            if(recents != null && notSelfInitiated)
                recents[currentSavePoint.ARRAY_INDEX] = new MinimalChapterSavePoint(currentSavePoint);

            if(readTimeDecreasing != null && notSelfInitiated){
                int index = 0;
                int value = currentSavePoint.ARRAY_INDEX;

                for (; index < readTimeDecreasing.length; index++) {
                    if(readTimeDecreasing[index] == value)
                        break;
                }

                for (; index > 0; index--) 
                    readTimeDecreasing[index] = readTimeDecreasing[index - 1];

                readTimeDecreasing[0] = value;
            }
            currentSavePoint.setUnmodifed();
        }

        samrock.commit();

        if(notSelfInitiated){
            mangas[currentManga.getIndex()] = mangas[currentManga.getIndex() == 0 ? 1 : 0]  instanceof MinimalListManga ? new MinimalListManga(currentManga) :  new MinimalManga(currentManga);

            if(currentSortingMethod == SortingMethod.READ_TIME_DECREASING || 
                    currentSortingMethod == SortingMethod.READ_TIME_INCREASING ||
                    currentSortingMethod == SortingMethod.FAVORITES || 
                    currentSortingMethod == SortingMethod.DELETE_QUEUED 
                    ){
                changeCurrentSortingMethod(currentSortingMethod, false);
                notifyWatchers(MOD_MODIFIED_INTERNALLY);
            }
        }
    }

    private void notifyWatchers(MangaManegerStatus change){ 
        for (MangaMangerWatcher c : mangaManegerWatchers)  
            c.changed(change);
    }

    private HashSet<Integer> deleteQueuedMangas;

    public void addMangaToDeleteQueue(MinimalManga m) {
        if( deleteQueuedMangas == null)
            deleteQueuedMangas = new HashSet<>();
        if(deleteQueuedMangas.add(m.getIndex()))
            notifyWatchers(DQ_UPDATED);
    }
    
    private HashSet<Integer> deleteChapters;
    public void deleteChapter(int id) {
        if(deleteChapters == null)
            deleteChapters = new HashSet<>();
        deleteChapters.add(id);
    }

    public void removeMangaFromDeleteQueue(MinimalManga m) {
        if(deleteQueuedMangas.remove(m.getIndex()))
            notifyWatchers(DQ_UPDATED);
    }

    public boolean isMangaInDeleteQueue(MinimalManga m) {
        return deleteQueuedMangas != null && deleteQueuedMangas.contains(m.getIndex());
    }
    public boolean mangasDeleteQueueIsEmpty(){
        return deleteQueuedMangas.isEmpty();
    }

    private void processDeleteQueue() {
        if(deleteQueuedMangas == null || deleteQueuedMangas.isEmpty())
            return;

        try (SamrockDB db = new SamrockDB()) {
            String sql = qm().select(MangasMeta.MANGA_ID, MangasMeta.DIR_NAME).from(MangasMeta.TABLE_NAME).where(w -> w.in(MangasMeta.MANGA_ID, deleteQueuedMangas)).build();

            StringBuilder sb = new StringBuilder();
            sb.append("\r\nSQL").append(sql).append("\r\n\r\n");

            List<File> dirs = new ArrayList<>();
            List<File> dirs2 = dirs;
            db.executeQueryIterate(sql, rs -> {
                String name = rs.getString("dir_name");
                int id = rs.getInt("manga_id");

                sb.append("id: ").append(id)
                .append(",  name:").append(name);

                File dir = new File(MyConfig.MANGA_FOLDER, name);

                boolean bool = dir.exists();

                sb.append(",  file count: ")
                .append(bool ?  dir.list().length : " Dir does not exists")
                .append(System.lineSeparator());

                if(bool)
                    dirs2.add(dir);
            });

            JTextArea t = new JTextArea(sb.toString(), 20, 40);
            t.setFont(new Font(null, 1, 20));
            int option = JOptionPane.showConfirmDialog(null, new JScrollPane(t), "R U Sure?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

            if(option == JOptionPane.OK_OPTION){
                dirs = dirs.stream()
                        .peek(f -> Stream.of(f.listFiles()).forEach(File::delete))
                        .filter(f -> !f.delete())
                        .collect(Collectors.toList());

                if(!dirs.isEmpty()){
                    sb.append("\r\n\r\nFiles needs Attention \r\n");
                    for (File f : dirs) sb.append(f).append(System.lineSeparator());
                }

                String format = qm().deleteFrom("%s").where(w -> w.in(MangasMeta.MANGA_ID, deleteQueuedMangas)).build();

                db.executeUpdate(String.format(format, MangasMeta.TABLE_NAME));
                db.executeUpdate(String.format(format, MangaUrlsMeta.TABLE_NAME));
                db.executeUpdate(String.format(format, RecentsMeta.TABLE_NAME));

                db.commit();
            }
            else 
                Utils.showHidePopup("delete cancelled", 1500);

        }
        catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException | IOException e) {
            Utils.openErrorDialoag(null, "error while deleting from database ids\r\n"+deleteQueuedMangas,MangaManeger.class,631/*{LINE_NUMBER}*/, e);
            return;
        }
    }
    public MinimalChapterSavePoint getChapterSavePoint(int array_index) {
        return recents[array_index];
    }
    /**
     * 
     * @return a <b>copy</b> of mangasOnDisplay
     */
    public int[] getMangasOnDisplay() {
        return Arrays.copyOf(mangasOnDisplay, mangasOnDisplay.length);
    }

    /**
     * @return mangasOnDisplay.length
     */
    public int getMangasOnDisplayCount() {
        return mangasOnDisplay.length;
    }

    /**
     * 
     * @param sortingMethod by which mangas are sorted
     * @param sortCurrentMangasOnDisplay if true and mangasOnDisplay is not null then current mangasOnDisplay is sorted, otherwise  mangasOnDisplay is set with new full mangas sorted array
     */
    public void changeCurrentSortingMethod(SortingMethod sortingMethod, boolean sortCurrentMangasOnDisplay) {
        if(sortingMethod == null)
            Utils.openErrorDialoag(null, "sortingMethod = null, changeCurrentSortingMethod()",MangaManeger.class,662/*{LINE_NUMBER}*/, null);

        if(mangasOnDisplay == null)
            sortCurrentMangasOnDisplay = false;



        this.currentSortingMethod = sortingMethod;

        mangasOnDisplay = sortArray(sortCurrentMangasOnDisplay ? mangasOnDisplay : null);

        notifyWatchers(MOD_MODIFIED);


    }
    /**
     * arrayToBeSorted is sorted with currentSortingMethod 
     * 
     * @param  
     * @return a new sorted array if arrayToBeSorted = null, otherwise arrayToBeSorted is sorted and returned 
     */
    public int[] sortArray(int[] arrayToBeSorted){
        if(arrayToBeSorted != null && arrayToBeSorted.length < 2)
            return arrayToBeSorted;

        if(arrayToBeSorted == null)
            return getSortedFullArray(currentSortingMethod, true);
        else{ 
            sortArray(getSortedFullArray(currentSortingMethod, false), arrayToBeSorted);
            return arrayToBeSorted;
        }
    }

    private void sortArray(int[] sortedArray, int[] arrayToBeSorted){
        if(arrayToBeSorted.length < 2)
            return;

        if(arrayToBeSorted.length == sortedArray.length)
            for (int i = 0; i < sortedArray.length; i++) arrayToBeSorted[i] = sortedArray[i];
        else{
            Arrays.sort(arrayToBeSorted);
            int[] array2 = new int[arrayToBeSorted.length];

            for (int i = 0, j = 0; i < array2.length; j++) {
                if(Arrays.binarySearch(arrayToBeSorted, sortedArray[j]) >= 0)
                    array2[i++] = sortedArray[j];
            }
            for (int i = 0; i < array2.length; i++) arrayToBeSorted[i] = array2[i];
        }
    }

    /*
     * Sorted Arrays (these are fixed, because i don't think they will change during one session)
     */ 
    private int[] alphabeticallyIncreasing;
    private int[] updateTimeDecreasing;
    private int[] ranksIncreasing;
    //can change arrays
    private int[] readTimeDecreasing;
    private int[] favorites;

    /**
     * 
     * @param sortingMethod
     * @param returnAcopy if true method will return a copy of sorted array, else original array, if there is a need for reverse, it will return a reversed copy, disregarding value return_copy      
     * @return
     */
    private int[] getSortedFullArray(SortingMethod sortingMethod, boolean returnAcopy){
        fillSortedArray(sortingMethod);

        switch (sortingMethod) {
        case ALPHABETICALLY_DECREASING:
            return arrayReversedCopy(alphabeticallyIncreasing);
        case ALPHABETICALLY_INCREASING:
            return returnAcopy ? arrayCopy(alphabeticallyIncreasing) : alphabeticallyIncreasing;
        case RANKS_DECREASING:
            return arrayReversedCopy(ranksIncreasing);
        case RANKS_INCREASING:
            return returnAcopy ? arrayCopy(ranksIncreasing) : ranksIncreasing;
        case READ_TIME_DECREASING:
            return returnAcopy ? arrayCopy(readTimeDecreasing) : readTimeDecreasing;
        case READ_TIME_INCREASING:
            return arrayReversedCopy(readTimeDecreasing);
        case UPDATE_TIME_DECREASING:
            return returnAcopy ? arrayCopy(updateTimeDecreasing) : updateTimeDecreasing;
        case UPDATE_TIME_INCREASING:
            return arrayReversedCopy(updateTimeDecreasing);
        case DELETE_QUEUED:
            return deleteQueuedMangas.stream().mapToInt(Integer::intValue).toArray();
        case FAVORITES:
            return returnAcopy ? arrayCopy(favorites) : favorites;
        default:
            return new int[0];
        }
    }

    private int[] arrayReversedCopy(int[] array) {
        int[] array2 = arrayCopy(array);
        reverse(array2);
        return array2;
    }

    private int[] arrayCopy(int[] array) {
        return Arrays.copyOf(array, array.length);
    }

    private void fillSortedArray(SortingMethod sortingMethod) {
        if(sortingMethod == SortingMethod.DELETE_QUEUED)
            return;

        String select = "SELECT "+MangasMeta.MANGA_ID + " FROM "+MangasMeta.TABLE_NAME;

        if(alphabeticallyIncreasing == null && (sortingMethod == SortingMethod.ALPHABETICALLY_INCREASING || sortingMethod == SortingMethod.ALPHABETICALLY_DECREASING))
            alphabeticallyIncreasing = extractSortedArrayFromDatabase(select+" ORDER BY "+MangasMeta.MANGA_NAME);
        else if(updateTimeDecreasing == null && (sortingMethod == SortingMethod.UPDATE_TIME_INCREASING || sortingMethod == SortingMethod.UPDATE_TIME_DECREASING))
            updateTimeDecreasing = extractSortedArrayFromDatabase(select+" ORDER BY "+MangasMeta.LAST_UPDATE_TIME+" DESC");
        else if(readTimeDecreasing == null && (sortingMethod == SortingMethod.READ_TIME_INCREASING || sortingMethod == SortingMethod.READ_TIME_DECREASING))
            readTimeDecreasing = extractSortedArrayFromDatabase(select+" ORDER BY "+MangasMeta.LAST_READ_TIME+" DESC");
        else if(ranksIncreasing == null && (sortingMethod == SortingMethod.RANKS_INCREASING || sortingMethod == SortingMethod.RANKS_DECREASING))
            ranksIncreasing = extractSortedArrayFromDatabase(select+" ORDER BY "+MangasMeta.RANK);
        else if(favorites == null && sortingMethod == SortingMethod.FAVORITES)
            favorites = extractSortedArrayFromDatabase(select+" WHERE "+MangasMeta.IS_FAVORITE+" = 1 ORDER BY "+MangasMeta.LAST_UPDATE_TIME+" DESC");
    }

    private int[] extractSortedArrayFromDatabase(String sql) {
        try(SamrockDB db = new SamrockDB()) {
            return db.executeQuery(sql, rs -> {
                int[] array2 = new int[mangas.length];
                int index = 0;

                while(rs.next())
                    array2[index++] = rs.getInt("manga_id");

                if(array2.length != index)
                    return Arrays.copyOf(array2, index);

                mangaIdsToArrayIndices(array2);
                return array2;
            });
        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException | IOException e2) {
            Utils.openErrorDialoag("failed to sql", e2);
            System.exit(0);
        }
        return null;
    }

    private void reverse(int[] array) {
        if(array.length < 2)
            return;
        for (int i = 0; i < array.length/2; i++) {
            int temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    /**
     * manga_id -> corresponding array_index    
     * @param array
     */
    private void mangaIdsToArrayIndices(int[] array) {
        for (int i = 0; i < array.length; i++) array[i] = mangaIds.indexOf(array[i]);
    }
    public IntArray getMangaIds() {
        return mangaIds;
    }
}
