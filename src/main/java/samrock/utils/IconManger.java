package samrock.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.properties.myconfig.MyConfig;

public final class IconManger {
    private static Logger logger = LoggerFactory.getLogger(IconManger.class);

    private static IconManger instance;
    private final ArrayList<String> existingIconCacheNames = new ArrayList<>();

    public synchronized static IconManger getInstance() {
        if(instance == null)
            instance = new IconManger();

        return instance;
    }

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
        thumbFolder = Paths.get(MyConfig.SAMROCK_THUMBS_FOLDER);
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
                logger.warn("failed to write cache config", e);
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
                logger.warn("error while reading cacheConfig", e);
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
            else
                existingIconCacheNames.addAll(Arrays.asList(cacheFolder.toFile().list()));
        } catch (IOException e) {
            logger.warn("error while cache check ups", e);

        }
    }

    /**
     * @param thumbPath String path to original Image
     * @param typeOfIcon <br>
     * 	&emsp;&emsp;&emsp;&emsp;ViewElementType.THUMB = returns icon for thumb_view;<br>
     * &emsp;&emsp;&emsp;&emsp;ViewElementType.LIST = returns icon for list_view;<br>
     * @return
     */
    public ImageIcon getViewIcon(String thumbPath, ViewElementType thumb) {

        if(thumbPath == null || thumbPath.trim().isEmpty())
            return null;

        return getViewIcon(thumbPath.startsWith("images/") ? ClassLoader.getSystemResource(thumbPath) : thumbFolder.resolve(thumbPath), thumb);
    }

    /**
     * for THUMB and RECENT_THUMB same icon is returned
     * @param thumbPath
     * @param elementType
     * @return
     */
    ImageIcon getViewIcon(Object thumbPathOrUrl, ViewElementType elementType) {

        if(elementType  == ViewElementType.RECENT_THUMB)
            elementType = ViewElementType.THUMB;

        String iconCacheName = null;
        if(thumbPathOrUrl instanceof Path) {
            iconCacheName = elementType+"_"+((Path)thumbPathOrUrl).getFileName();

            if(existingIconCacheNames.contains(iconCacheName))
                return fetchCachedIcon(iconCacheName);
        }
        int h, w;

        if(elementType == ViewElementType.THUMB){
            w = THUMB_IMAGE_WIDTH;
            h = THUMB_IMAGE_HEIGHT;
        }
        else if(elementType == ViewElementType.LIST){
            w = LIST_IMAGE_WIDTH;
            h = LIST_IMAGE_HEIGHT;
        }
        else if(elementType == ViewElementType.RECENT_LIST){
            w = RECENT_LIST_IMAGE_WIDTH;
            h = RECENT_LIST_IMAGE_HEIGHT;
        }
        else{
            logger.warn("invalid View Supplied to IconManager.getViewIcon(String thumbPath = %s, Views view = %s)\r\n", thumbPathOrUrl, elementType);
            return null;
        }
        BufferedImage img = thumbPathOrUrl instanceof URL ? Utils.getImage((URL)thumbPathOrUrl) : Utils.getImage((Path)thumbPathOrUrl);

        if(img != null && img.getHeight() > 10){
            ImageIcon icon = new ImageIcon(img.getScaledInstance(w , h, Image.SCALE_SMOOTH));
            if(iconCacheName != null) {
                writeIcon(icon, iconCacheName);
                existingIconCacheNames.add(iconCacheName);
            }
            return  icon;
        }
        return null;
    }

    public ImageIcon getDataPanelImageSetIcon(String[] pathStrings, int manga_id) {

        if(pathStrings == null || pathStrings.length == 0)
            return null;

        String iconCacheName = Views.DATA_VIEW+"_"+manga_id;

        if(existingIconCacheNames.contains(iconCacheName))
            return fetchCachedIcon(iconCacheName);

        BufferedImage img  = new BufferedImage(DATAPANEL_PER_IMAGE_WIDTH, pathStrings.length*DATAPANEL_PER_IMAGE_HEIGHT + pathStrings.length*20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        boolean error = false;
        for (int i = 0; i < pathStrings.length; i++){
            Image img2 = Utils.getImage(thumbFolder.resolve(pathStrings[i]));

            if(img != null)
                g2.drawImage(img2.getScaledInstance(DATAPANEL_PER_IMAGE_WIDTH, DATAPANEL_PER_IMAGE_HEIGHT, Image.SCALE_SMOOTH), 0, DATAPANEL_PER_IMAGE_HEIGHT*i + i*10,null);
            else{
                g2.drawString("Image not found", 10, DATAPANEL_PER_IMAGE_HEIGHT*i + DATAPANEL_PER_IMAGE_HEIGHT/2);
                error = true;
            }
        }
        g2.dispose();

        ImageIcon icon = new ImageIcon(img);

        if(!error){
            writeIcon(icon, iconCacheName);
            existingIconCacheNames.add(iconCacheName);
        }

        return icon;
    }

    private ImageIcon fetchCachedIcon(String iconCacheName) {
        ImageIcon icon = null;

        try(ObjectInputStream in = new ObjectInputStream(Files.newInputStream(cacheFolder.resolve(iconCacheName), StandardOpenOption.READ))) {
            icon = (ImageIcon) in.readObject();
        } catch (IOException|ClassNotFoundException e) {
            logger.warn("Error while fetching icon, iconPath:\t"+iconCacheName, e);
            icon = null;
        }

        return icon;
    }

    private void writeIcon(ImageIcon icon, String iconCacheName){
        try(ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(cacheFolder.resolve(iconCacheName), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.writeObject(icon);
        } catch (IOException e) {
            logger.warn("Error while writing icon cache, path: "+iconCacheName, e);
        }
    }
    public ImageIcon getNullIcon(ViewElementType elementtype){
        if(elementtype == ViewElementType.THUMB || elementtype == ViewElementType.RECENT_THUMB)
            return new ImageIcon(new BufferedImage(THUMB_IMAGE_WIDTH, THUMB_IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY));
        if(elementtype == ViewElementType.LIST)
            return new ImageIcon(new BufferedImage(LIST_IMAGE_WIDTH, LIST_IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY));
        if(elementtype == ViewElementType.RECENT_LIST)
            return new ImageIcon(new BufferedImage(RECENT_LIST_IMAGE_WIDTH, RECENT_LIST_IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY));

        logger.warn("Invalid ElementType value:" +elementtype);
        return null ;
    }
    public void removeIconCache(int manga_id) {
        String s = String.valueOf(manga_id);
        existingIconCacheNames.removeIf(cName -> cName.contains(s));
    }
}
