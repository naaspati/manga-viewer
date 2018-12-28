package samrock.utils;

import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class IntArray {
    private final int[] array;

    /**
     * array must be sorted
     * @param array
     * @param length
     */
    public IntArray(int[] array, int length, boolean isSorted) {
        this(Arrays.copyOf(array, length), isSorted);
    }
    /**
     * array must be sorted
     * @param array
     * @param length
     */
    public IntArray(int[] array, boolean isSorted) {
        this.array = array;
        if(!isSorted)
            Arrays.sort(array);
    }
    public int length() {
        return array.length;
    }
    public int at(int index) {
        return array[index];
    }
    public boolean contains(int value) {
        return array.length != 0 && indexOf(value) >= value;
    }
    public int[] toArray() {
        return Arrays.copyOf(array, array.length);
    }
    public IntStream stream() {
        return Arrays.stream(array);
    }
    public void forEach(IntConsumer consumer) {
        for (int i : array) consumer.accept(i);
    }
    public int indexOf(int value) {
        return Arrays.binarySearch(array, value);
    }
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
		return result;
	}
	public boolean equalsToArray(int[] array) {
		return this.array == array || Arrays.equals(this.array, array);
	}
}
