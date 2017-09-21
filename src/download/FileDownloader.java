package download;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import base.IAction;
import base.IAction1;
import base.IMission;
import base.Utils;

public class FileDownloader implements IMission{
	// private static final String DEFAULT_ENCODE = "utf-8";
	private String targetUrl;
	private String localFilename;
    private URLConnection urlConnection = null;
    private volatile boolean isDownloading = false;
    private volatile boolean isStarted = false;
    private boolean bStopJob = false;
    private IAction started;
    private IAction stopped;
    private IAction1<Long> bytesReceived;
    private int bufferLength = 65536;
    
    private FileDownloader(String targetUrl, String localFilename) {
    	this.targetUrl = targetUrl;
    	this.localFilename = localFilename;
    }

    public static IMission newMission(String targetUrl, String localFilename) {
    	return new FileDownloader(targetUrl,localFilename);
    }
    
	@Override
	public IMission start(String targetUrl) {
		// TODO Auto-generated method stub
		isStarted = true;
		this.targetUrl = targetUrl;
		downloadFileImpl(targetUrl, localFilename);
		return this;
	}

	@Override
	public IMission stop() {
		// TODO Auto-generated method stub
		isStarted = false;
        if (isDownloading) {
            bStopJob = true;
        }
		return this;
	}
    
    public IMission onStarted(IAction started) {
    	this.started = started;
		return this;
    }
    
    public IMission onStopped(IAction stopped) {
    	this.stopped = stopped;
		return this;
    }
    
    public IMission onBytesReceived(IAction1<Long> bytesReceived) {
    	this.bytesReceived = bytesReceived;
		return this;
    }
    

    private void setIsDownloading(boolean isDownloading) {
        if (this.isDownloading != isDownloading) {
            this.isDownloading = isDownloading;
            Utils.tryInvoke(isDownloading ? started : stopped);
        }
    }

    @SuppressWarnings("unused")
    public boolean downloadFileImpl(String targetUrl, String localFilename) {
        boolean bResult = false;

        if (Utils.isNullOrEmpty(targetUrl) || Utils.isNullOrEmpty(localFilename)) {
            System.err.println("Invalid download parameters.");
            return bResult;
        }

        System.out.println("Url:" + targetUrl);
        System.out.println("File:" + localFilename);

        int respCode = 0;
        long totalSize = 0;
        long remoteSize = 0;
        long bytesDownloaded = 0;

        RandomAccessFile randomAccessFile = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(targetUrl);
            urlConnection = url.openConnection();
            if (urlConnection == null) {
                System.err.println("Open uri error.");
                return bResult;
            }
            urlConnection.setConnectTimeout(15 * 1000);
            urlConnection.setReadTimeout(15 * 1000);
            urlConnection.setDoOutput(true);

            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setInstanceFollowRedirects(true);//明确指示自动跟踪301和302跳转

            // set request head
            httpURLConnection.setRequestProperty("Accept-Charset", "utf-8");
            httpURLConnection.setRequestProperty("Content-Type", "*/*");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Cache-Control", "no-cache");

            respCode = httpURLConnection.getResponseCode();
            if (respCode >= 300) {
                System.err.println("Http request failed,response status:" + respCode);
                return bResult;
            } else if (respCode == 200) {
                bytesDownloaded = 0;
            }

            String strSize = urlConnection.getHeaderField("Content-Length");
            if (!Utils.isNullOrEmpty(strSize)) {
                try {
                    remoteSize = Long.parseLong(strSize);
                    System.out.println("Size:" + remoteSize + "---" + Utils.getFriendlySize(remoteSize));
                    totalSize += remoteSize;
                } catch (Exception e) {
                    System.err.println("Get remote file size error.");
                    e.printStackTrace();
                }
            }
            inputStream = urlConnection.getInputStream();
            randomAccessFile = new RandomAccessFile(localFilename, "rw");
            if (bytesDownloaded > 0) {
                randomAccessFile.seek(bytesDownloaded);
            }

            // start download
            byte[] buffer = new byte[bufferLength];
            setIsDownloading(true);
            int readBytes;
            while ((readBytes = inputStream.read(buffer)) != -1) {
                bytesDownloaded += readBytes;
                if (readBytes > 0) {
                    randomAccessFile.write(buffer, 0, readBytes);
                    Utils.tryInvoke(bytesReceived, bytesDownloaded);
                    if (bStopJob == true) {
                        System.out.println("task interrupt");
                        break;
                    }
                }
            }
            setIsDownloading(false);
            bResult = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                setIsDownloading(false);
                if (inputStream != null) {
                    inputStream.close();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                urlConnection = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bResult;
    }

	@Override
	public boolean isStarted() {
		// TODO Auto-generated method stub
		return this.isStarted;
	}

}
