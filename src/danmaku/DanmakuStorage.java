package danmaku;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

import base.model.DanmakuModel;
import base.model.MsgType;

public class DanmakuStorage {
	private static final String BaseHead = "<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?><i>";
	private static final String PartHead = "<chatserver>chat.bilibili.com</chatserver><chatid>0</chatid><mission>0</mission>";
	private static final String XmlHeader = BaseHead + PartHead+"<maxlimit>0</maxlimit><source>k-v</source>";
    private static final String XmlFooter = "</i>";
	private long startTime;
	private String path;
	private Charset charset;
    private int flushTimeMs;
    private volatile boolean isWriting;
    private FileOutputStream output;
    private LinkedBlockingQueue<DanmakuModel> queue;
    
	public DanmakuStorage(String path, long startTime, Charset charset) {
		this(path, startTime, charset, 30000);
	}
	
	public DanmakuStorage(String path, long startTime, Charset charset, int flushTimeMs) {
		this.path = path;
		this.startTime = startTime;
		this.charset = charset;
		this.flushTimeMs = flushTimeMs;
		this.queue = new LinkedBlockingQueue<DanmakuModel>();
	}
	
	public void enqueue(DanmakuModel danmaku) {
		if(danmaku == null || danmaku.MsgType != MsgType.Comment) {
			return;
		}
		try {
			this.queue.put(danmaku);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
    
    public boolean isWriting() {
    	return this.isWriting;
    }
	
	public void stop(boolean force) {
		this.isWriting = false;
        if (force && output !=null) {
            try {
                output.close ();
            } catch (Exception e) {
                e.printStackTrace ();
            }
        }
	}
	
	public void startAsync() {
		if(isWriting) {
			return;
		}
		isWriting = true;
		startWriteImpl();
	}
	
	private void startWriteImpl() {
    	File file = new File(path);
        try {
    		output = openOutputStream(file, false);
        	output.write(XmlHeader.getBytes(charset));
        } catch (Exception e) {
            e.printStackTrace ();
    		stop(false);
            return;
        }
        startFlushAsync();
        byte[] newline = "\r\n".getBytes(charset);
        while(isWriting) {
        	try {
        		DanmakuModel danmaku = queue.take();
        		output.write(newline);
        		output.write(danmaku.toString(startTime).getBytes(charset));
        	}catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        try {
        	output.write (XmlFooter.getBytes(charset));
        	output.flush();
        	output.close();
        } catch (Exception e) {
            e.printStackTrace ();
        }
	}
	
	private void startFlushAsync() {
		//TODO
		new Thread() {
			@Override
			public void run() {
				while(isWriting) {
					try {
						if(output == null) continue;
						output.flush();
						Thread.sleep(flushTimeMs);
					} catch (InterruptedException | IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

    //Open output stream safety with basic check
    public static FileOutputStream openOutputStream(File file, boolean append)
            throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file
                        + "' exists but is a directory");
            }
            if (!file.canWrite()) {
                throw new IOException("File '" + file
                        + "' cannot be written to");
            }
        }
        else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent
                            + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }
}
