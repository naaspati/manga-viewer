package samrock;

import java.lang.ref.WeakReference;

import org.codejargon.feather.Provides;

import sam.nopkg.EnsureSingleton;
import samrock.api.AppConfig;

class AppSettingProvider {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{ singleton.init(); }
	
	private volatile WeakReference<AppConfig> w = new WeakReference<AppConfig>(null);
	
	@Provides
	public AppConfig instance() throws Exception {
		AppConfig s = w.get();
		if(s != null)
			return s;
		
		synchronized (AppSettingProvider.class) {
			s = w.get();
			if(s != null)
				return s;
			
			s = new AppConfigImpl(){};
			w = new WeakReference<AppConfig>(s);
			return s;
		}
	}
	

}
