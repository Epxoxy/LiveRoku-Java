package download;

import base.Utils;
import base.model.RoomInfo;
import base.interfaces.ILogger;
import helpers.BiliApi;

public class FetchBean {
	public String Folder;
	public String FileFullName;
	public boolean AutoStart;
	public boolean DanmakuNeed;
	public ILogger Logger;
	
	private final BiliApi biliApi;
	private String originRoomIdText;
	private String realRoomIdText;
	private int originRoomId;
	private int realRoomId;
	private String flvAddress;
	private RoomInfo roomInfo;
	

	public String getOriginRoomIdText() {return originRoomIdText;}
	public String getRealRoomIdText() {return realRoomIdText;}
	public int getOriginRoomId() {return originRoomId;}
	public int getRealRoomId() {return realRoomId;}
	public String getFlvAddress() {return flvAddress;}
	public RoomInfo getRoomInfo() {return roomInfo;}

	public FetchBean(int originRoomId, BiliApi biliApi) {
		this.originRoomId = originRoomId;
		this.originRoomIdText = String.valueOf(originRoomId);
		this.biliApi = biliApi;
	}

	public boolean refreshAll() {
		// Try to get real roomId
		String realRoomIdTextTemp = biliApi.getRealRoomId(originRoomIdText);
		// Try to get flv url
		if (!Utils.isNullOrEmpty(realRoomIdTextTemp)) {
			int realRoomIdTemp;
			try {
				realRoomIdTemp = Integer.parseInt(realRoomIdTextTemp);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			String flvUrl = null;
			try {
				flvUrl = biliApi.getRealUrl(realRoomIdTextTemp);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			if (!Utils.isNullOrEmpty(flvUrl)) {
				this.realRoomId = realRoomIdTemp;
				this.realRoomIdText = realRoomIdTextTemp;
				this.flvAddress = flvUrl;
				return true;
			}
		}
		return false;
	}

	public boolean fetchRoomInfo() {
		if (realRoomId <= 0) {
			String realRoomIdTextTemp = biliApi.getRealRoomId(originRoomIdText);
			int realRoomIdTemp;
			try {
				realRoomIdTemp = Integer.parseInt(realRoomIdTextTemp);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			this.realRoomId = realRoomIdTemp;
		}
		this.roomInfo = biliApi.getRoomInfo(realRoomId);
		return true;
	}
}
