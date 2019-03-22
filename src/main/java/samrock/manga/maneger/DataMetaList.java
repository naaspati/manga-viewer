package samrock.manga.maneger;

import sam.io.infile.DataMeta;

class DataMetaList {
	private final DataMeta[] array;
	private int mod;
	
	public DataMetaList(int size) {
		this.array = new DataMeta[size];
	}
	public DataMeta get(int index) {
		return array[index];
	}
	public void set(int index, DataMeta dm) {
		array[index] = dm;
		mod++;
	}
}
