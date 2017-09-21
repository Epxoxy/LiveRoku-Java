package base;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Utils {

	public static boolean isNullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}

	public static void tryInvoke(IAction action) {
		if (action != null) {
			action.invoke();
		}
	}

	public static <T> void tryInvoke(IAction1<T> action, T data) {
		if (action != null) {
			action.invoke(data);
		}
	}

	public static <T, R> void tryInvoke(IAction2<T, R> action, T d, R r) {
		if (action != null) {
			action.invoke(d, r);
		}
	}

	public static String changeExtension(String source, String newExtension) {
		String target;
		String currentExtension = getFileExtension(source);

		if (currentExtension.equals("")) {
			target = source + "." + newExtension;
		} else {
			target = source.replaceFirst(Pattern.quote("." + currentExtension) + "$",
					Matcher.quoteReplacement("." + newExtension));
		}
		return target;
	}

	public static String getFileExtension(String f) {
		String ext = "";
		int i = f.lastIndexOf('.');
		if (i > 0 && i < f.length() - 1) {
			ext = f.substring(i + 1);
		}
		return ext;
	}

	public static String md5(String data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(data.getBytes());
		StringBuffer buf = new StringBuffer();
		byte[] bits = md.digest();
		for (int i = 0; i < bits.length; i++) {
			int a = bits[i];
			if (a < 0)
				a += 256;
			if (a < 16)
				buf.append("0");
			buf.append(Integer.toHexString(a));
		}
		return buf.toString();
	}

	public static String getFriendlySize(long nSize) {
		StringBuilder builder = new StringBuilder();
		final int UNIT = 1024;
		boolean bBitBytes = true;
		if (UNIT == 1000) {
			bBitBytes = false;
		} else {
			bBitBytes = true;
		}

		DecimalFormat df = new DecimalFormat("#.00");
		if (nSize < UNIT) {
			builder.append(df.format(1.0f * nSize));
			builder.append(bBitBytes ? "B" : "b");
		} else if (nSize < UNIT * UNIT) {
			builder.append(df.format(1.0f * nSize / UNIT));
			builder.append(bBitBytes ? "KiB" : "KB");
		} else if (nSize < UNIT * UNIT * UNIT) {
			builder.append(df.format(1.0f * nSize / UNIT / UNIT));
			builder.append(bBitBytes ? "MiB" : "MB");
		} else {
			builder.append(df.format(1.0f * nSize / UNIT / UNIT / UNIT));
			builder.append(bBitBytes ? "GiB" : "GB");
		}

		return builder.toString();
	}
}
