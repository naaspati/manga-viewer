package samrock.manga.maneger;

import static sam.manga.samrock.mangas.MangasMeta.DIR_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGAS_TABLE_NAME;
import static sam.manga.samrock.mangas.MangasMeta.MANGA_ID;
import static sam.sql.querymaker.QueryMaker.qm;

import java.awt.Font;
import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sam.config.MyConfig;
import sam.manga.samrock.chapters.ChaptersMeta;
import sam.manga.samrock.meta.RecentsMeta;
import sam.manga.samrock.urls.MangaUrlsMeta;
import sam.nopkg.Junk;
import samrock.Utils;
import samrock.manga.maneger.api.DeleteQueue;

class ProcessDeleteQueue {
	public static void process(DeleteQueue deleteQueue, DB db) {
		Junk.notYetImplemented();
		
		int[] mangaIdsArray = deleteQueue.toMangaIdsArray();

		try {
			String sql = qm().select(MANGA_ID, DIR_NAME).from(MANGAS_TABLE_NAME).where(w -> w.in(MANGA_ID, mangaIdsArray)).build();

			StringBuilder sb = new StringBuilder();
			sb.append("\nSQL\n  ").append(sql).append("\r\n\r\n");

			List<File> dirs = new ArrayList<>();
			List<File> dirs2 = dirs;
			db.iterate(sql, rs -> {
				String name = rs.getString(DIR_NAME);
				int id = rs.getInt(MANGA_ID);

				sb.append("id: ").append(id)
				.append(",  name:").append(name);

				File dir = new File(MyConfig.MANGA_DIR, name);

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
						.filter(File::exists)
						.peek(f -> Stream.of(f.listFiles()).forEach(File::delete))
						.filter(f -> !f.delete())
						.collect(Collectors.toList());

				if(!dirs.isEmpty()){
					sb.append("\r\n\r\nFiles needs Attention \r\n");
					for (File f : dirs) sb.append(f).append(System.lineSeparator());
				}

				String format = qm().deleteFrom("%s").where(w -> w.in(MANGA_ID, mangaIdsArray)).build();

				try(Statement s = db.createStatement()) {
					for (String table : new String[] {MANGAS_TABLE_NAME, MangaUrlsMeta.TABLE_NAME, RecentsMeta.RECENTS_TABLE_NAME, ChaptersMeta.CHAPTERS_TABLE_NAME}) {
						s.addBatch(String.format(format, table));
					}
					sb.append("executes: "+s.executeBatch());
				}
				// FIXME db.commit();
			}
			else 
				Utils.showHidePopup("delete cancelled", 1500);

		}
		catch (SQLException   e) {
			Utils.getLogger(ProcessDeleteQueue.class)
			.error("error while deleting from database ids\r\n {}", Arrays.toString(mangaIdsArray), e);
			return;
		}
	}

}
