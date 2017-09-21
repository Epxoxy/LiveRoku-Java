package base.interfaces;

public interface IDanmakuClient {
	void start(String host, int port, int channelId);
	boolean isActive();
	IDanmakuServiceEvents getEvents();
}
