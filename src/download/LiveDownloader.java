package download;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import base.IAction;
import base.IAction1;
import base.LowList;
import base.IMission;
import base.Utils;
import base.interfaces.IDanmakuResolver;
import base.interfaces.IDanmakuServiceEvents;
import base.interfaces.ILiveDataResolver;
import base.interfaces.ILiveDownloader;
import base.interfaces.ILogger;
import base.interfaces.IRequestModel;
import base.interfaces.IStatusBinder;
import base.model.DanmakuModel;
import base.model.MsgType;
import base.model.RoomInfo;
import base.model.VideoInfo;
import danmaku.DanmakuClient;
import danmaku.DanmakuStorage;
import helpers.BiliApi;
import helpers.FutureHelper;
import helpers.BiliApi.ServerBean;
import helpers.FutureManager;

public class LiveDownloader implements ILiveDownloader, ILogger {
	private ExecutorService eventExcutor = Executors.newCachedThreadPool();
	private ExecutorService workerExcutor = Executors.newCachedThreadPool();

	private final LowList<IStatusBinder> statusBinders;
	private final LowList<ILiveDataResolver> liveDataResolvers;
	private final LowList<IDanmakuResolver> danmakuResolvers;
	private final LowList<ILogger> loggers;
	private final LowList<IAction1<Long>> byteReceivedResolvers;
	private final BiliApi biliApi;
	private FetchBean settings;
	private long recordSize;
	private volatile boolean isRunning = false;
	private volatile boolean isStreaming = false;
	private volatile boolean isLiveOn = false;
	private IMission flvMission = null;
	private final DanmakuClient danmakuClient;
	private VideoInfo videoInfo;
	private DanmakuStorage danmakuStorage;
	private IRequestModel model;
	private final FutureManager cancelMgr;
	private long timeoutMs = 5000;

	public LiveDownloader(IRequestModel model) {
		//
		this.model = model;
		byteReceivedResolvers = new LowList<IAction1<Long>>();
		statusBinders = new LowList<IStatusBinder>();
		liveDataResolvers = new LowList<ILiveDataResolver>();
		danmakuResolvers = new LowList<IDanmakuResolver>();
		loggers = new LowList<ILogger>();
		biliApi = new BiliApi(this, "");
		danmakuClient = new DanmakuClient();
		cancelMgr = new FutureManager();
	}

	
	//---------  ILiveDownloader start -----------

	@Override
	public LowList<IStatusBinder> getStatusBinders() {
		return statusBinders;
	}

	@Override
	public LowList<ILiveDataResolver> getLiveDataResolvers() {
		return liveDataResolvers;
	}

	@Override
	public LowList<IDanmakuResolver> getDanmakuResolvers() {
		return danmakuResolvers;
	}

	@Override
	public LowList<ILogger> getLoggers() {
		return loggers;
	}

	@Override
	public Object getExtra(String key) {
		return null;
	}

	@Override
	public void stop(boolean force) {
		System.out.println("stop " + force);
		isRunning = false;
		isStreaming = false;
		if (this.flvMission != null) {
			IMission temp = this.flvMission;
			this.flvMission = null;
			temp.stop();
		}
		if(danmakuClient!= null) {
			danmakuClient.stop();
		}
		if(danmakuStorage!=null) {
			danmakuStorage.stop(force);
		}
		cancelMgr.cancelAll();
		workerExcutor.shutdown();
		eventExcutor.shutdown();
	}
	
	@Override
	public void start(IRequestModel model) {
		if (isRunning)
			return;
		if (this.model != model) {
			this.model = model;
		}
		isRunning = true;
		isStreaming = false;
		recordSize = 0;
		int roomId;
		try {
			roomId = Integer.parseInt(model.getRoomId());
			startImpl(model, roomId);
		} catch (Exception e) {
			e.printStackTrace();
			stop(false);
		}
	}

