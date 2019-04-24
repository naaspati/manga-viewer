package samrock.api;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import sam.reference.ReferenceType;

public class ReferenceList<E> {
	private final ArrayList<WeakReference<E>> list = new ArrayList<>();
	private final Supplier<E> generator;
	private final ReferenceType type;
	
	public ReferenceList(ReferenceType type, Supplier<E> generator) {
		this.generator = generator;
		this.type = type;
	}

	public void forEach(Consumer<E> object) {
		// TODO Auto-generated method stub
		
	}

	public E poll() {
		// TODO Auto-generated method stub
		return null;
	}

	public void add(E v) {
		// TODO Auto-generated method stub
		
	}

	public void addAll(Collection<E> added) {
		// TODO Auto-generated method stub
		
	}
}
