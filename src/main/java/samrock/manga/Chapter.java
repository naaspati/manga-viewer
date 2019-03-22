package samrock.manga;

import sam.manga.samrock.chapters.ChapterWithId;
import sam.manga.samrock.chapters.MinimalChapter;

public abstract class Chapter implements ChapterWithId {
	protected final int id;
	protected double number;
	protected String filename;
	
	public Chapter(int id, double number, String filename) {
		this.id = id;
		this.number = number;
		this.filename = filename;
	}

	@Override
	public double getNumber() {
		return number;
	}
	
    private String _filename, title;

	@Override
	public String getTitle() {
		if(_filename == filename)
			return title;
		
		_filename = filename;
		return title = MinimalChapter.getTitleFromFileName(filename);
	}

	@Override
	public String getFileName() {
		return filename;
	}

	@Override
	public int getChapterId() {
		return id;
	}
	public abstract boolean isRead();
	public abstract void setRead(boolean read);
}
