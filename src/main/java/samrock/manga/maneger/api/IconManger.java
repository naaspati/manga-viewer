package samrock.manga.maneger.api;

import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;

import samrock.api.ViewElementType;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;

public interface IconManger {

	ImageIcon getViewIcon(String resourceName, ViewElementType type);

	/**
	 * for THUMB and RECENT_THUMB same icon is returned
	 * @param thumbPath
	 * @param type
	 * @return
	 */
	ImageIcon getViewIcon(MinimalManga manga, File file, ViewElementType type);
	ImageIcon getDataPanelImageSetIcon(List<File> thumbs, Manga manga);
	ImageIcon getNullIcon(ViewElementType elementtype);
	void removeIconCache(int manga_id);

}