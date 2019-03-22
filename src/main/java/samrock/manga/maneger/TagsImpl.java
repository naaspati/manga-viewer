package samrock.manga.maneger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static sam.manga.samrock.meta.TagsMeta.ID;
import static sam.manga.samrock.meta.TagsMeta.NAME;
import static sam.manga.samrock.meta.TagsMeta.TAGS_TABLE_NAME;
import static samrock.Utils.APP_DATA;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.slf4j.Logger;

import sam.collection.ArrayIterator;
import sam.functions.IOExceptionConsumer;
import sam.io.BufferConsumer;
import sam.io.BufferSupplier;
import sam.io.IOUtils;
import sam.io.serilizers.StringIOUtils;
import sam.manga.samrock.mangas.MangaUtils;
import sam.myutils.Checker;
import sam.nopkg.Resources;
import samrock.Utils;

class TagsImpl {
	private static final Logger logger = Utils.getLogger(TagsImpl.class);
	private static final Path cache_path = APP_DATA.resolve(TagsImpl.class.getName());

	private int[] ids;
	private String[] tags;

	public TagsImpl(DB db) throws SQLException, IOException {
		if(!db.isModified())
			loadFile(null);
		else if(Files.notExists(cache_path))
			loadSql(db);
		else
			loadFile(db);
	}

	private static class Temp {
		final int id;
		final String tag;

		public Temp(ResultSet rs) throws SQLException {
			this.id = rs.getInt(ID);
			this.tag = rs.getString(NAME);
		}
	}

	private static final String META_FIELD = "tags_mod";


	private void loadSql(DB db) throws SQLException, IOException {
		ArrayList<Temp> list = new ArrayList<>(100);
		db.iterate(db.selectAllQuery(TAGS_TABLE_NAME), rs -> list.add(new Temp(rs)));

		list.sort(Comparator.comparingInt(t -> t.id));
		this.ids = new int[list.size()];
		this.tags = new String[list.size()];

		for (int i = 0; i < tags.length; i++) {
			Temp t = list.get(i);
			tags[i] = t.tag;
			ids[i] = t.id;
		}

		int meta = db.appMeta(META_FIELD);

		try(FileChannel fc = FileChannel.open(cache_path, CREATE, TRUNCATE_EXISTING, WRITE);
				Resources r = Resources.get();) {
			ByteBuffer buf = r.buffer();
			buf.putInt(meta);
			buf.putInt(tags.length);

			for (int n : ids) {
				IOUtils.writeIf(buf, fc, 4);
				buf.putInt(n);
			}

			IOUtils.write(buf, fc, true);
			StringIOUtils.writeJoining(new ArrayIterator<>(tags), "\n", BufferConsumer.of(fc, false), buf, r.chars(), r.encoder());
			logger.debug("loaded from DB: {}, saved: {}", tags.length, cache_path);
		}
	}

	private void loadFile(DB db) throws IOException, SQLException {
		if(!loadFile0(db))
			loadSql(db);
	}

	private boolean loadFile0(DB db) throws SQLException, IOException {
		try(FileChannel fc = FileChannel.open(cache_path, READ);
				Resources r = Resources.get();) {
			ByteBuffer buf = r.buffer();
			int n = fc.read(buf);
			if(n < 8)
				return false;

			buf.flip();

			int meta = buf.getInt();
			int size = buf.getInt();

			int meta2;
			if(db != null && meta != (meta2 = db.appMeta(META_FIELD))) {
				logger.debug("'{}' table modified. {}, {} -> {}", TAGS_TABLE_NAME, META_FIELD, meta, meta2);
				return false;
			}

			this.ids = new int[size];
			this.tags = new String[size];

			for (int i = 0; i < ids.length; i++) {
				IOUtils.readIf(buf, fc, 4);
				ids[i] = buf.getInt();
			}

			IOExceptionConsumer<String> collector = new IOExceptionConsumer<String>() {
				int k = 0;
				@Override
				public void accept(String e) throws IOException {
					tags[k++] = e;
				}
			};

			IOUtils.compactOrClear(buf);
			StringIOUtils.collect(BufferSupplier.of(fc, buf), '\n', collector, r.decoder(), r.chars(), r.sb());
			logger.debug("loaded from cache: {}", cache_path);
			return true;
		}
	}

	public String getTag(int tagId) {
		if(tagId < ids.length && ids[tagId] == tagId)
			return tags[tagId];
		else {
			int n = Arrays.binarySearch(ids, tagId);
			logger.debug("binary search tagId: {}", tagId);
			
			if(n < 0) {
				logger.warn("no tag found for tagId: {}", tagId);
				return null;
			} else 
				return tags[n]; 
		}
	}
	public String[] parseTags(String tags) {
		if(Checker.isEmptyTrimmed(tags))
			return new String[0];

		return  MangaUtils.tagsToIntStream(tags).mapToObj(this::getTag).toArray(String[]::new) ;
	}
}
