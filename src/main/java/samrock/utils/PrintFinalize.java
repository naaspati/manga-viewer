package samrock.utils;

import sam.logging.MyLoggerFactory;

public interface PrintFinalize {
	default void printFinalize() {
		printFinalize(getClass());
	}

	@SuppressWarnings("rawtypes")
	static void printFinalize(Class c) {
		MyLoggerFactory.logger(c).info("garbaged");
	}
	@SuppressWarnings("rawtypes")
	static void printFinalize(Class c, String msg) {
		MyLoggerFactory.logger(c).info("garbaged: "+msg);
	}
}
