package samrock;

import java.lang.ref.WeakReference;

import org.codejargon.feather.Provides;

import sam.nopkg.EnsureSingleton;
import samrock.api.AppSetting;

class AppSettingProvider {
	private static final EnsureSingleton singleton = new EnsureSingleton();
	{ singleton.init(); }
	
	private volatile WeakReference<AppSetting> w = new WeakReference<AppSetting>(null);
	
	@Provides
	public AppSetting instance() throws Exception {
		AppSetting s = w.get();
		if(s != null)
			return s;
		
		synchronized (AppSettingProvider.class) {
			s = w.get();
			if(s != null)
				return s;
			
			s = new AppSettingImpl(){};
			w = new WeakReference<AppSetting>(s);
			return s;
		}
	}
	

}
