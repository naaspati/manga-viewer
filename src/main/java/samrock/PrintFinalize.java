package samrock;

public interface PrintFinalize {
	default void printFinalize() {
		printFinalize(getClass());
	}

	@SuppressWarnings("rawtypes")
	static void printFinalize(Class c) {
		Utils.getLogger(c).info("garbaged");
	}
	@SuppressWarnings("rawtypes")
	static void printFinalize(Class c, String msg) {
		Utils.getLogger(c).info("garbaged: "+msg);
	}
}
