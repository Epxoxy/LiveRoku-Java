package base.interfaces;

import base.IAction;
import base.IAction1;
import base.model.DanmakuModel;

public interface IDanmakuServiceEvents {
	void setHotUpdated(IAction1<Integer> hotUpdated);
	void setDanmakuReceived(IAction1<DanmakuModel> danmakuReceived);
	void setOnActive(IAction active);
	void setOnInactive(IAction inactive);
}
