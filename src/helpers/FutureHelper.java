package helpers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureHelper {
	public static void timedRun(Future<?> task, long timeout, TimeUnit unit) {
		try {
			if(task.isCancelled()) return;
			task.get(timeout, unit); 
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
        	e.printStackTrace();
        } finally {  
            task.cancel(true);  
        }
	}
}
