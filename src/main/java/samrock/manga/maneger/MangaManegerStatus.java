package samrock.manga.maneger;

public enum MangaManegerStatus {
    /**
     * MOD = MANGAS_ON_DISPLAY
     */
    MOD_MODIFIED,
    /**
     * MOD = MANGAS_ON_DISPLAY<br>
     * When MangaManeger changes MOD by Itself<br>
     * this variable is helpful for search maneger<br>
     * e.g. when mangas are added or removed from delete queue, or favorite list,
     * it signals search maneger of change is mod backup and redo search (if needed)      
     */
    MOD_MODIFIED_INTERNALLY,
    /**
     * DQ = DELETE_QUEUE
     */
    DQ_UPDATED;

}
