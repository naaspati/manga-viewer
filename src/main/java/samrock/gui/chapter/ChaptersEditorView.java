package samrock.gui.chapter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import org.slf4j.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import sam.console.ANSI;
import sam.nopkg.Junk;
import samrock.PrintFinalize;
import samrock.RH;
import samrock.Utils;
import samrock.manga.Chapter;
import samrock.manga.Manga;
import samrock.manga.maneger.MangaManeger;

public final class ChaptersEditorView extends JPanel implements PrintFinalize{
	private static final int READ_UNREAD_COLUMN = 0;
	private static final int CHAPTER_NAME_COLUMN = 1;
	private static final int DELETE_COLUMN = 2;
	private static final Font DEFAULT_FONT;

	private static final long serialVersionUID = -4470974173446749547L;
	private final static Logger logger = Utils.getLogger(ChaptersEditorView.class);

	private final JTable chapterTable;
	private final JLabel chaptersCountLabel = new JLabel("", JLabel.CENTER);
	private final ArrayList<ChapterWrap> chapters = new ArrayList<>();
	private Manga manga;
	private final int readUnreadColumnWidth;
	private final int deleteColumnWidth;
	private boolean started;

	private class ChapterWrap {
		final Chapter chapter;
		boolean delete, read;
		String oldName;

		ChapterWrap(Chapter chapter) {
			this.chapter = chapter;
			reset();
		}
		void reset() {
			read = chapter.isRead();
			delete = false;
			oldName = null;
		}
		void setDelete(boolean delete) {
			this.delete = delete;
			started = true;
		} 
		void setRead(boolean read) {
			this.read = read;
			started = true;
		}
		String getTitle() {
			return chapter.getTitle();
		}
		boolean rename(String newName) throws IOException {
			String oldName = chapter.getTitle();
			if(chapter.rename(newName)) {
				this.oldName = oldName;
				return true;
			}
			// return false;
			
			//FIXME // plan to move to Chapters
			return Junk.notYetImplemented();
		}
		void commit() throws IOException {
			if(chapter.isRead() != read)
				chapter.setRead(read);
			if(delete)
				manga.getChapters().delete(chapter);
		}
		public boolean fileExists() {
			return chapter.chapterFileExists();
		}
	}

	static {
		DEFAULT_FONT = RH.getFont("chaptertableeditor.table.font");
	}

