package samrock.manga.maneger;

import static sam.manga.samrock.meta.TagsMeta.ID;
import static sam.manga.samrock.meta.TagsMeta.NAME;
import static sam.manga.samrock.meta.TagsMeta.TAGS_TABLE_NAME;
import static samrock.utils.Utils.APP_DATA;
import static samrock.utils.Utils.subpath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.io.serilizers.StringReader2;
import sam.io.serilizers.StringWriter2;
import sam.logging.MyLoggerFactory;
import sam.manga.samrock.mangas.MangaUtils;
import sam.myutils.Checker;
import sam.reference.WeakAndLazy;
import sam.string.StringUtils;
import samrock.utils.Utils;

public class TagsDAO {
	private static final Logger LOGGER = MyLoggerFactory.logger(TagsDAO.class);

	private static final String c_name = TagsDAO.class.getName();
	private static final Path cache_path_string = APP_DATA.resolve(c_name.concat(".string"));
	private static final Path cache_path_map = APP_DATA.resolve(c_name.concat(".map"));

	private static class Tags {
		private final String[] array;
		private final Map<Integer, String> map;
		private final int max;

		public Tags(String[] array) {
			this.array = array;
			this.max = array.length - 1;
			for (int i = 0; i < array.length; i++) {
				if(Checker.isEmptyTrimmed(array[i]))
					array[i] = null;
			}
			this.map = null;
		}

		public Tags(Map<Integer, String> map, boolean save) throws IOException {
			this.map = map;
			this.array = null;
			this.max = map.keySet().stream().mapToInt(e -> e).max().orElse(0);

			if(save) {
				if(max < 51) {
					StringBuilder sb = new StringBuilder();
					String[] ar = new String[max+1];
					map.forEach((s,t) -> ar[s] = t);
					for (int i = 0; i < ar.length - 1; i++) {
						if(ar[i] != null)
							sb.append(ar[i]);
						sb.append('\n');
					}
					if(ar[ar.length - 1] != null)
						sb.append(ar[ar.length - 1]);

					StringWriter2.setText(cache_path_string, sb);
					LOGGER.fine(() -> "Tags saved: "+subpath(cache_path_string));
				} else {
					ObjectWriter.write(cache_path_map, map);
					LOGGER.fine(() -> "Tags loaded: "+subpath(cache_path_map));
				}
			}
		}
		public String getTag(int tagId) {
			if(map != null)
				return map.get(tagId);

			if(tagId > max && tagId < 0) {
				LOGGER.warning("tagId out of bounds [0, "+max+"]");
				return null;
			} 

			String s = array[tagId]; 
			if(s == null) 
				LOGGER.warning("no tag found for tagId: "+tagId);

			return s;
		}
		private static Tags load() throws SQLException, ClassNotFoundException, IOException {
			if(Files.exists(cache_path_string)) {
				String[] s = StringUtils.split(StringReader2.getText(cache_path_string), '\n');
				LOGGER.fine(() -> "Tags loaded: "+subpath(cache_path_string));
				return new Tags(s);
			} if(Files.exists(cache_path_map)) {
				Map<Integer, String> map = ObjectReader.read(cache_path_map);
				LOGGER.fine(() -> "Tags loaded: "+subpath(cache_path_map));
				return new Tags(map, false);
			} else {
				Map<Integer, String> map =  new HashMap<>();
				DB.iterate(DB.selectAll(TAGS_TABLE_NAME), rs -> map.put(rs.getInt(ID), rs.getString(NAME)));
				return new Tags(map, true); 
			} 
		}
	}

	public TagsDAO() throws SQLException, IOException {
		if(DB.isModified()) {
			Utils.delete(cache_path_map);
			Utils.delete(cache_path_string);
		}
	}
	private final WeakAndLazy<Tags> tags = new WeakAndLazy<>(this::load);

	private Tags load(){
		try {
			return Tags.load();
		} catch (ClassNotFoundException | SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	public String getTag(int tagId) {
		return tags.get().getTag(tagId);
	}
	public String[] parseTags(String tags) {
		if(Checker.isEmptyTrimmed(tags))
			return new String[0];
		
		Tags t = this.tags.get();
		return  MangaUtils.tagsToIntStream(tags).mapToObj(t::getTag).toArray(String[]::new) ;
	}
}
