import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import base.IAction;
import base.IAction1;
import base.IMission;
import base.interfaces.IDanmakuClient;
import base.interfaces.ILiveDownloader;
import base.interfaces.ILogger;
import base.interfaces.IRequestModel;
import base.model.DanmakuModel;
import base.model.MsgType;
import danmaku.DanmakuClient;
import download.FileDownloader;
import download.LiveDownloader;
import helpers.BiliApi;
import helpers.BiliApi.ServerBean;
import helpers.FLVMetaData;

public class Program {

	static class Logger implements ILogger {
		@Override
		public void appendLine(String tag, String msg) {
			System.out.println("[" + tag + "] " + msg);
		}
	}

	static class Test implements IRequestModel{
		private SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		private ILiveDownloader downloader;
		private String roomId;
		
		public Test(String roomId) {
			this.roomId = roomId;
		}
		
		public ILiveDownloader getDownloader() {
			return downloader;
		}
		
		public boolean run() {
			downloader = new LiveDownloader(this);
			downloader.getLoggers().add(new Logger());
			downloader.start(this);
			return true;
		}

		@Override
		public String getRoomId() {
			return roomId;
		}

		@Override
		public String formatFileName(Date date) {
			return f.format(date);
		}

		@Override
		public String getLocation() {
			return "C:\\mnt\\hd1\\station\\bilibili\\test\\java";
		}

		@Override
		public boolean isDanmakuNeed() {
			return true;
		}

		@Override
		public boolean autoStart() {
			return true;
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		List<IMission> missions = new ArrayList<>();
		boolean breakMe = true;
		String originRoomId = "498";
		String userHome = System.getProperty("user.home");
		String location = userHome + "\\desktop\\java\\a.flv";
		System.out.println(location);
		Test test = new Test(originRoomId);
		new Thread(){
			@Override
			public void run() {
				test.run();
			}
		}.start();
		if(!breakMe){
			BiliApi api = new BiliApi(new Logger(), "");
			String realRoomIdText = api.getRealRoomId(originRoomId);
			String targetUrl = api.getRealUrl(realRoomIdText);
			IMission downloadMission = null;
			// downloadMission = testDownload(targetUrl, location);
			missions.add(downloadMission);
			IDanmakuClient client = testDanmaku(api, realRoomIdText);
		}
		waitingInputValue(-1, new IAction() {
			@Override
			public void invoke() {
				ILiveDownloader downloader = null;
				if(test!=null && (downloader = test.getDownloader()) != null) {
					System.out.println("do what");
					downloader.stop(true);
				}
				for(IMission mission : missions) {
					try{
						if(mission == null) continue;
						mission.stop();
					}catch(Exception e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		});
	}
	
	private static void waitingInputValue(int value, IAction doWhat) {
		Scanner scanner = new Scanner(System.in);
		if (scanner.nextInt() == value) {
			if(doWhat!= null) {
				doWhat.invoke();
			}
		}
		scanner.close();
	}

	private static IMission testDownload(String targetUrl, String location) {
		IMission mission = FileDownloader.newMission(targetUrl, location);
		mission.onStarted(new IAction() {
			@Override
			public void invoke() {
				System.out.println("started......");
			}
		}).onStopped(new IAction() {
			@Override
			public void invoke() {
				System.out.println("stopped......");
			}
		}).onBytesReceived(new IAction1<Long>() {
			int times;

			@Override
			public void invoke(Long size) {
				times++;
				if (times % 100 == 0) {
					System.out.print("-");
				}
				if (times > 10000) {
					times = 0;
					System.out.println();
				}
				if (size % 10240 > 0) {
					// System.out.print(Utils.getFriendlySize(size));
					// mission.stop();
				}
			}
		});
		Thread thread = new Thread() {
			@Override
			public void run() {
				mission.start(targetUrl);
			}
		};
		thread.start();
		return mission;
	}

	private static IDanmakuClient testDanmaku(BiliApi api, String realRoomIdText) {
		// Danmaku client start
		IDanmakuClient client = new DanmakuClient();
		ServerBean server = api.getDmServerAddr(realRoomIdText);
		System.out.println(server.ok);
		if (server.ok) {
			String host = server.url;
			int port = Integer.parseInt(server.port);
			int channelId = Integer.parseInt(realRoomIdText);
			// register events
			client.getEvents().setDanmakuReceived(new IAction1<DanmakuModel>() {
				@Override
				public void invoke(DanmakuModel danmaku) {
					if (danmaku.MsgType == MsgType.Comment)
						System.out.println(danmaku.toString(0));
				}
			});
			client.getEvents().setOnInactive(new IAction() {
				@Override
				public void invoke() {

				}
			});
			client.start(host, port, channelId);
			return client;
		}
		return null;
	}
}
