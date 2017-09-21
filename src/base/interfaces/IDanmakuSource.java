package base.interfaces;

import base.LowList;;

public interface IDanmakuSource {
	LowList<IDanmakuResolver> getDanmakuResolvers();
}