	public ChaptersEditorView() {
		super(new BorderLayout(2 , 2), false);

		Color default_background = RH.getColor("chaptertableeditor.background");
		Color default_foreground = RH.getColor("chaptertableeditor.foreground");


		ImageIcon readUnreadHeaderIcon = RH.getImageIcon("chaptertableeditor.header.read.unread.icon");
		ImageIcon deleteHeaderIcon = RH.getImageIcon("chaptertableeditor.header.delete.icon");

		readUnreadColumnWidth = readUnreadHeaderIcon.getIconWidth()+10;
		deleteColumnWidth = deleteHeaderIcon.getIconWidth()+100;

		setOpaque(true);
		setBackground(default_background);
		setForeground(default_foreground);

		reset(false);

		chapterTable = new JTable(getModel(MangaChapterModel.MODE_SHOW_ALL));

		chapterTable.setDoubleBuffered(false);		
		chapterTable.setBackground(default_background);
		chapterTable.setForeground(default_foreground);
		chapterTable.setRowHeight(RH.getInt("chaptertableeditor.table.row_height"));
		chapterTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		chapterTable.getColumnModel().getColumn(READ_UNREAD_COLUMN).setMaxWidth(readUnreadColumnWidth);
		chapterTable.getColumnModel().getColumn(DELETE_COLUMN).setMaxWidth(deleteColumnWidth);

		chapterTable.getColumnModel().setColumnSelectionAllowed(false);

		chapterTable.setDefaultEditor(String.class, new MangaChapterTableCellEditor());

		chapterTable.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				boolean reset = false;
				switch (e.getKeyCode()) {
					case KeyEvent.VK_F2:
						if(chapterTable.getSelectedRowCount() == 0)
							Utils.showHidePopup("nothing selected", 1000);
						else if(chapterTable.getSelectedRowCount() != 1)
							Utils.showHidePopup("select only one", 1000);
						else
							rename(chapterTable.getSelectedRow());
						break;
					case KeyEvent.VK_R:
						chapterTable.setValueAt(!(boolean)chapterTable.getValueAt(chapterTable.getSelectedRow(), READ_UNREAD_COLUMN), chapterTable.getSelectedRow(), READ_UNREAD_COLUMN);
						reset = true;
						break;
					case KeyEvent.VK_DELETE:
						chapterTable.setValueAt(!(boolean)chapterTable.getValueAt(chapterTable.getSelectedRow(), DELETE_COLUMN), chapterTable.getSelectedRow(), DELETE_COLUMN);
						reset = true;
						break;
					default:
						break;
				}
				if(reset){
					chapterTable.revalidate();
					chapterTable.repaint();
				}
			}
		});

		TableCellRenderer renderer = chapterTable.getDefaultRenderer(Boolean.class);
		Color red = Color.red;
		Color green = Color.green;

		chapterTable.setDefaultRenderer(Boolean.class, (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) -> {
			JCheckBox c = (JCheckBox) renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if(!model.getChapter(row).fileExists()){
				c.setBackground(red);
				c.setToolTipText("File does not Exists");
			}				
			else if(column == READ_UNREAD_COLUMN)
				c.setBackground(c.isSelected() ?  default_foreground : default_background);
			else
				c.setBackground(c.isSelected() ? red : green);

			return c;
		});

		Border labelBorder = new EmptyBorder(2, 10, 2, 2);
		Color background_when_selected = RH.getColor("chaptertableeditor.selected.background");
		Color foreground_when_selected = RH.getColor("chaptertableeditor.selected.foreground");
		Color white = Color.white;

		chapterTable.setDefaultRenderer(String.class, (JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column)  -> {

					JLabel l = new JLabel((String)value);
					l.setFont(DEFAULT_FONT);
					l.setBorder(labelBorder);
					l.setOpaque(true);
					ChapterWrap  c = model.getChapter(row);

					if(!c.fileExists()){
						l.setBackground(red);
						l.setForeground(white);
						l.setToolTipText("File does not Exists");
						return l;
					}

					if(isSelected){
						l.setBackground(background_when_selected);
						l.setForeground(foreground_when_selected);
					}
					else{
						boolean b = c.read;
						l.setForeground(b ? default_background : default_foreground);
						l.setBackground(!b ? default_background : default_foreground);
					}
					return l;
				});


		Color header_background = RH.getColor("chaptertableeditor.header.background");
		Color header_foreground = RH.getColor("chaptertableeditor.header.foreground");
		Font header_font = RH.getFont("chaptertableeditor.header.font");

		JTableHeader header = chapterTable.getTableHeader();
		header.setFont(header_font);
		header.setOpaque(true);
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, RH.getColor("chaptertableeditor.header.separator.color")));
		header.setBackground(header_background);
		header.setForeground(header_foreground);
		String readUnreadHeaderTooltip = RH.getString("chaptertableeditor.header.read.unread.tooltip");
		String deleteHeaderTooltip = RH.getString("chaptertableeditor.header.delete.tooltip");

		Border border = BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 0, 1, Color.white), new EmptyBorder(10, 20, 10, 20));

		header.setDefaultRenderer((JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) -> {

					JLabel l;
					if(column == READ_UNREAD_COLUMN || column == DELETE_COLUMN){
						l =  new JLabel(column == READ_UNREAD_COLUMN ? readUnreadHeaderIcon : deleteHeaderIcon);
						l.setToolTipText(column == READ_UNREAD_COLUMN ? readUnreadHeaderTooltip : deleteHeaderTooltip);
					}
					else {
						l = new JLabel((String) value);
						l.setFont(header_font);
						l.setForeground(header_foreground);
					}

					l.setBorder(border);

					return l;
				});

		add(new JScrollPane(chapterTable), BorderLayout.CENTER);

		JPanel controlTop = Utils.createJPanel(new BoxLayout(null, BoxLayout.X_AXIS));

		IntConsumer changeListing = mode -> {
			chapterTable.setModel(getModel(mode));
			chapterTable.getColumnModel().getColumn(READ_UNREAD_COLUMN).setMaxWidth(readUnreadColumnWidth);
			chapterTable.getColumnModel().getColumn(DELETE_COLUMN).setMaxWidth(deleteColumnWidth);
			chapterTable.revalidate();
			chapterTable.repaint();
		};

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem ji  = new JMenuItem("Reset Manga");
		ji.setToolTipText("Check chapter list in pc");
		ji.addActionListener(e -> {
			manga.resetChapters();
			reset(true);
		});
		popupMenu.add(ji);

		ji  = new JMenuItem("List All");
		ji.addActionListener(e -> changeListing.accept(MangaChapterModel.MODE_SHOW_ALL));
		popupMenu.add(ji);

		ji  = new JMenuItem("List Only Unread");
		ji.addActionListener(e -> changeListing.accept(MangaChapterModel.MODE_SHOW_UNREAD));
		popupMenu.add(ji);

		ji  = new JMenuItem("List Only Read");
		ji.addActionListener(e -> changeListing.accept(MangaChapterModel.MODE_SHOW_READ));
		popupMenu.add(ji);

		ji = new JMenuItem("Open Manga Folder");
		ji.addActionListener(e -> Utils.openFile(manga.getDir().toFile()));
		popupMenu.add(ji);

		ji = new JMenuItem("Clear Selections");
		ji.addActionListener(e -> chapterTable.clearSelection());
		popupMenu.add(ji);

		JButton markReadButton = Utils.createButton("chaptertableeditor.button.markread.icon", "chaptertableeditor.button.markread.tooltip", null, null, e -> model.markAll(chapterTable.getSelectedRows(), true));
		JButton markUnreadButton = Utils.createButton("chaptertableeditor.button.markunread.icon", "chaptertableeditor.button.markunread.tooltip", null, null,e -> model.markAll(chapterTable.getSelectedRows(), false));
		JButton saveChapterEditButton = Utils.createButton("chaptertableeditor.button.savechapteredit.icon", "chaptertableeditor.button.savechapteredit.tooltip", null, null, e -> save());
		JButton cancelChapterEditButton = Utils.createButton("chaptertableeditor.button.cancelchapteredit.icon", "chaptertableeditor.button.cancelchapteredit.tooltip", null, null, e -> cancel());
		JButton menuButton = Utils.createMenuButton(e -> popupMenu.show((JButton)e.getSource(), 0, 0));

		chaptersCountLabel.setFont(DEFAULT_FONT); 
		chaptersCountLabel.setBorder(new EmptyBorder(0, 20, 0, 50));
		chaptersCountLabel.setForeground(default_foreground);

		JLabel l = new JLabel("<html>Keys: <br>R:&emsp;Toggle Read/Unread, &emsp;Delete:&emsp;Toggle Delete, &emsp;F2:&emsp;ToggleRename</html>", JLabel.CENTER);
		l.setFont(DEFAULT_FONT.deriveFont((float)(DEFAULT_FONT.getSize() - 5))); 
		l.setForeground(default_foreground);

		menuButton.setText("Menu");
		menuButton.setIconTextGap(5);
		menuButton.setFont(header_font);
		menuButton.setForeground(default_foreground);
		menuButton.setHorizontalTextPosition(SwingConstants.LEFT);

		controlTop.add(markReadButton);
		controlTop.add(markUnreadButton);
		controlTop.add(menuButton);
		controlTop.add(Box.createGlue());
		controlTop.add(l);
		controlTop.add(chaptersCountLabel);
		controlTop.add(saveChapterEditButton);
		controlTop.add(cancelChapterEditButton);

		saveChapterEditButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		cancelChapterEditButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

		markReadButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		markUnreadButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		menuButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		chaptersCountLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		l.setAlignmentX(Component.RIGHT_ALIGNMENT);

		add(controlTop, BorderLayout.NORTH);

	}

	private MangaChapterModel model;
	private MangaChapterModel getModel(int mode){
		return model = new MangaChapterModel(mode);
	} 

	public void  changeManga(){ reset(true); }

	private void reset(boolean changeChapterModel) {
		started = false;
		chapters.clear();
		manga = MangaManeger.getCurrentManga();

		for (Chapter chapter : manga) 
			chapters.add(new ChapterWrap(chapter));

		if(changeChapterModel){
			chapterTable.setModel(getModel(MangaChapterModel.MODE_SHOW_ALL));
			chapterTable.getColumnModel().getColumn(READ_UNREAD_COLUMN).setMaxWidth(readUnreadColumnWidth);
			chapterTable.getColumnModel().getColumn(DELETE_COLUMN).setMaxWidth(deleteColumnWidth);
			revalidate();
			repaint();
		}
	}

	public void cancel() {
		if(!started)
			return;

		started = false;
		chapters.forEach(ChapterWrap::reset);
		Utils.showHidePopup("Changes Cancelled", 1000);
		chapterTable.revalidate();
		chapterTable.repaint();

	}
	private void save() {
		if(!started){
			Utils.showHidePopup("Nothing to save", 1000);
			return;
		}

		started = false;

		StringBuilder delete = new StringBuilder();
		StringBuilder rename = new StringBuilder();
		StringBuilder read = new StringBuilder();

		String renameFormat = "Old Name: %s\r\nNew Name: %s\r\n";
		String readFormat = "%-10s%-10s%s";

		for (ChapterWrap c : chapters) {
			if(c.delete)
				delete.append(c.chapter.getTitle()).append((c.oldName != null ? "\t(old name: "+ c.oldName +")": "")).append(System.lineSeparator());
			if(c.oldName != null)
				rename.append(String.format(renameFormat, c.oldName, c.getTitle())).append(System.lineSeparator());
			if(c.chapter.isRead() != c.read)
				read.append(String.format(readFormat, c.read ? "read" : "unread", c.chapter.isRead() ? "read" : "unread", c.getTitle())).append(System.lineSeparator());
		} 

		boolean deletesFound  = false;
		boolean renameFound = false;
		StringBuilder combined = new StringBuilder();
		if(delete.length() != 0){
			deletesFound  = true;
			combined.append(ANSI.createUnColoredBanner("DELETE LIST"))
			.append('\n')
			.append(delete)
			.append("\r\n\r\n");
		}
		if(rename.length() != 0){
			combined.append(ANSI.createUnColoredBanner("RENAME CHANGE"))
			.append('\n')
			.append(rename)
			.append("\r\n\r\n");
			renameFound = true;
		}
		if(read.length() != 0){
			combined.append(ANSI.createUnColoredBanner("READ/UNREAD CHANGES"))
			.append('\n')
			.append(String.format(readFormat, "old", "new", "chapter name"))			
			.append(read)
			.append("\r\n\r\n");
		}	

		String string = combined.toString().trim();

		if(!string.isEmpty()){
			if((deletesFound || renameFound)){
				JTextArea ta = new JTextArea(string, 20, 30);
				ta.setFont(DEFAULT_FONT);
				int option = JOptionPane.showConfirmDialog(this, new JScrollPane(ta), "Save Changes?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);
				if(option != JOptionPane.YES_OPTION){
					Utils.showHidePopup("save cancelled", 1000);
					return;
				}
			}
		}
		else{
			Utils.showHidePopup("Nothing to save", 1000);
			return;
		}

		for (ChapterWrap c : chapters) {
			try {
				c.commit();
			} catch (IOException e) {
				Utils.getLogger(getClass()).log(Level.SEVERE, "failed: "+c.chapter, e);
			}
		}
		manga.getChapters().resetCounts();
		reset(true);
		Utils.showHidePopup("Saved", 1000);
	}

	private final class MangaChapterTableCellEditor extends AbstractCellEditor implements TableCellEditor{
		private static final long serialVersionUID = -8395855424927544010L;

		//private String editorValue;

		@Override
		public Object getCellEditorValue() {
			//return editorValue;
			return null;
		}

		@Override
		public
		boolean isCellEditable(EventObject e) {
			if (e instanceof MouseEvent) {
				MouseEvent m = (MouseEvent) e;
				return m.getClickCount() >= 2;
			}
			return false;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			rename(row);
			return null;
		}
	}

	private final class MangaChapterModel extends AbstractTableModel {
		private static final long serialVersionUID = -2996621646803149003L;
		private static final int MODE_SHOW_ALL = 0x900;
		private static final int MODE_SHOW_UNREAD = 0x901;
		private static final int MODE_SHOW_READ = 0x902;

		private ArrayList<ChapterWrap> content;

		public MangaChapterModel(int mode) {
			content = new ArrayList<>(ChaptersEditorView.this.chapters);

			if(mode == MODE_SHOW_UNREAD)
				content.removeIf(c -> c.read);
			else if(mode == MODE_SHOW_READ)
				content.removeIf(c -> !c.read);

			chaptersCountLabel.setText("Chapters Count: ".concat(String.valueOf(content.size())));

			if(content.isEmpty())
				Utils.showHidePopup((mode == MODE_SHOW_ALL ? "Chapters" : mode == MODE_SHOW_UNREAD ? "Unread " : "Read")+" Count = 0", 2000);
		}

		public void markAll(int[] selectedRows, boolean markRead) {
			if(selectedRows.length == 0 || selectedRows.length == content.size()){
				for (int i = 0; i < content.size(); i++) 
					setValueAt(markRead, i, READ_UNREAD_COLUMN);
			}
			else
				for (int i : selectedRows) setValueAt(markRead, i, READ_UNREAD_COLUMN); 

			chapterTable.revalidate();
			chapterTable.repaint();
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int index) {
			ChapterWrap c = content.get(rowIndex);
			if(index == READ_UNREAD_COLUMN)
				c.setRead((boolean) value);
			else if(index == DELETE_COLUMN)
				c.setDelete((boolean) value);
			else{
				try {
					if(c.rename((String)value))
						Utils.showHidePopup("Renaming success", 2000);
				} catch (IOException e) {
					logger.log(Level.SEVERE, "renaming failed", e);
				}
			}
		}
		@Override
		public boolean isCellEditable(int rowIndex, int index) { return true; }

		@Override
		public
		Object getValueAt(int rowIndex, int index) {
			ChapterWrap  c = getChapter(rowIndex);

			if(index == READ_UNREAD_COLUMN)
				return c.read;
			else if(index == DELETE_COLUMN)
				return c.delete;
			else
				return c.getTitle();
		}

		ChapterWrap getChapter(int rowIndex){
			return content.get(rowIndex);
		}

		@Override
		public int getRowCount() { return content.size(); }

		@Override
		public String getColumnName(int index) {
			if(index == READ_UNREAD_COLUMN)
				return "Read?";
			else if(index == DELETE_COLUMN)
				return "Delete?";
			else
				return "Chapter Name";
		}

		@Override
		public int getColumnCount() { return 3; }

		@Override
		public Class<?> getColumnClass(int index) {
			return   index == READ_UNREAD_COLUMN || index == DELETE_COLUMN ? Boolean.class : String.class;
		}
	}

	@Deprecated
	/**
	 * move this to 
	 * FIXME
	 */
	public void rename(int row) {
		ChapterWrap chapter = model.getChapter(row);

		if(!chapter.fileExists()){
			Utils.showHidePopup("Renaming Failed: File does not Exists", 2000);
			return;
		}

		String oldName = chapter.getTitle();
		String newName = JOptionPane.showInputDialog("<html>Old Name: "+oldName+"<br>New Name: ?", oldName);

		if(newName == null || newName.trim().isEmpty() || oldName.equals(newName))
			return;

		chapterTable.setValueAt(newName, row, CHAPTER_NAME_COLUMN);
	}

	@Override
	protected void finalize() throws Throwable {
		printFinalize();
	}
}


