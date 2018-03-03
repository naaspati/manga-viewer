package samrock.gui;

public enum Change {
    BACK_TO_DOCK,
    CHANGEVIEW_DATA_VIEW,
    CHANGEVIEW_CHAPTERS_LIST_VIEW,
    CHANGETYPE_LIST,
    CHANGETYPE_THUMB,
    OPEN_MOST_RECENT_CHAPTER,
    OPEN_MOST_RECENT_MANGA,
    ICONFY_APP,
    CLOSE_APP,
    /**
     * if current type is LIST change to RECENT_LIST, if THUMB change to RECENT_THUMB
     */
    CHANGETYPE_RECENT,
    /**
     * opposite action of {@link #CHANGETYPE_RECENT}
     */
    CHANGETYPE_NORMAL,
    
    VIEW_ELEMENT_CLICKED,
    
    START_CHAPTER_EDITOR,
    START_MANGA_VIEWER,
    
    
    // MangaViewer
    STARTED,
    CLOSED;
}
