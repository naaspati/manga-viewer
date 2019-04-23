package samrock.gui;

import java.util.function.Supplier;

import sam.reference.LazyReference;
import sam.reference.ReferenceType;
import samrock.PrintFinalize;

public class SoftAndLazyWrapper<E extends PrintFinalize> extends LazyReference<E>{
	private E solidRefrence;
    
	public SoftAndLazyWrapper(Supplier<E> generator) {
		super(generator, ReferenceType.SOFT);
	}
	@Override
	public E get() {
		if(solidRefrence != null)
			return solidRefrence;
		return solidRefrence = super.get();
	}
	@Override
	public void clear() {
		solidRefrence = null;
		// super.clear();
	}
}
