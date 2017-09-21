package base.interfaces;

import java.util.Date;

public interface IRequestModel {
	String getRoomId();
	String formatFileName(Date date);
	String getLocation();
	boolean isDanmakuNeed();
	boolean autoStart();
}
