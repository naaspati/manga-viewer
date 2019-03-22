package samrock.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;

import sam.io.IOConstants;
import sam.io.serilizers.IntSerializer;
import sam.logging.MyLoggerFactory; 

public final class Serializers {
	private static final Logger LOGGER = Utils.getLogger(Serializers.class);
	private static final ByteBuffer buffer = ByteBuffer.allocate(IOConstants.defaultBufferSize());

	public static int[] ints(Path path) throws IOException {
		if(Files.notExists(path))
			return null;

		synchronized (buffer) {
			int[] a = IntSerializer.readArray(path, buffer);
			LOGGER.fine(() -> "read: " + Utils.subpath(path));
			return a;
		}
	}
	
	public static void write(int[] array, Path p) throws IOException {
		Objects.requireNonNull(array);
		Objects.requireNonNull(p);
		
		synchronized (buffer) {
			IntSerializer.write(array, p, buffer);
			LOGGER.fine(() -> "write: " + Utils.subpath(p));
		}
	}


}
