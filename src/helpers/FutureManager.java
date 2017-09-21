package helpers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class FutureManager {
	private Map<String, Future<?>> map = new HashMap<>();
	
	public void set(String key, Future<?> future) {
		cancel(key);
		map.put(key, future);
	}
	
	public void cancel(String key) {
		if(map.containsKey(key)) {
			cancel(map.get(key));
		}
	}
	
	public void remove(String key) {
		cancel(key);
		map.remove(key);
	}
	
	public void cancelAll() {
		Iterator<String> keys = map.keySet().iterator();
		while(keys.hasNext()) {
			cancel(keys.next());
		}
	}
	
	private void cancel(Future<?> exist) {
		if(exist != null && !exist.isDone() && !exist.isCancelled()) {
			exist.cancel(true);
		}
	}
}
