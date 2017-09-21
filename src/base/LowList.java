package base;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LowList<T>{
    private List<T> source;
    private Object lockHelper = new Object ();

    public LowList () {
        source = new ArrayList<T> ();
    }

    public void add (T value) {
    	synchronized (lockHelper) {
            source.add (value);
        }
    }

    public void remove (T value) {
    	synchronized (lockHelper) {
            source.remove (value);
        }
    }

    public void purge () {
    	synchronized (lockHelper) {
            source.removeIf(new Predicate<T>() {
				@Override
				public boolean test(T t) {
					return null == t;
				}
            });
        }
    }

    public void clear () {
        source.clear ();
    }

    public void foreachEx (IAction1<T> action, IAction1<Exception> onError) {
    	for(int i = 0; i < source.size(); i++) {
    		T target = source.get(i);
            if (null != target) {
                try {
                    action.invoke (target);
                } catch (Exception e) {
                	try {
                        onError.invoke (e);
                	}catch(Exception e2) {
                		e2.printStackTrace();
                	}
                }
            }
    	}
    }
}
