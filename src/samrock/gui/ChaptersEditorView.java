package samrock.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

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

import samrock.manga.Chapter;
import samrock.manga.Manga;
import samrock.manga.MangaManeger;
import samrock.utils.RH;
import samrock.utils.Utils;

public final class ChaptersEditorView extends JPanel {
	private static final long serialVersionUID = -4470974173446749547L;

	private final JTable chapterTable;
	private final JLabel chaptersCountLabel = new JLabel("", JLabel.CENTER);
	private Chapter[] initialChapters;
	private boolean[] initialReadStatus;
	private String[] initialChapterExtensionLessNames;
	private Manga manga;
	private final int readUnreadColumnWidth;
	private final int deleteColumnWidth;

	private static final int READ_UNREAD_COLUMN = 0;
	private static final int CHAPTER_NAME_COLUMN = 1;
	private static final int DELETE_COLUMN = 2;
	private final Font DEFAULT_FONT; 

	public ChaptersEditorView() {
		super(new BorderLayout(2 , 2), false);

		Color default_background = RH.getColor("chaptertableeditor.background");
		Color default_foreground = RH.getColor("chaptertableeditor.foreground");

		DEFAULT_FONT = RH.getFont("chaptertableeditor.table.font");

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

		chapterTable.setDefaultRenderer(Boolean.class, new TableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				JCheckBox c = (JCheckBox) renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				if(!model.getChapter(row).chapterFileExists()){
					c.setBackground(red);
					c.setToolTipText("File does not Exists");
				}				
				else if(column == READ_UNREAD_COLUMN)
					c.setBackground(c.isSelected() ?  default_foreground : default_background);
				else
					c.setBackground(c.isSelected() ? red : green);

				return c;
			}
		});

		Border labelBorder = new EmptyBorder(2, 10, 2, 2);
		Color background_when_selected = RH.getColor("chaptertableeditor.selected.background");
		Color foreground_when_selected = RH.getColor("chaptertableeditor.selected.foreground");
		Color white = Color.white;

		chapterTable.setDefaultRenderer(String.class, new TableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {

				JLabel l = new JLabel((String)value);
				l.setFont(DEFAULT_FONT);
				l.setBorder(labelBorder);
				l.setOpaque(true);
				Chapter c = model.getChapter(row);

				if(!c.chapterFileExists()){
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
					boolean b = c.isRead();
					l.setForeground(b ? default_background : default_foreground);
					l.setBackground(!b ? default_background : default_foreground);
				}
				return l;
			}
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

		header.setDefaultRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {

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
			}
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
		ji.addActionListener(e -> Utils.openFile(manga.getMangaFolderPath().toFile()));
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
		manga = MangaManeger.getInstance().getCurrentManga();
		
		initialChapters = new Chapter[manga.getChaptersCount()];

		for (int i = 0; i < initialChapters.length; i++) initialChapters[i] = manga.getChapter(i);

		if(changeChapterModel){
			chapterTable.setModel(getModel(MangaChapterModel.MODE_SHOW_ALL));
			chapterTable.getColumnModel().getColumn(READ_UNREAD_COLUMN).setMaxWidth(readUnreadColumnWidth);
			chapterTable.getColumnModel().getColumn(DELETE_COLUMN).setMaxWidth(deleteColumnWidth);
			revalidate();
			repaint();
		}

		
	}

	public void cancel() {
		if(initialReadStatus == null)
			return;

		

		for (int i = 0; i < initialReadStatus.length; i++) {
			initialChapters[i].setRead(initialReadStatus[i]);
			initialChapters[i].setInDeleteQueue(false);
		}

		Chapter[] cs = new Chapter[initialChapters.length];

		//first try rename rollback
		for (int i = 0; i < initialChapters.length; i++){
			if(!initialChapters[i].getName().equals(initialChapterExtensionLessNames[i]) && initialChapters[i].rename(initialChapterExtensionLessNames[i]) != null)
				cs[i] = initialChapters[i];
		}

		if(Stream.of(cs).anyMatch(Objects::nonNull)){
			//second try rename rollback
			String str = String.valueOf(System.currentTimeMillis());
			int j = 0;
			for (Chapter c : cs) if(c != null) c.rename(str.concat(String.valueOf(j++)));

			int failedCount = 0;
			for (int i = 0; i < cs.length; i++){
				if(cs[i] != null && cs[i].rename(initialChapterExtensionLessNames[i]) != null)
					failedCount++;
			}
			if(failedCount != 0)
				Utils.showHidePopup("Rename Rollback, Failed Count: "+failedCount, 2000);
		}

		initialReadStatus = null;
		initialChapterExtensionLessNames = null;
		manga.setBatchEditingMode(false);

		Utils.showHidePopup("Changes Cancelled", 1000);
		chapterTable.revalidate();
		chapterTable.repaint();
		
	}

	private void save() {
		if(initialReadStatus == null){
			Utils.showHidePopup("Nothing to save", 1000);
			return;
		}

		

		StringBuilder delete = new StringBuilder();
		StringBuilder rename = new StringBuilder();
		StringBuilder read = new StringBuilder();

		String renameFormat = "Old Name: %s\r\nNew Name: %s\r\n";
		String readFormat = "%-10s%-10s%s";

		for (int i = 0; i < initialChapters.length; i++) {
			if(initialChapters[i].isInDeleteQueue())
				delete.append(initialChapters[i].getName()).append((!initialChapters[i].getName().equals(initialChapterExtensionLessNames[i]) ? "\t(old name: "+ initialChapterExtensionLessNames[i] +")": "")).append(System.lineSeparator());
			if(!initialChapters[i].getName().equals(initialChapterExtensionLessNames[i]))
				rename.append(String.format(renameFormat, initialChapterExtensionLessNames[i], initialChapters[i].getName())).append(System.lineSeparator());
			if(initialChapters[i].isRead() != initialReadStatus[i])
				read.append(String.format(readFormat, initialReadStatus[i] ? "read" : "unread", initialChapters[i].isRead()? "read" : "unread", initialChapters[i].getName())).append(System.lineSeparator());
		}

		boolean deletesFound  = false;
		boolean renameFound = false;
		StringBuilder combined = new StringBuilder();
		if(delete.length() != 0){
			deletesFound  = true;
			combined.append("################################\r\n")
			.append("########### DELETE LIST ########\r\n")
			.append("################################\r\n")
			.append(delete)
			.append("\r\n\r\n");
		}
		if(rename.length() != 0){
			combined.append("################################\r\n")
			.append("########### RENAME CHANGE ########\r\n")
			.append("################################\r\n")
			.append(rename)
			.append("\r\n\r\n");
			renameFound = true;
		}
		if(read.length() != 0){
			combined.append("################################\r\n")
			.append("########### READ/UNREAD CHANGES ########\r\n")
			.append("################################\r\n")
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

		if(deletesFound){
			Stream.of(initialChapters)
			.filter(Chapter::isInDeleteQueue)
			.forEach(Chapter::delete);
		}

		initialReadStatus = null;
		initialChapterExtensionLessNames = null;

		if(deletesFound)
			manga.finalizeMangaChanges();
		else
			manga.resetCounts();

		manga.setBatchEditingMode(false);

		if(manga.getChapCountPc() == 0)
			MangaManeger.getInstance().removeMangaFromDeleteQueue(manga);

		reset(true);
		Utils.showHidePopup("Saved", 1000);

		
	}

	private void backup() {
		if(initialReadStatus != null)
			return;

		manga.setBatchEditingMode(true);
		initialReadStatus = new boolean[initialChapters.length];
		initialChapterExtensionLessNames = new String[initialChapters.length];

		for (int i = 0; i < initialChapters.length; i++) {
			initialReadStatus[i] = initialChapters[i].isRead();
			initialChapterExtensionLessNames[i] = initialChapters[i].getName();
		}
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

		private Chapter[] chapters;
		private int size;

		public MangaChapterModel(int mode) {
			chapters = initialChapters;

			if(mode == MODE_SHOW_UNREAD)
				chapters = Stream.of(initialChapters).filter(c -> !c.isRead()).toArray(Chapter[]::new);
			else if(mode == MODE_SHOW_READ)
				chapters = Stream.of(initialChapters).filter(c -> c.isRead()).toArray(Chapter[]::new);

			size = chapters.length;

			chaptersCountLabel.setText("Chapters Count: ".concat(String.valueOf(size)));

			if(size == 0)
				Utils.showHidePopup((mode == MODE_SHOW_ALL ? "Chapters" : mode == MODE_SHOW_UNREAD ? "Unread " : "Read")+" Count = 0", 2000);
		}

		public void markAll(int[] selectedRows, boolean markRead) {
			if(selectedRows.length == 0 || selectedRows.length == chapters.length){
				for (int i = 0; i < chapters.length; i++) 
					setValueAt(markRead, i, READ_UNREAD_COLUMN);
			}
			else
				for (int i : selectedRows) setValueAt(markRead, i, READ_UNREAD_COLUMN); 

			chapterTable.revalidate();
			chapterTable.repaint();
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			Chapter c = chapters[rowIndex];
			backup();
			if(columnIndex == READ_UNREAD_COLUMN)
				c.setRead((boolean) value);
			else if(columnIndex == DELETE_COLUMN)
				c.setInDeleteQueue((boolean) value);
			else{
				String status = c.rename((String)value);
				if(status != null)
					Utils.showHidePopup(status, 2000);
				else 
					Utils.showHidePopup("Renaming success", 2000);
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) { return true; }

		@Override
		public
		Object getValueAt(int rowIndex, int columnIndex) {
			Chapter c = getChapter(rowIndex);

			if(columnIndex == READ_UNREAD_COLUMN)
				return c.isRead();
			else if(columnIndex == DELETE_COLUMN)
				return c.isInDeleteQueue();
			else
				return c.getName();
		}

		Chapter getChapter(int rowIndex){
			return chapters[rowIndex];
		}

		@Override
		public int getRowCount() { return size; }

		@Override
		public String getColumnName(int columnIndex) {
			if(columnIndex == READ_UNREAD_COLUMN)
				return "Read?";
			else if(columnIndex == DELETE_COLUMN)
				return "Delete?";
			else
				return "Chapter Name";
		}

		@Override
		public int getColumnCount() { return 3; }

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return   columnIndex == READ_UNREAD_COLUMN || columnIndex == DELETE_COLUMN ? Boolean.class : String.class;
		}
	}

	public void rename(int row) {
		Chapter chapter = model.getChapter(row);

		if(!chapter.chapterFileExists()){
			Utils.showHidePopup("Renaming Failed: File does not Exists", 2000);
			return;
		}

		String oldName = chapter.getName();
		String newName = JOptionPane.showInputDialog("<html>Old Name: "+oldName+"<br>New Name: ?", oldName);

		if(newName == null || newName.trim().isEmpty() || oldName.equals(newName))
			return;

		chapterTable.setValueAt(newName, row, CHAPTER_NAME_COLUMN);

	};
}


