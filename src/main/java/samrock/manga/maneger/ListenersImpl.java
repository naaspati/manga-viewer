package samrock.manga.maneger;

import java.util.Arrays;

import sam.myutils.Checker;
import samrock.manga.maneger.api.ChangeListener;
import samrock.manga.maneger.api.Listeners;

@SuppressWarnings({"rawtypes", "unchecked"})
class ListenersImpl<E,F> implements Listeners<E, F> {
	private ChangeListener[] listeners;

	@Override
	public void addChangeListener(ChangeListener<E,F> listener) {
		if(listener != null && indexOf(listener) == -1) {
			int n = indexOf(null);
			if(n != -1)
				listeners[n] = listener;
			else {
				listeners = listeners == null ? new ChangeListener[1] : Arrays.copyOf(listeners, listeners.length + 1);
				listeners[listeners.length - 1] = listener;
			}
		}
	}
	private int indexOf(ChangeListener listener) {
		if(Checker.isEmpty(listeners))
			return -1;

		for (int i = 0; i < listeners.length; i++) {
			if(listeners[i] == listener)
				return i;
		}
		return -1;
	}
	@Override
	public void removeChangeListener(ChangeListener<E,F> listener) {
		if(listener != null) {
			int n = indexOf(listener);
			if(n != -1)
				listeners[n] = null;
		}
	}
	void close() {
		listeners = null;
	}
	protected void notifyWatchers(E e, F f) {
		for (ChangeListener c : listeners) {
			if(c != null)
				c.changed(e, f);
			
		}
	}
}
