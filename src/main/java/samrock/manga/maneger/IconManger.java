package samrock.manga.maneger;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import sam.config.MyConfig;
import sam.logging.MyLoggerFactory;
import sam.myutils.Checker;
import sam.reference.ReferenceUtils;
import samrock.manga.Manga;
import samrock.manga.MinimalManga;
import samrock.utils.RH;
import samrock.utils.Utils;
import samrock.utils.ViewElementType;
import samrock.utils.Views;

public final class IconManger {
    private static Logger logger = MyLoggerFactory.logger(IconManger.class);

    private static IconManger instance;

    public synchronized static IconManger getInstance() {
        if(instance == null)
            instance = new IconManger();

        return instance;
    }

    @SuppressWarnings("rawtypes")
    private final WeakReference[] cache = new WeakReference[MangaManeger.getMangasCount()]; 

    //thumb view image width and height 
    private final int THUMB_IMAGE_WIDTH;
    private final int THUMB_IMAGE_HEIGHT;

    //list view image width and height
    private  final int LIST_IMAGE_WIDTH;
    private final int LIST_IMAGE_HEIGHT;

    //list view image width and height
    private  final int RECENT_LIST_IMAGE_WIDTH;
    private final int RECENT_LIST_IMAGE_HEIGHT;

    //DataView.MangaImageSetLabel per Image width and height
    private final int DATAPANEL_PER_IMAGE_WIDTH;
    private final int DATAPANEL_PER_IMAGE_HEIGHT;

    private final Path thumbFolder;
    private final Path cacheFolder = Paths.get("cache");

