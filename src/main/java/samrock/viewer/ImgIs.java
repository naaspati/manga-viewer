package samrock.viewer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;

class ImgIs implements ImageInputStream {
	private final ByteBuffer buf = ByteBuffer.allocate(8 * 1024);

	@Override
	public void setByteOrder(ByteOrder byteOrder) {
		buf.clear();
		buf.order(byteOrder);
	}

	@Override
	public ByteOrder getByteOrder() {
		return buf.order();
	}

	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int read(byte[] b) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void readBytes(IIOByteBuffer buf, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean readBoolean() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte readByte() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readUnsignedByte() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short readShort() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readUnsignedShort() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public char readChar() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readInt() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long readUnsignedInt() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long readLong() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float readFloat() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double readDouble() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String readLine() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String readUTF() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFully(short[] s, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFully(char[] c, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFully(int[] i, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFully(long[] l, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFully(float[] f, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFully(double[] d, int off, int len) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getStreamPosition() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBitOffset() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setBitOffset(int bitOffset) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int readBit() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long readBits(int numBits) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long length() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int skipBytes(int n) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long skipBytes(long n) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void seek(long pos) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mark() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flushBefore(long pos) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getFlushedPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isCached() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCachedMemory() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCachedFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
