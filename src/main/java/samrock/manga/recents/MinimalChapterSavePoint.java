package samrock.manga.recents;

import static sam.manga.samrock.meta.RecentsMeta.CHAPTER_ID;
import static sam.manga.samrock.meta.RecentsMeta.CHAPTER_NAME;
import static sam.manga.samrock.meta.RecentsMeta.MANGA_ID;
import static sam.manga.samrock.meta.RecentsMeta.TIME;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.IntUnaryOperator;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import samrock.manga.MinimalManga;
import samrock.utils.MangaIdIndexContainer;

public class MinimalChapterSavePoint implements MangaIdIndexContainer {
	public static final String[] COLUMNS_NAMES = {MANGA_ID,CHAPTER_ID, CHAPTER_NAME,TIME};

	private final int mangaIndex;
	public final int mangaId;
	protected long saveTime;
	/**
	 * expected chapterId
	 */
	protected int chapterId;
	protected String chapterFileName;
	
	private MinimalChapterSavePoint(int mangaId, int mangaIndex) {
		this.mangaId = mangaId;
		this.mangaIndex = mangaIndex;
	}
	public int getMangaIndex() {
		return mangaIndex;
	}
	public MinimalChapterSavePoint(ResultSet rs, int mangaIndex) throws SQLException {
		this.mangaId = rs.getInt(MANGA_ID);
		this.saveTime = rs.getLong(TIME);
		this.chapterFileName = rs.getString(CHAPTER_NAME);
		this.chapterId = rs.getInt(CHAPTER_ID);
		this.mangaIndex = mangaIndex;
	}
	@SuppressWarnings("deprecation")
	public MinimalChapterSavePoint(MinimalManga manga, Chapter chapter, long saveTime) {
		this.mangaId = manga.getMangaId();
		this.saveTime = saveTime;
		this.mangaIndex = manga.getMangaIndex();
		setChapter(chapter);
	}
	public final String getChapterFileName() { return chapterFileName; }
	public final int getMangaId() { return mangaId; }
	public final int getChapterId() { return chapterId; }

	public long getSaveTime() { return saveTime; }
	public void setSaveTime(long saveTime) { this.saveTime = saveTime; }

	public void setChapter(Chapter chapter) { 
		this.chapterFileName = chapter.getFileName();
		this.chapterId = chapter.getId();
	}

	public static class MinimalChapterSavePointSerilizer implements Serializer<MinimalChapterSavePoint>{
		private final IntUnaryOperator indexGetter;
		
		public MinimalChapterSavePointSerilizer(IntUnaryOperator indexGetter) {
			this.indexGetter = indexGetter;
		}
		
		@Override
		public void serialize(DataOutput2 out, MinimalChapterSavePoint value) throws IOException {
			out.writeInt(value.mangaId);
			out.writeInt(value.chapterId);
			out.writeLong(value.saveTime);
			out.writeUTF(value.chapterFileName);
		}
		@Override
		public MinimalChapterSavePoint deserialize(DataInput2 input, int available) throws IOException {
			int mangaId =  input.readInt();
			int index = indexGetter.applyAsInt(mangaId);
			MinimalChapterSavePoint m = new MinimalChapterSavePoint(mangaId, index);
			m.chapterId = input.readInt();
			m.saveTime = input.readLong();
			m.chapterFileName = input.readUTF();
			
			return m;
		}
	}
}
