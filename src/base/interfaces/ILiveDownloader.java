package base.interfaces;

import base.LowList;
import base.model.RoomInfo;

public interface ILiveDownloader extends IDanmakuSource{
	LowList<IStatusBinder> getStatusBinders();
	LowList<ILiveDataResolver> getLiveDataResolvers();
	LowList<ILogger> getLoggers();
	Object getExtra(String key);
	
	void start(IRequestModel model);
	void stop(boolean force);
	void disponse();
	RoomInfo getRoomInfo(boolean refresh);
}
