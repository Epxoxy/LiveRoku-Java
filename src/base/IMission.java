package base;

public interface IMission {
	boolean isStarted();
	IMission start(String url);
	IMission stop();
	IMission onStarted(IAction started);
	IMission onStopped(IAction stopped);
	IMission onBytesReceived(IAction1<Long> bytesReceived);
}
