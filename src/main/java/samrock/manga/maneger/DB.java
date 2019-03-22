package samrock.manga.maneger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

import sam.config.MyConfig;
import sam.io.serilizers.LongSerializer;
import sam.manga.samrock.SamrockDB;
import sam.manga.samrock.urls.nnew.MangaUrlsUtils;
import sam.manga.samrock.urls.nnew.UrlsPrefixImpl;
import sam.myutils.MyUtilsException;
import sam.sql.SqlConsumer;
import sam.sql.SqlFunction;
import samrock.Utils;

public class DB {
	private static final Path DB_PATH = Paths.get(MyConfig.SAMROCK_DB);
	private static SamrockDB db;
	private static final Logger LOGGER = Utils.getLogger(DB.class);
	private static final StringBuilder BULK_SQL = new StringBuilder();
	
	private static final boolean is_modified;
	
	private DB() { }
	
	private static final AtomicBoolean init = new AtomicBoolean(false);
	private static void init() {
		if(init.get())
			return;
		init.set(true);
		
		db = MyUtilsException.noError(SamrockDB::new);
	}
	
	static {
		if(Files.notExists(DB_PATH)) 
			throw new RuntimeException("samrock_db not found: "+DB_PATH);
		
		Path p = Utils.APP_DATA.resolve(DB.class.getName().concat(".lastmodified.long"));
		try {
			is_modified = Files.notExists(p) || new LongSerializer().read(p) != DB_PATH.toFile().lastModified();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} 
		LOGGER.fine(() -> "is_samrockDB_modified: "+is_modified);
	}
	public static boolean isModified() {
		return is_modified;
	}
	public static String selectAll(String table_name) {
		return "SELECT * FROM ".concat(table_name);
	}
	public static void iterate(String sql, SqlConsumer<ResultSet> action) throws SQLException {
		init();
		db.iterate(sql, action);
	}
	public static <E> E executeQuery(String sql, SqlFunction<ResultSet, E> action) throws SQLException {
		init();
		return db.executeQuery(sql, action);
	}
	static Map<String, UrlsPrefixImpl> mangaUrlsPrefixes() throws SQLException {
		init();
		return new MangaUrlsUtils(db).getPrefixes();
	}
	static Statement createStatement() throws SQLException {
		init();
		return db.createStatement();
	}
	public static <E> ArrayList<E> collectToList(String sql, SqlFunction<ResultSet, E> mapper) throws SQLException {
		return db.collectToList(sql, mapper);
	}
	
}