	private void startImpl(IRequestModel model, int roomId) {
		settings = new FetchBean(roomId, biliApi);
		if (settings.refreshAll()) {
			settings.AutoStart = model.autoStart();
			settings.DanmakuNeed = model.isDanmakuNeed();
			settings.Folder = model.getLocation();
			String fileName = model.formatFileName(new Date(System.currentTimeMillis()));
			settings.FileFullName = settings.Folder + File.separator + fileName + ".flv";
			if (flvMission != null) {
				flvMission.stop();
				flvMission = null;
			}
			flvMission = FileDownloader.newMission(settings.getFlvAddress(), settings.FileFullName);
			flvMission.onStarted(new IAction() {
				@Override
				public void invoke() {
					System.out.println("flv download started......");
				}
			}).onStopped(new IAction() {
				@Override
				public void invoke() {
					System.out.println("flv download stopped......");
				}
			}).onBytesReceived(new IAction1<Long>() {
				@Override
				public void invoke(Long totalBytes) {
					recordSize = totalBytes;
					downloadSizeUpdatedEmit(totalBytes);
					byteReceivedEmit(totalBytes);
				}
			});
			byteReceivedResolvers.remove(checkStreaming);
			byteReceivedResolvers.add(checkStreaming);
			workerExcutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						flvMission.start(settings.getFlvAddress());
					}catch(Exception e) {
						e.printStackTrace();
						System.out.println("flv download mission stopped.");
					}
				}
			});
			danmakuClientStartImpl(workerExcutor, settings.getRealRoomId(), new IAction() {
				//initialize basic data
				@Override
				public void invoke() {
					IDanmakuServiceEvents events = danmakuClient.getEvents();
					events.setOnInactive(new IAction() {
						@Override
						public void invoke() { dmServerReconnect(null); }
					});
					events.setDanmakuReceived(new IAction1<DanmakuModel>() {
						@Override
						public void invoke(DanmakuModel danmaku) {
							if (!isRunning/* May not come here */)
								return;
							checkLiveStatus(danmaku);
							danmakuReceivedEmit(danmaku);
						}
					});
					events.setHotUpdated(new IAction1<Integer>() {
						@Override
						public void invoke(Integer popularity) {
							hotUpdatedEmit(popularity);
						}
					});
				}
			});
		}
	}
	
	private void danmakuClientStartImpl(ExecutorService worker, int channelId, IAction init) {
		worker.execute(new Runnable() {
			@Override
			public void run() {
				ServerBean dmServer = biliApi.getDmServerAddr(String.valueOf(channelId));
				if(!dmServer.ok) {
					System.out.println("connect to danmaku server error, " + dmServer.url + "," + dmServer.port);
					return;
				}
				String url = dmServer.url;
				int port = Integer.parseInt(dmServer.port);
				if(init != null) {
					init.invoke();
				}
				danmakuClient.start(url, port, channelId);
			}
		});
	}

	@Override
	public void disponse() {

	}

	@Override
	public RoomInfo getRoomInfo(boolean refresh) {
		if (!isRunning || refresh) {
			if (settings == null) {
				int roomId;
				try {
					roomId = Integer.parseInt(model.getRoomId());
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				settings = new FetchBean(roomId, biliApi);
				settings.refreshAll();
			}
			settings.fetchRoomInfo();
		}
		return settings.getRoomInfo();
	}

	
	private void checkLiveStatus(DanmakuModel danmaku) {
		if (MsgType.LiveEnd == danmaku.MsgType) {
			cancelMgr.cancel ("autostart-fetch");
			cancelMgr.remove ("autostart-fetch");
			flvMission.stop();
			if(danmakuStorage!=null)
				danmakuStorage.stop(false);
			danmakuResolvers.remove(enqueueDanmaku);
			// Update live status
			updateLiveStatus(false, true);
			System.out.println("Live stoppd");
		} else if (MsgType.LiveStart == danmaku.MsgType) {
			// Update live status
			updateLiveStatus(true, true);
			System.out.println("Live started");
			if (settings.AutoStart && !flvMission.isStarted()) {
				cancelMgr.cancel ("autostart-fetch");
				Future<?> task = workerExcutor.submit(new Runnable() {
					@Override
					public void run() {
						boolean isUpdated = settings.refreshAll();
						if (isUpdated && LiveDownloader.this.isRunning && LiveDownloader.this.isLiveOn) {
							appendInfoMsg("Flv address updated : " + settings.getFlvAddress());
							flvMission.start(settings.getFlvAddress());
						}
						cancelMgr.remove("autostart-fetch");
					}
				});
				cancelMgr.set ("autostart-fetch", task);
				FutureHelper.timedRun(task, timeoutMs, TimeUnit.MILLISECONDS);
			}
		}
	}

	private void updateLiveStatus(boolean isLiveOn, boolean raiseEvent) {
		if (this.isLiveOn == isLiveOn)
			return;
		this.isLiveOn = isLiveOn;
		if (!raiseEvent)
			return;
		statusUpdatedEmit();
	}

	private void dmServerReconnect(Exception e) {
		if (e != null)
			appendErrorMsg(e.getMessage());
		if (!isRunning)
			return;
		workerExcutor.execute(new Runnable() {
			@Override
			public void run() {
				appendInfoMsg("Trying to reconnect to the danmaku server.");
				danmakuClientStartImpl(workerExcutor, settings.getRealRoomId(), null);
			}
		});
	}


	// Raise event when video info checked
	private void videoChecked(VideoInfo info) {
		VideoInfo previous = this.videoInfo;
		this.videoInfo = info;
		if (previous.BitRate != info.BitRate) {
			appendInfoMsg(previous.BitRate + "  " + info.BitRate);
			String text = info.BitRate / 1000 + " Kbps";
			forEachByTaskWithDebug(liveDataResolvers, new IAction1<ILiveDataResolver>() {
				@Override
				public void invoke(ILiveDataResolver resolver) {
					resolver.onBitRateUpdate(info.BitRate, text);
				}
			});
		}
		if (previous.Duration != info.Duration) {
			String text = formatTime(info.Duration);
			forEachByTaskWithDebug(liveDataResolvers, new IAction1<ILiveDataResolver>() {
				@Override
				public void invoke(ILiveDataResolver resolver) {
					resolver.onDurationUpdate(info.Duration, text);
				}

			});
		}
	}

	//---------  Actions start -----------

	private IAction1<Long> checkStreaming = new IAction1<Long>() {
		@Override
		public void invoke(Long bytes) {
			appendInfoMsg("Streaming check.....");
			if (bytes < 2 || isStreaming)
				return;
			isStreaming = true;
			byteReceivedResolvers.remove(this);
			danmakuResolvers.remove(enqueueDanmaku);
			workerExcutor.execute(new Runnable() {
				@Override
				public void run() {
					if(danmakuStorage!=null){
						danmakuStorage.stop(true);
					}
					// Generate danmaku storage
					if (settings.DanmakuNeed) {
						String xmlPath = Utils.changeExtension(settings.FileFullName, "xml");
						long startTime = System.currentTimeMillis();
						danmakuStorage = new DanmakuStorage(xmlPath, startTime, Charset.forName("utf-8"));
						danmakuResolvers.add(enqueueDanmaku);
						danmakuStorage.startAsync();
						appendInfoMsg("Start danmaku storage.....");
					}
				}
			});
			onStreamingEmit();
		}
	};

	private IDanmakuResolver enqueueDanmaku= new IDanmakuResolver() {
		@Override
		public void hand(DanmakuModel danmaku) {
			if(danmakuStorage == null) return;
			danmakuStorage.enqueue(danmaku);
		}
	};

	private IAction1<Exception> printError = new IAction1<Exception>() {
		@Override
		public void invoke(Exception e) {
			e.printStackTrace();
		}
	};

	
	
	//---------  Emit start -----------

	private void danmakuReceivedEmit(DanmakuModel danmaku) {
		forEachByTaskWithDebug(danmakuResolvers, new IAction1<IDanmakuResolver>() {
			@Override
			public void invoke(IDanmakuResolver resolver) {
				// TODO something
				resolver.hand(danmaku);
			}
		});
	}

	private void byteReceivedEmit(long bytesReceived) {
		forEachByTaskWithDebug(byteReceivedResolvers, new IAction1<IAction1<Long>>() {
			@Override
			public void invoke(IAction1<Long> action) {
				action.invoke(bytesReceived);
			}
		});
	}
	
	private void hotUpdatedEmit(long popularity) {
		forEachByTaskWithDebug(liveDataResolvers, new IAction1<ILiveDataResolver>() {
			@Override
			public void invoke(ILiveDataResolver resolver) {
				resolver.onHotUpdate(popularity);
			}
		});
	}
	
	private void downloadSizeUpdatedEmit(long totalBytes) {
		// OnDownloadSizeUpdate
		String text = Utils.getFriendlySize(totalBytes);
		forEachByTaskWithDebug(liveDataResolvers, new IAction1<ILiveDataResolver>() {
			@Override
			public void invoke(ILiveDataResolver resolver) {
				resolver.onDownloadSizeUpdate(totalBytes, text);
			}
		});
	}
	
	private void statusUpdatedEmit() {
		forEachByTaskWithDebug(liveDataResolvers, new IAction1<ILiveDataResolver>() {
			@Override
			public void invoke(ILiveDataResolver resolver) {
				resolver.onStatusUpdate(isLiveOn);
			}
		});
	}

	private void onStreamingEmit() {
		forEachByTaskWithDebug(statusBinders, new IAction1<IStatusBinder>() {
			@Override
			public void invoke(IStatusBinder binder) {
				binder.onStreaming();
			}
		});
	}

	
	//---------  Helpers start -----------

	private <T> void forEachByTaskWithDebug(LowList<T> host, IAction1<T> action) {
		eventExcutor.execute(new Runnable() {
			@Override
			public void run() {
				host.foreachEx(action, printError);
			}
		});
	}

	@Override
	public void appendLine(String tag, String msg) {
		forEachByTaskWithDebug(loggers, new IAction1<ILogger>() {
			@Override
			public void invoke(ILogger logger) {
				logger.appendLine(tag, msg);
			}
		});
	}
	
	private void appendErrorMsg(String msg) {
		appendLine("Error", msg);
	}

	private void appendInfoMsg(String msg) {
		appendLine("INFO", msg);
	}

	private String formatTime(long ms) {
		return new StringBuilder().append(String.valueOf(ms / (1000 * 60 * 60))).append(":")
				.append(String.valueOf(ms / (1000 * 60) % 60)).append(":").append(String.valueOf(ms / 1000 % 60))
				.toString();
	}

}
