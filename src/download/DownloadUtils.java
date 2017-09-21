package download;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUtils {

	public static String downloadString(String address) throws Exception {
		URL url = new URL(address);
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		return builder.toString();
	}

	//TODO remove it
	public static void downloadFromUrl(String urlStr, String fileName, String savePath) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		// conn.setConnectTimeout(3 * 1000);
		conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		InputStream inputStream = conn.getInputStream();
		byte[] getData = readInputStream(inputStream);

		File saveDir = new File(savePath);
		if (!saveDir.exists()) {
			saveDir.mkdir();
		}
		File file = new File(saveDir + File.separator + fileName);
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(getData);
		if (fos != null) {
			fos.close();
		}
		if (inputStream != null) {
			inputStream.close();
		}
		System.out.println("info:" + url + " download success");
	}

	//TODO remove it
	public static byte[] readInputStream(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[1024];
		int len = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((len = inputStream.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		bos.close();
		return bos.toByteArray();
	}
    
}