    private IconManger() {
        ImageIO.setUseCache(false);

        THUMB_IMAGE_WIDTH = RH.getInt("thumbview.icon.width");
        THUMB_IMAGE_HEIGHT = RH.getInt("thumbview.icon.height");

        LIST_IMAGE_WIDTH = RH.getInt("listview.icon.width");
        LIST_IMAGE_HEIGHT = RH.getInt("listview.icon.height");

        RECENT_LIST_IMAGE_WIDTH = RH.getInt("recentview.list.icon.width");
        RECENT_LIST_IMAGE_HEIGHT = RH.getInt("recentview.list.icon.height");

        DATAPANEL_PER_IMAGE_WIDTH = RH.getInt("mangathumbsetlabel.per.thumb.width"); 
        DATAPANEL_PER_IMAGE_HEIGHT = RH.getInt("mangathumbsetlabel.per.thumb.height");

        /** config file read/write order

         * Thumb folder last modified time -> long
         * THUMB_IMAGE_WIDTH -> int
         * THUMB_IMAGE_HEIGHT -> int
         * LIST_IMAGE_WIDTH -> int
         * LIST_IMAGE_HEIGHT -> int
         * RECENT_LIST_IMAGE_WIDTH -> int
         * RECENT_LIST_IMAGE_HEIGHT -> int
         * DATAPANEL_PER_IMAGE_WIDTH -> int
         * DATAPANEL_PER_IMAGE_HEIGHT -> int
         */

        final Path cacheConfigPath  = cacheFolder.resolve("cacheConfig");
        thumbFolder = Paths.get(MyConfig.SAMROCK_THUMBS_DIR);
        long THUMB_FOLDER_TIME = thumbFolder.toFile().lastModified();

        Supplier<Boolean> writeCacheConfig = () -> {
            boolean b = false;
            try(DataOutputStream out = new DataOutputStream(Files.newOutputStream(cacheConfigPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {

                out.writeLong(THUMB_FOLDER_TIME);

                out.writeInt(THUMB_IMAGE_WIDTH);
                out.writeInt(THUMB_IMAGE_HEIGHT);
                out.writeInt(LIST_IMAGE_WIDTH);
                out.writeInt(LIST_IMAGE_HEIGHT);
                out.writeInt(RECENT_LIST_IMAGE_WIDTH);
                out.writeInt(RECENT_LIST_IMAGE_HEIGHT);
                out.writeInt(DATAPANEL_PER_IMAGE_WIDTH);
                out.writeInt(DATAPANEL_PER_IMAGE_HEIGHT);

                b = true;
            } catch (IOException e) {
                logger.log(Level.WARNING, "failed to write cache config", e);
                b = false;
            }

            return b;
        };

        Supplier<Boolean> confirmCacheConfig = () -> {
            if(Files.notExists(cacheConfigPath))
                return false;

            boolean b = false;

            try(DataInputStream in = new DataInputStream(Files.newInputStream(cacheConfigPath))) {
                b =  in.readLong() == THUMB_FOLDER_TIME &&
                        in.readInt() == THUMB_IMAGE_WIDTH &&
                        in.readInt() == THUMB_IMAGE_HEIGHT &&
                        in.readInt() == LIST_IMAGE_WIDTH &&
                        in.readInt() == LIST_IMAGE_HEIGHT &&
                        in.readInt() == RECENT_LIST_IMAGE_WIDTH &&
                        in.readInt() == RECENT_LIST_IMAGE_HEIGHT &&
                        in.readInt() == DATAPANEL_PER_IMAGE_WIDTH &&
                        in.readInt() == DATAPANEL_PER_IMAGE_HEIGHT; 
            } catch (IOException e) {
                logger.log(Level.WARNING, "error while reading cacheConfig", e);
                b = false;
            }
            return b;
        };

        //routine check-ups
        try {
            if(Files.notExists(cacheFolder)){
                Files.createDirectories(cacheFolder);
                writeCacheConfig.get();
            }
            else if(!confirmCacheConfig.get()){
                Stream.of(cacheFolder.toFile().listFiles()).forEach(File::delete);
                writeCacheConfig.get();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING,  "error while cache check ups", e);

        }
    }

    public ImageIcon getViewIcon(String resourceName, ViewElementType type) {
        try {
            return readImage(null, ClassLoader.getSystemResourceAsStream(resourceName), type, null);
        } catch (IOException e) {
            logger.warning(() -> "failed to get resource: "+resourceName+"  "+e);
        }
        return null;
    }

    /**
     * for THUMB and RECENT_THUMB same icon is returned
     * @param thumbPath
     * @param type
     * @return
     */
    public ImageIcon getViewIcon(MinimalManga manga, File file, ViewElementType type) {
        if(file == null || !file.exists())
            return null;

        if(type  == ViewElementType.RECENT_THUMB)
            type = ViewElementType.THUMB;

        String iconCacheName = type+"_"+file.getName();
        ImageIcon icon = fetchCachedIcon(manga, iconCacheName);

        if(icon != null)
            return icon;

        try {
            return readImage(manga,new FileInputStream(file), type, iconCacheName);
        } catch (IOException e) {
            logger.log(Level.WARNING, "invalid View Supplied to IconManager.getViewIcon(String thumbPath = {}, Views view = {})\r\n", new Object[]{file, type});
            return null;
        }
    }

    private ImageIcon readImage(MinimalManga manga, InputStream is, ViewElementType type, String iconCacheName) throws IOException {
        int h, w;

        if(type == ViewElementType.THUMB){
            w = THUMB_IMAGE_WIDTH;
            h = THUMB_IMAGE_HEIGHT;
        }
        else if(type == ViewElementType.LIST){
            w = LIST_IMAGE_WIDTH;
            h = LIST_IMAGE_HEIGHT;
        }
        else if(type == ViewElementType.RECENT_LIST){
            w = RECENT_LIST_IMAGE_WIDTH;
            h = RECENT_LIST_IMAGE_HEIGHT;
        } else{
            throw new IOException("invalid view type");
        }

        BufferedImage img = ImageIO.read(is);

        if(img != null && img.getHeight() > 10){
            ImageIcon icon = new ImageIcon(img.getScaledInstance(w , h, Image.SCALE_SMOOTH));
            if(iconCacheName != null)
                writeIcon(manga, icon, iconCacheName);
            return  icon;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public ImageIcon getDataPanelImageSetIcon(List<File> thumbs, Manga manga) {

        if(Checker.isEmpty(thumbs))
            return null;

        String iconCacheName = Views.DATA_VIEW+"_"+manga.getMangaId();

        ImageIcon icon = fetchCachedIcon(manga, iconCacheName);
        if(icon != null)
            return icon;

        Image img = null;

        if(thumbs.size() == 1) {
            img = Utils.getImage(thumbs.get(0));
            img = img == null ? null : img.getScaledInstance(DATAPANEL_PER_IMAGE_WIDTH, DATAPANEL_PER_IMAGE_HEIGHT, Image.SCALE_SMOOTH);
        }

        boolean error = false;

        if(img == null) {
            BufferedImage img0  = new BufferedImage(DATAPANEL_PER_IMAGE_WIDTH, thumbs.size()*DATAPANEL_PER_IMAGE_HEIGHT + thumbs.size()*20, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img0.createGraphics();

            int n = 0;
            for (File file : thumbs) {
                Image img2 = Utils.getImage(file);

                if(img2 != null)
                    g2.drawImage(img2.getScaledInstance(DATAPANEL_PER_IMAGE_WIDTH, DATAPANEL_PER_IMAGE_HEIGHT, Image.SCALE_SMOOTH), 0, DATAPANEL_PER_IMAGE_HEIGHT*n + n*10,null);
                else{
                    g2.drawString("Image not found", 10, DATAPANEL_PER_IMAGE_HEIGHT*n + DATAPANEL_PER_IMAGE_HEIGHT/2);
                    error = true;
                }
            }
            g2.dispose();
            img = img0;
        }

        icon = new ImageIcon(img);

        if(!error)
            writeIcon(manga, icon, iconCacheName);

        return icon;
    }

    private ImageIcon fetchCachedIcon(MinimalManga manga, String iconCacheName) {
        if(manga == null) return null;

        ImageIcon icon = cacheGet(manga, iconCacheName);

        if(icon != null)
            return icon;

        Path p = cacheFolder.resolve(iconCacheName);
        if(Files.notExists(p))
            return null;

        try(ObjectInputStream in = new ObjectInputStream(Files.newInputStream(p, StandardOpenOption.READ))) {
            icon = (ImageIcon) in.readObject();
            cachePut(manga, iconCacheName, icon);
            return icon;
        } catch (IOException|ClassNotFoundException e) {
            logger.log(Level.WARNING, "Error while fetching icon, iconPath: "+iconCacheName, e);
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    private Map<String, ImageIcon> getCacheMap(MinimalManga manga) {
        if(manga == null) return null;
        return (Map<String, ImageIcon>) ReferenceUtils.get(cache[index(manga)]);
    }
    private static int index(MinimalManga manga) {
		return MangaManeger.indexOf(manga);
	}

	private void cachePut(MinimalManga manga, String iconCacheName, ImageIcon icon) {
        if(manga == null) return;
        
        Map<String, ImageIcon> map = getCacheMap(manga);

        if(map == null) {
            map = new HashMap<>();
            cache[index(manga)] = new WeakReference<>(map);
        }

        map.put(iconCacheName, icon);
    }

    private ImageIcon cacheGet(MinimalManga manga, String iconCacheName) {
        if(manga == null) return null;
        
        Map<String, ImageIcon> map = getCacheMap(manga);
        if(map == null)
            return null;
        return map.get(iconCacheName);
    }

    private void writeIcon(MinimalManga manga, ImageIcon icon, String iconCacheName){
        try(ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(cacheFolder.resolve(iconCacheName), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.writeObject(icon);
            cachePut(manga, iconCacheName, icon);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while writing icon cache, path: "+iconCacheName, e);
        }
    }
    public ImageIcon getNullIcon(ViewElementType elementtype){
        if(elementtype == ViewElementType.THUMB || elementtype == ViewElementType.RECENT_THUMB)
            return new ImageIcon(new BufferedImage(THUMB_IMAGE_WIDTH, THUMB_IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY));
        if(elementtype == ViewElementType.LIST)
            return new ImageIcon(new BufferedImage(LIST_IMAGE_WIDTH, LIST_IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY));
        if(elementtype == ViewElementType.RECENT_LIST)
            return new ImageIcon(new BufferedImage(RECENT_LIST_IMAGE_WIDTH, RECENT_LIST_IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY));

        logger.warning(() -> "Invalid ElementType value:" +elementtype);
        return null ;
    }
    @SuppressWarnings("unchecked")
    public void removeIconCache(int manga_id) {
        String s = String.valueOf(manga_id);

        Arrays.stream(cache)
        .map(ReferenceUtils::get)
        .filter(Objects::nonNull)
        .forEach(m -> {
            ((Map<String, ImageIcon>)m).keySet()
            .removeIf(cName -> {
                if(cName.contains(s)) {
                    cacheFolder.resolve(cName).toFile().delete();
                    return true;
                }
                return false;
            });
        });


    }
}
