package uk.co.minter.ottrss.utils;

import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class HTTPDownload {
	public static InputStream download(URL url, String data, boolean verify, int timeout, String auth) throws IOException {
		final int MAX_REDIRECTS = 5;
		int redirects = 0;
		while(true) {
			HttpURLConnection c = (HttpURLConnection)url.openConnection();
			c.setInstanceFollowRedirects(true);
			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);

			if(auth != null) {
				auth = "Basic " + Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
				c.setRequestProperty("Authorization", auth);
			}

			if(c instanceof HttpsURLConnection && !verify)
				((HttpsURLConnection)c).setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession s) {
						return true;
					}
				});

			if(data != null) {
				c.setRequestProperty("Content-Type", "application/json");
				c.setDoOutput(true);
				c.setChunkedStreamingMode(0);
				c.getOutputStream().write(data.getBytes());
			}

			int code = c.getResponseCode();

			if(code == 200)
				return c.getInputStream();
			else if(redirects++ < MAX_REDIRECTS && (code == 301 || code == 302))
				url = new URL(c.getHeaderField("Location")); // and try again
			else
				throw new IOException("Unexpected HTTP status code " + c.getResponseCode());
		}
	}

	public static String downloadString(URL url, String data, boolean verify, int timeout, String auth) throws IOException {
		InputStreamReader i = new InputStreamReader(download(url, data, verify, timeout, auth));
		StringWriter o = new StringWriter();

		char[] buf = new char[4096];
		int n;

		while((n = i.read(buf)) != -1)
			o.write(buf, 0, n);

		i.close();

		return o.toString();
	}

	public static void downloadFile(URL url, String data, boolean verify, int timeout, File f) throws IOException {
		InputStream i = download(url, data, verify, timeout, null);
		FileOutputStream o = new FileOutputStream(f);

		byte[] buf = new byte[4096];
		int n;

		while((n = i.read(buf)) != -1)
			o.write(buf, 0, n);

		i.close();
		o.close();
	}
}
